package es.us.isa.httpmutator.integrations.restassured;

import es.us.isa.httpmutator.core.HttpMutator;
import es.us.isa.httpmutator.core.converter.ConversionException;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.reporter.CsvReporter;
import es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy;
import es.us.isa.httpmutator.core.strategy.MutationStrategy;
import es.us.isa.httpmutator.core.util.RandomUtils;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * REST Assured {@link Filter} integration for HttpMutator.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Observe and record <b>all</b> HTTP responses passing through the filter.</li>
 *   <li>Allow test code to attach assertions to the <b>last observed response</b>.</li>
 *   <li>Run HttpMutator-based mutation testing only on requests with valid assertions
 *       that pass on the original (unmutated) response.</li>
 *   <li>Track discard reasons for requests that cannot be used for mutation testing.</li>
 *   <li>Provide a summary object separating discarded requests from mutation results.</li>
 * </ul>
 * <p>
 * This implementation is NOT thread-safe and assumes a single-threaded test execution model.
 */
public class HttpMutatorRestAssuredFilter implements Filter {

    private final HttpMutator httpMutator;
    private final Path reportDir;
    private final MutationSummaryCsvReporter reporter;

    private static final Logger log = LogManager.getLogger(HttpMutatorRestAssuredFilter.class);

    /**
     * Global counter for assigning stable labels like "request-0", "request-1", ...
     */
    private static final AtomicInteger GLOBAL_REQUEST_INDEX = new AtomicInteger(0);

    /**
     * All observed interactions, in order.
     */
    private final List<RecordedInteraction> recordedInteractions = new ArrayList<>();

    /**
     * The last observed RestAssured Response (used by addAssertionsForLastRequest).
     */
    private Response lastResponse;

    /**
     * Index in {@link #recordedInteractions} of the last observed interaction.
     */
    private int lastInteractionIndex = -1;

    /**
     * How to handle original assertion failures.
     */
    private final OriginalAssertionFailurePolicy originalAssertionFailurePolicy;

