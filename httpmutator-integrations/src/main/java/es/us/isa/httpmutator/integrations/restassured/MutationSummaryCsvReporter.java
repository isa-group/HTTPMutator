package es.us.isa.httpmutator.integrations.restassured;

import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.MutationSummary;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.RequestMutationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes a {@link MutationSummary} produced by {@link HttpMutatorRestAssuredFilter}
 * to a CSV file.
 *
 * <p>Format:</p>
 * <ul>
 *   <li>Header row with per-request and overall-summary columns</li>
 *   <li>One row per request: label, status, message, totalMutants, killedMutants</li>
 *   <li>Final row named {@code SUMMARY} that aggregates totals from {@link MutationSummary}</li>
 * </ul>
 */
public class MutationSummaryCsvReporter {

    private final Path csvPath;

    /**
     * @param csvPath path to the CSV file to be written (will be created or overwritten)
     */
    public MutationSummaryCsvReporter(Path csvPath) {
        this.csvPath = csvPath;
    }

    /**
     * Writes the given {@link MutationSummary} to the configured CSV file.
     *
     * @param summary the mutation summary to serialize
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void write(MutationSummary summary) throws IOException {
        if (summary == null) {
            throw new IllegalArgumentException("summary must not be null");
        }

        // Ensure parent directory exists
        Path parent = csvPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            // Header
            writer.write(String.join(",",
                    "label",
                    "status",
                    "message",
                    "totalMutants",
                    "killedMutants",
                    "totalObservedRequests",
                    "totalRequestsWithAssertions",
                    "discardedNoAssertions",
                    "discardedOriginalAssertionFailed",
                    "mutationExecutedRequests",
                    "overallMutationScore"
            ));
            writer.newLine();

            // Per-request rows
            List<RequestMutationResult> results = summary.getPerRequestResults();
            for (RequestMutationResult r : results) {
                writer.write(String.join(",",
                        csv(r.getLabel()),
                        csv(r.getStatus() != null ? r.getStatus().name() : ""),
                        csv(r.getMessage()),
                        String.valueOf(r.getTotalMutants()),
                        String.valueOf(r.getKilledMutants()),
                        "",     // summary-level fields left empty for request rows
                        "",
                        "",
                        "",
                        "",
                        ""
                ));
                writer.newLine();
            }

            // Final summary row
            writer.write(String.join(",",
                    csv("SUMMARY"),
                    csv(""),
                    csv("Aggregated totals"),
                    String.valueOf(summary.getTotalMutants()),
                    String.valueOf(summary.getKilledMutants()),
                    String.valueOf(summary.getTotalObservedRequests()),
                    String.valueOf(summary.getTotalRequestsWithAssertions()),
                    String.valueOf(summary.getDiscardedNoAssertions()),
                    String.valueOf(summary.getDiscardedOriginalAssertionFailed()),
                    String.valueOf(summary.getMutationExecutedRequests()),
                    String.valueOf(summary.getOverallMutationScore())
            ));
            writer.newLine();
        }
    }

    /**
     * Minimal CSV escaping:
     * - null -> empty
     * - escape double quotes by doubling them
     * - wrap in quotes so commas/newlines are safe
     */
    private static String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}