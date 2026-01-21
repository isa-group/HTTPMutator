package es.us.isa.httpmutator.integrations.restassured;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.MutationSummary;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.RequestMutationResult;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.RequestStatus;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.defaultReportDir;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Example JUnit 5 test class showing how to use {@link HttpMutatorRestAssuredFilter}
 * with REST Assured and an embedded WireMock server.
 *
 * <p>Each {@code @Test} method performs exactly one HTTP request. The filter is
 * configured once at class level and records all requests globally. In {@code @AfterAll},
 * we run {@link HttpMutatorRestAssuredFilter#runAllMutations()} and print/verify the
 * global mutation summary.</p>
 */
@TestMethodOrder(OrderAnnotation.class)
public class HttpMutatorRestAssuredFilterExampleTest {

    private static WireMockServer wireMockServer;
    private static HttpMutatorRestAssuredFilter filter;

    @BeforeAll
    static void setup() {
        // Start WireMock on a random port
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // 1) JSON, no assertions
        wireMockServer.stubFor(
                get(urlEqualTo("/json-no-assert"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{ \"type\": \"noAssert\", \"ok\": true, \"value\": 1 }")
                        )
        );

        // 2) JSON, assertions present but fail on original
        wireMockServer.stubFor(
                get(urlEqualTo("/json-fail-assert"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{ \"type\": \"failOriginal\", \"ok\": false, \"value\": 2 }")
                        )
        );

        // 3) JSON, assertions present and pass
        wireMockServer.stubFor(
                get(urlEqualTo("/json-pass-assert"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{ \"type\": \"passOriginal\", \"ok\": true, \"value\": 42 }")
                        )
        );

        // 4) text/plain, assertions present and pass
        wireMockServer.stubFor(
                get(urlEqualTo("/text-pass-assert"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("Hello Mutator")
                        )
        );

        // 5) text/plain, no assertions
        wireMockServer.stubFor(
                get(urlEqualTo("/text-no-assert"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("No assert here")
                        )
        );

        // Configure REST Assured to hit the WireMock server
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = wireMockServer.port();

        // Create the global filter instance used by all tests
        filter = new HttpMutatorRestAssuredFilter(42L, new AllOperatorsStrategy(), defaultReportDir(), "mutants-not-throw-assert-error", HttpMutatorRestAssuredFilter.OriginalAssertionFailurePolicy.DISCARD);
        RestAssured.filters(filter);
    }

    @AfterAll
    static void teardown() {
        // Run mutation testing over all globally recorded interactions
        MutationSummary summary = filter.runAllMutationsAndPrintSummary();
        List<RequestMutationResult> results = summary.getPerRequestResults();

        assertNotNull(summary, "MutationSummary must not be null");

        // Global expectations:
        // We invoked 5 requests in 5 separate tests.
        assertEquals(5, summary.getTotalObservedRequests(),
                "Expected 5 observed requests (we issued 5 HTTP calls).");
        // Assertions were attached for requests 2, 3, and 4.
        assertEquals(3, summary.getTotalRequestsWithAssertions(),
                "Expected 3 requests with assertions (requests 2, 3, and 4).");
        // Requests 1 and 5 never had assertions.
        assertEquals(2, summary.getDiscardedNoAssertions(),
                "Expected 2 requests discarded due to missing assertions (requests 1 and 5).");
        // Request 2 had assertions that failed on the original response.
        assertEquals(1, summary.getDiscardedOriginalAssertionFailed(),
                "Expected 1 request discarded due to failing original assertions (request 2).");
        // Requests 3 and 4 should have mutation executed.
        assertEquals(2, summary.getMutationExecutedRequests(),
                "Expected 2 requests where mutation actually executed (requests 3 and 4).");

        // Check per-request statuses in order:
        // request-0 -> /json-no-assert          -> DISCARDED_NO_ASSERTIONS
        // request-1 -> /json-fail-assert        -> DISCARDED_ORIGINAL_ASSERTION_FAILED
        // request-2 -> /json-pass-assert        -> MUTATION_EXECUTED
        // request-3 -> /text-pass-assert        -> MUTATION_EXECUTED
        // request-4 -> /text-no-assert          -> DISCARDED_NO_ASSERTIONS
        assertEquals(5, results.size(), "Expected 5 per-request results.");

        RequestMutationResult r0 = results.get(0);
        RequestMutationResult r1 = results.get(1);
        RequestMutationResult r2 = results.get(2);
        RequestMutationResult r3 = results.get(3);
        RequestMutationResult r4 = results.get(4);

        assertEquals(RequestStatus.DISCARDED_NO_ASSERTIONS, r0.getStatus(),
                "First request (/json-no-assert) should be DISCARDED_NO_ASSERTIONS.");
        assertEquals(RequestStatus.DISCARDED_ORIGINAL_ASSERTION_FAILED, r1.getStatus(),
                "Second request (/json-fail-assert) should be DISCARDED_ORIGINAL_ASSERTION_FAILED.");
        assertEquals(RequestStatus.MUTATION_EXECUTED, r2.getStatus(),
                "Third request (/json-pass-assert) should be MUTATION_EXECUTED.");
        assertEquals(RequestStatus.MUTATION_EXECUTED, r3.getStatus(),
                "Fourth request (/text-pass-assert) should be MUTATION_EXECUTED.");
        assertEquals(RequestStatus.DISCARDED_NO_ASSERTIONS, r4.getStatus(),
                "Fifth request (/text-no-assert) should be DISCARDED_NO_ASSERTIONS.");

        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        RestAssured.reset();
    }

    // -------------------------------------------------------------------------
    // Individual tests: one HTTP call per test
    // -------------------------------------------------------------------------

    /**
     * Request 1: JSON response, no assertions.
     * Expected final status: DISCARDED_NO_ASSERTIONS.
     */
    @Test
    @Order(1)
    void jsonNoAssert() {
        given()
                .when()
                .get("/json-no-assert")
                .then()
                .statusCode(200);
        // No call to addAssertionsForLastRequest(): this request will be discarded later.
    }

    /**
     * Request 2: JSON response, assertions present but fail on original.
     * Expected final status: DISCARDED_ORIGINAL_ASSERTION_FAILED.
     */
    @Test
    @Order(2)
    void jsonFailAssert() throws Exception {
        given()
                .when()
                .get("/json-fail-assert")
                .then()
                .statusCode(200);


        // Attach assertions that will fail on the original response:
        // JSON has "ok": false but we assert it to be true.
        filter.addAssertionsForLastRequest((ValidatableResponse resp) -> {
            resp.statusCode(200);
            resp.body("ok", equalTo(true)); // This will throw AssertionError on original response
        });
    }

    /**
     * Request 3: JSON response, assertions present and pass.
     * Expected final status: MUTATION_EXECUTED.
     */
    @Test
    @Order(3)
    void jsonPassAssert() throws Exception {
        given()
                .when()
                .get("/json-pass-assert")
                .then()
                .statusCode(200);

        // Assertions must pass on the original JSON body.
        filter.addAssertionsForLastRequest((ValidatableResponse resp) -> {
            resp.statusCode(200);
            resp.body("type", equalTo("passOriginal"));
            resp.body("ok", equalTo(true));
            resp.body("value", equalTo(42));
        });
    }

    /**
     * Request 4: text/plain response, assertions present and pass.
     * Expected final status: MUTATION_EXECUTED.
     */
    @Test
    @Order(4)
    void textPassAssert() throws Exception {
        given()
                .when()
                .get("/text-pass-assert")
                .then()
                .statusCode(200);

        // Assertions for plain text response.
        filter.addAssertionsForLastRequest((ValidatableResponse resp) -> {
            resp.statusCode(200);
            resp.body(equalTo("Hello Mutator"));
            resp.body(containsString("Mutator"));
        });
    }

    /**
     * Request 5: text/plain response, no assertions.
     * Expected final status: DISCARDED_NO_ASSERTIONS.
     */
    @Test
    @Order(5)
    void textNoAssert() {
        given()
                .when()
                .get("/text-no-assert")
                .then()
                .statusCode(200);
        // No assertions attached for this request.
    }
}
