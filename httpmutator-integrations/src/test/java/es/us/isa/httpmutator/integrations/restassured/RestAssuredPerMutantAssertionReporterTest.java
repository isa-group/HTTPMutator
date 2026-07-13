package es.us.isa.httpmutator.integrations.restassured;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.MutationSummary;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestAssuredPerMutantAssertionReporterTest {

    private WireMockServer wireMockServer;

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        RestAssured.reset();
    }

    @Test
    void writesPerMutantAssertionCsvConsistentWithSummary() throws Exception {
        Path reportDir = Paths.get("target", "httpmutator-restassured-test", "per-mutant");
        String reportName = "per-mutant-report-test";
        Path perMutantCsv = reportDir.resolve(reportName + "-per-mutant-assertions.csv");
        Files.createDirectories(reportDir);
        Files.deleteIfExists(perMutantCsv);

        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        wireMockServer.stubFor(
                get(urlEqualTo("/item"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"id\":7,\"name\":\"alpha\",\"ok\":true}")));

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = wireMockServer.port();

        HttpMutatorRestAssuredFilter filter = new HttpMutatorRestAssuredFilter(
                42L,
                new AllOperatorsStrategy(),
                reportDir,
                reportName,
                HttpMutatorRestAssuredFilter.OriginalAssertionFailurePolicy.THROW);
        RestAssured.filters(filter);

        given().when().get("/item").then().statusCode(200);
        filter.addAssertionsForLastRequest((ValidatableResponse resp) -> {
            resp.statusCode(200);
            resp.body("id", equalTo(7));
        });

        MutationSummary summary = filter.runAllMutations();

        assertTrue(Files.exists(perMutantCsv), "Per-mutant assertion CSV should be written");

        List<String> lines = Files.readAllLines(perMutantCsv);
        assertFalse(lines.isEmpty(), "CSV should include a header");
        assertEquals("requestId,mutantIndex,mutator,operator,originalJsonPath,killed,outcome,message",
                lines.get(0));

        List<List<String>> rows = parseCsvRows(lines.subList(1, lines.size()));
        long killedRows = 0;
        long countedRows = 0;
        String requestId = summary.getPerRequestResults().get(0).getLabel();

        for (List<String> row : rows) {
            assertEquals(8, row.size(), "Each per-mutant row should have 8 columns");
            assertEquals(requestId, row.get(0), "Request id should match summary label");
            assertFalse(row.get(1).trim().isEmpty(), "mutantIndex should be present");
            assertFalse(row.get(3).trim().isEmpty(), "operator should be present");
            assertFalse(row.get(6).trim().isEmpty(), "outcome should be present");

            String outcome = row.get(6);
            if ("KILLED".equals(outcome)) {
                killedRows++;
                countedRows++;
                assertEquals("true", row.get(5));
            } else if ("SURVIVED".equals(outcome)) {
                countedRows++;
                assertEquals("false", row.get(5));
            } else {
                assertEquals("CONVERSION_FAILED", outcome);
            }
        }

        assertNotNull(summary);
        assertEquals(summary.getKilledMutants(), killedRows,
                "KILLED rows should match summary killed mutant count");
        assertEquals(summary.getTotalMutants(), countedRows,
                "KILLED + SURVIVED rows should match summary total mutant count");
    }

    private static List<List<String>> parseCsvRows(List<String> lines) {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            rows.add(parseCsvLine(line));
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }
}