    /**
     * Creates a new filter with explicit configuration.
     *
     * @param randomSeed       seed for HttpMutator's randomness (via {@link RandomUtils}).
     * @param mutationStrategy strategy selecting which mutants to execute.
     * @param reportDir        directory where reports could be written (not used directly here, but kept for future extensions).
     */
    public HttpMutatorRestAssuredFilter(long randomSeed, MutationStrategy mutationStrategy, Path reportDir, String reportName, OriginalAssertionFailurePolicy originalAssertionFailurePolicy) {
        this.reportDir = reportDir;
        try {
            Files.createDirectories(reportDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.httpMutator = new HttpMutator(randomSeed).withMutationStrategy(mutationStrategy).addReporter(new CsvReporter(reportDir.resolve(reportName + ".csv")));
        this.reporter = new MutationSummaryCsvReporter(reportDir.resolve(reportName + "-assert-summary.csv"));
        this.originalAssertionFailurePolicy = originalAssertionFailurePolicy;
    }

    /**
     * Creates a new filter with default seed, strategy, and report directory.
     */
    public HttpMutatorRestAssuredFilter() {
        this(42L, new AllOperatorsStrategy(), defaultReportDir(), "mutants-csv-reporter", OriginalAssertionFailurePolicy.THROW);
    }

    static Path defaultReportDir() {
        return Paths.get("target", "httpmutator-restassured");
    }

    /**
     * Internal status of how a recorded request was ultimately treated.
     */
    public enum RequestStatus {
        /**
         * Request was observed but has not yet been processed further.
         */
        OBSERVED,
        /**
         * Request was discarded because no assertions were ever provided.
         */
        DISCARDED_NO_ASSERTIONS,
        /**
         * Request was discarded because the original assertions failed.
         */
        DISCARDED_ORIGINAL_ASSERTION_FAILED,
        /**
         * Mutation testing was executed for this request.
         */
        MUTATION_EXECUTED
    }

    /**
     * Internal discard categories, used for statistics and diagnostics.
     */
    public enum DiscardReason {
        NO_ASSERTIONS_PROVIDED, ORIGINAL_ASSERTION_FAILED
    }

    /**
     * One observed interaction: original response plus optional assertions and mutation statistics.
     */
    private static final class RecordedInteraction {

        private final String label;
        private final Response originalResponse; // raw RestAssured response
        private StandardHttpResponse originalStandardResponse;
        private Consumer<ValidatableResponse> assertions;

        private RequestStatus status;
        private DiscardReason discardReason;
        private String message;

        private int totalMutants;
        private int killedMutants;

        RecordedInteraction(String label, Response originalResponse) {
            this.label = label;
            this.originalResponse = originalResponse;
            this.status = RequestStatus.OBSERVED;
        }

        String getLabel() {
            return label;
        }

        Response getOriginalResponse() {
            return originalResponse;
        }

        StandardHttpResponse getOriginalStandardResponse() {
            return originalStandardResponse;
        }

        void setOriginalStandardResponse(StandardHttpResponse originalStandardResponse) {
            this.originalStandardResponse = originalStandardResponse;
        }

        Consumer<ValidatableResponse> getAssertions() {
            return assertions;
        }

        void setAssertions(Consumer<ValidatableResponse> assertions) {
            this.assertions = assertions;
        }

        RequestStatus getStatus() {
            return status;
        }

        void setStatus(RequestStatus status) {
            this.status = status;
        }

        DiscardReason getDiscardReason() {
            return discardReason;
        }

        void setDiscardReason(DiscardReason discardReason) {
            this.discardReason = discardReason;
        }

        String getMessage() {
            return message;
        }

        void setMessage(String message) {
            this.message = message;
        }

        int getTotalMutants() {
            return totalMutants;
        }

        int getKilledMutants() {
            return killedMutants;
        }

        void setMutationStats(int totalMutants, int killedMutants) {
            this.totalMutants = totalMutants;
            this.killedMutants = killedMutants;
        }
    }

    /**
     * Per-request mutation result, used in summaries.
     */
    public static final class RequestMutationResult {
        private final String label;
        private final RequestStatus status;
        private final String message;
        private final int totalMutants;
        private final int killedMutants;

        public RequestMutationResult(String label, RequestStatus status, String message, int totalMutants, int killedMutants) {
            this.label = label;
            this.status = status;
            this.message = message;
            this.totalMutants = totalMutants;
            this.killedMutants = killedMutants;
        }

        public String getLabel() {
            return label;
        }

        public RequestStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public int getTotalMutants() {
            return totalMutants;
        }

        public int getKilledMutants() {
            return killedMutants;
        }

        /**
         * Returns the mutation score for this request: killed / total, or 0.0 if total is 0.
         */
        public double getMutationScore() {
            if (totalMutants == 0) {
                return 0.0d;
            }
            return (double) killedMutants / (double) totalMutants;
        }
    }

    /**
     * Policy for handling failures of the original assertions.
     * <ul>
     *   <li>DISCARD: record the failure and skip mutation testing for this request.</li>
     *   <li>THROW: record the failure (optional) and rethrow the AssertionError so the test fails.</li>
     * </ul>
     */
    public enum OriginalAssertionFailurePolicy {
        DISCARD, THROW
    }

    /**
     * Aggregated summary for all observed requests and their mutation/discard status.
     */
    public static final class MutationSummary {
        private final List<RequestMutationResult> perRequestResults;

        private final int totalObservedRequests;
        private final int totalRequestsWithAssertions;
        private final int discardedNoAssertions;
        private final int discardedOriginalAssertionFailed;
        private final int mutationExecutedRequests;
        private final int totalMutants;
        private final int killedMutants;

        public MutationSummary(List<RequestMutationResult> perRequestResults, int totalObservedRequests, int totalRequestsWithAssertions, int discardedNoAssertions, int discardedOriginalAssertionFailed, int mutationExecutedRequests, int totalMutants, int killedMutants) {
            this.perRequestResults = perRequestResults;
            this.totalObservedRequests = totalObservedRequests;
            this.totalRequestsWithAssertions = totalRequestsWithAssertions;
            this.discardedNoAssertions = discardedNoAssertions;
            this.discardedOriginalAssertionFailed = discardedOriginalAssertionFailed;
            this.mutationExecutedRequests = mutationExecutedRequests;
            this.totalMutants = totalMutants;
            this.killedMutants = killedMutants;
        }

        public List<RequestMutationResult> getPerRequestResults() {
            return perRequestResults;
        }

        public int getTotalObservedRequests() {
            return totalObservedRequests;
        }

        public int getTotalRequestsWithAssertions() {
            return totalRequestsWithAssertions;
        }

        public int getDiscardedNoAssertions() {
            return discardedNoAssertions;
        }

        public int getDiscardedOriginalAssertionFailed() {
            return discardedOriginalAssertionFailed;
        }

        public int getMutationExecutedRequests() {
            return mutationExecutedRequests;
        }

        public int getTotalMutants() {
            return totalMutants;
        }

        public int getKilledMutants() {
            return killedMutants;
        }

        /**
         * Overall mutation score across all requests: killed / total, or 0.0 if total is 0.
         */
        public double getOverallMutationScore() {
            if (totalMutants == 0) {
                return 0.0d;
            }
            return (double) killedMutants / (double) totalMutants;
        }
    }

    /**
     * Filter hook: records every response that passes through and assigns it
     * a stable global label (e.g., "request-0", "request-1", ...).
     *
     * <p>The last observed response can subsequently be associated with assertions
     * via {@link #addAssertionsForLastRequest(Consumer)}.</p>
     */
    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {

        // Let RestAssured perform the actual HTTP call
        Response response = ctx.next(requestSpec, responseSpec);

        // Assign a stable label for this interaction
        String label = "request-" + GLOBAL_REQUEST_INDEX.getAndIncrement();

        // Record this interaction: initially with status OBSERVED and no assertions attached
        RecordedInteraction interaction = new RecordedInteraction(label, response);
        recordedInteractions.add(interaction);

        // Track this as the "last" response so user can attach assertions
        this.lastResponse = response;
        this.lastInteractionIndex = recordedInteractions.size() - 1;

        return response;
    }

    /**
     * Attaches assertions to the last observed response, validates them on the
     * original (unmutated) response, and schedules the request for mutation testing
     * only if the assertions pass.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>If no response has been captured, throws {@link IllegalStateException}.</li>
     *   <li>If {@code assertions} is {@code null}, throws {@link IllegalStateException}.</li>
     *   <li>Converts the last response to {@link StandardHttpResponse}.</li>
     *   <li>Applies the assertions to the original response:
     *     <ul>
     *       <li>If assertions throw {@link AssertionError}, the request is marked as
     *           {@link RequestStatus#DISCARDED_ORIGINAL_ASSERTION_FAILED} and will not
     *           be used for mutation testing.</li>
     *       <li>If assertions succeed, the request is recorded as a mutation candidate
     *           with its {@link StandardHttpResponse} and assertion function.</li>
     *     </ul>
     *   </li>
     *   <li>Clears the internal reference to the last response afterwards.</li>
     * </ul>
     *
     * @param assertions the assertion logic to apply on the original and mutated responses.
     * @throws ConversionException if conversion from RestAssured {@link Response} to
     *                             {@link StandardHttpResponse} fails.
     */
    public void addAssertionsForLastRequest(Consumer<ValidatableResponse> assertions) {
        if (lastResponse == null || lastInteractionIndex < 0) {
            throw new IllegalStateException("No response captured yet. Make sure a RestAssured request was executed " + "before calling addAssertionsForLastRequest.");
        }
        if (assertions == null) {
            throw new IllegalStateException("Assertions must not be null");
        }

        // Lookup the last recorded interaction
        RecordedInteraction interaction = recordedInteractions.get(lastInteractionIndex);

        // 1) Convert the last response into StandardHttpResponse (may throw ConversionException)
        StandardHttpResponse stdResponse = null;
        try {
            stdResponse = RestAssuredBidirectionalConverter.INSTANCE.toStandardResponse(lastResponse);
        } catch (ConversionException e) {
            throw new RuntimeException(e);
        }
        interaction.setOriginalStandardResponse(stdResponse);
        interaction.setAssertions(assertions);

        // 2) Apply assertions to the original (unmutated) response
        ValidatableResponse originalValidatable = lastResponse.then();
        try {
            // If this throws AssertionError, we must discard this interaction
            assertions.accept(originalValidatable);
        } catch (AssertionError ae) {
            interaction.setStatus(RequestStatus.DISCARDED_ORIGINAL_ASSERTION_FAILED);
            interaction.setDiscardReason(DiscardReason.ORIGINAL_ASSERTION_FAILED);
            interaction.setMessage("Original assertions failed before mutation; request discarded.");
            // Clear last response tracking
            lastResponse = null;
            lastInteractionIndex = -1;

            if (originalAssertionFailurePolicy == OriginalAssertionFailurePolicy.THROW) {
                // Re-throw so the test fails immediately (JUnit-style behavior)
                throw ae;
            } else {
                // DISCARD policy: we just skip mutation for this request
                return;
            }
        }

        // 3) Original assertions succeeded; record as a mutation candidate
        interaction.setStatus(RequestStatus.OBSERVED);
        interaction.setDiscardReason(null);
        interaction.setMessage("Assertions attached and original response passed.");

        // Clear last response tracking
        lastResponse = null;
        lastInteractionIndex = -1;
    }

    /**
     * Runs mutation testing for all recorded interactions that:
     * <ul>
     *   <li>have a valid {@link StandardHttpResponse},</li>
     *   <li>have assertions attached, and</li>
     *   <li>passed the original assertion check (i.e., were not discarded).</li>
     * </ul>
     *
     * <p>All other recorded interactions are categorized into discard reasons:
     * <ul>
     *   <li>{@link DiscardReason#NO_ASSERTIONS_PROVIDED}</li>
     *   <li>{@link DiscardReason#ORIGINAL_ASSERTION_FAILED}</li>
     * </ul>
     * and are included in the returned summary.</p>
     *
     * @return a {@link MutationSummary} containing per-request results and aggregated statistics.
     */
    public MutationSummary runAllMutations() {
        if (httpMutator == null) {
            throw new IllegalStateException("HttpMutatorRestAssuredFilter not initialized with an HttpMutator");
        }

        int totalObserved = recordedInteractions.size();
        int totalWithAssertions = 0;
        int discardedNoAssertions = 0;
        int discardedOriginalAssertionFailed = 0;
        int mutationExecuted = 0;
        int totalMutantsOverall = 0;
        int killedMutantsOverall = 0;

        List<RequestMutationResult> perRequestResults = new ArrayList<>();

        // First count how many have assertions at all (regardless of later discard)
        for (RecordedInteraction interaction : recordedInteractions) {
            if (interaction.getAssertions() != null) {
                totalWithAssertions++;
                log.debug("Request Id = {} has assertions attached.", interaction.getLabel());
            }
        }

        // Process each recorded interaction
        for (RecordedInteraction interaction : recordedInteractions) {
            RequestMutationResult result;

            // If no assertions were ever provided, mark as discarded for that reason
            if (interaction.getAssertions() == null && interaction.getStatus() == RequestStatus.OBSERVED) {

                interaction.setStatus(RequestStatus.DISCARDED_NO_ASSERTIONS);
                interaction.setDiscardReason(DiscardReason.NO_ASSERTIONS_PROVIDED);
                interaction.setMessage("Request observed but no assertions were provided; skipping mutation.");
            }

            RequestStatus status = interaction.getStatus();
            switch (status) {
                case DISCARDED_NO_ASSERTIONS:
                    discardedNoAssertions++;
                    result = new RequestMutationResult(interaction.getLabel(), RequestStatus.DISCARDED_NO_ASSERTIONS, interaction.getMessage(), 0, 0);
                    break;

                case DISCARDED_ORIGINAL_ASSERTION_FAILED:
                    discardedOriginalAssertionFailed++;
                    result = new RequestMutationResult(interaction.getLabel(), RequestStatus.DISCARDED_ORIGINAL_ASSERTION_FAILED, interaction.getMessage(), 0, 0);
                    break;

                case OBSERVED:
                case MUTATION_EXECUTED:
                default:
                    // Only run mutation if we have both a standard response and assertions
                    if (interaction.getAssertions() != null && interaction.getOriginalStandardResponse() != null) {

                        RequestMutationResult mutationResult = runMutationForRecord(interaction);
                        mutationExecuted++;
                        totalMutantsOverall += mutationResult.getTotalMutants();
                        killedMutantsOverall += mutationResult.getKilledMutants();
                        result = mutationResult;
                    } else {
                        // Fallback discard: missing required data for mutation
                        interaction.setStatus(RequestStatus.DISCARDED_NO_ASSERTIONS);
                        interaction.setDiscardReason(DiscardReason.NO_ASSERTIONS_PROVIDED);
                        interaction.setMessage("Request observed but missing assertions or standard response; skipping mutation.");
                        discardedNoAssertions++;
                        result = new RequestMutationResult(interaction.getLabel(), interaction.getStatus(), interaction.getMessage(), 0, 0);
                    }
                    break;
            }

            perRequestResults.add(result);
        }

        try {
            httpMutator.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MutationSummary summary =  new MutationSummary(perRequestResults, totalObserved, totalWithAssertions, discardedNoAssertions, discardedOriginalAssertionFailed, mutationExecuted, totalMutantsOverall, killedMutantsOverall);
        try {
            reporter.write(summary);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return summary;
    }

    /**
     * Convenience method that runs mutation for all recorded interactions
     * and prints a human-readable summary to {@code System.out}.
     *
     * @return the {@link MutationSummary} produced by {@link #runAllMutations()}.
     */
    public MutationSummary runAllMutationsAndPrintSummary() {
        MutationSummary summary = runAllMutations();
        printSummary(summary);
        return summary;
    }

    /**
     * Print a human-readable summary of mutation results to {@code System.out}.
     *
     * <p>This is primarily intended for developers to quickly inspect how many
     * requests were observed, how many were discarded, and basic per-request
     * mutation scores, without having to parse CSV/JSON reports.</p>
     *
     * @param summary the summary to print; if {@code null}, a short message is printed.
     */
    public void printSummary(MutationSummary summary) {
        if (summary == null) {
            System.out.println("=== HttpMutator Summary ===");
            System.out.println("Summary is null (no mutations run?).");
            return;
        }

        System.out.println("=== HttpMutator Summary ===");
        System.out.println("Total observed: " + summary.getTotalObservedRequests());
        System.out.println("With assertions: " + summary.getTotalRequestsWithAssertions());
        System.out.println("Discarded (no assertions): " + summary.getDiscardedNoAssertions());
        System.out.println("Discarded (original assertion failed): " + summary.getDiscardedOriginalAssertionFailed());
        System.out.println("Mutation executed requests: " + summary.getMutationExecutedRequests());
        System.out.println("Total mutants: " + summary.getTotalMutants());
        System.out.println("Killed mutants: " + summary.getKilledMutants());
        System.out.println("Overall mutation score: " + summary.getOverallMutationScore());
        System.out.println("--- Per-request results ---");

        List<RequestMutationResult> results = summary.getPerRequestResults();
        for (RequestMutationResult r : results) {
            System.out.printf(
                    "  %s -> status=%s, msg=%s, total=%d, killed=%d, score=%.2f%n",
                    r.getLabel(),
                    r.getStatus(),
                    r.getMessage(),
                    r.getTotalMutants(),
                    r.getKilledMutants(),
                    r.getMutationScore()
            );
        }
    }

    /**
     * Runs mutation testing for a single recorded interaction:
     * <ol>
     *   <li>Uses {@link HttpMutator#mutate} to obtain a mutant group.</li>
     *   <li>Uses {@link MutationStrategy} to select which mutants to execute.</li>
     *   <li>For each selected mutant:
     *     <ul>
     *       <li>Builds a mutated {@link StandardHttpResponse}.</li>
     *       <li>Converts it back to a RestAssured {@link Response} via {@link RestAssuredBidirectionalConverter}.</li>
     *       <li>Wraps it as {@link ValidatableResponse} and applies the stored assertions.</li>
     *       <li>Counts total mutants vs. killed mutants (assertion failures).</li>
     *     </ul>
     *   </li>
     *   <li>Updates the interaction's status and mutation statistics.</li>
     * </ol>
     *
     * @param interaction the interaction to mutate.
     * @return the per-request mutation result.
     */
    private RequestMutationResult runMutationForRecord(RecordedInteraction interaction) {
        final StandardHttpResponse std = interaction.getOriginalStandardResponse();
        final Consumer<ValidatableResponse> assertFunc = interaction.getAssertions();

        final AtomicInteger total = new AtomicInteger();
        final AtomicInteger killed = new AtomicInteger();

        // Delegate to HttpMutator: it will provide a MutantGroup to inspect
        httpMutator.mutate(std, interaction.label, mutated -> {
            try {
                // Convert mutated response back to a RestAssured Response
                Response raResp = RestAssuredBidirectionalConverter.INSTANCE.fromStandardResponse(mutated);
                ValidatableResponse valMResp = raResp.then();

                total.incrementAndGet();
                log.debug("Applying assertions to mutant for request Id = {}, mutant #{}.", interaction.getLabel(), total.get());

                try {
                    // Apply user assertions; if they fail, we consider the mutant "killed"
                    assertFunc.accept(valMResp);
                } catch (AssertionError | Exception e) {
                    killed.incrementAndGet();
                }
            } catch (ConversionException e) {
                // If conversion fails, skip this mutant but continue processing others
                // (we do not increment 'total' in this case).
            }
        });

        int totalMutants = total.get();
        int killedMutants = killed.get();

        interaction.setMutationStats(totalMutants, killedMutants);
        interaction.setStatus(RequestStatus.MUTATION_EXECUTED);
        interaction.setMessage("Mutation executed on " + totalMutants + " mutants; killed " + killedMutants + ".");

        return new RequestMutationResult(interaction.getLabel(), RequestStatus.MUTATION_EXECUTED, interaction.getMessage(), totalMutants, killedMutants);
    }
}
