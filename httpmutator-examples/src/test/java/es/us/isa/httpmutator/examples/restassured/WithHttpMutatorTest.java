package es.us.isa.httpmutator.examples.restassured;

import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class is the HttpMutator-enabled counterpart of a baseline RestAssured
 * test suite for the GET /projects endpoint.
 *
 * <p>All request–response interactions and assertions are intentionally kept
 * identical to the original {@code WithoutHttpMutatorTest}. The only changes
 * are:
 * <ul>
 *   <li>Registering a {@link HttpMutatorRestAssuredFilter} in {@code @BeforeAll}
 *       so that HttpMutator can observe the traffic.</li>
 *   <li>Wrapping the original RestAssured assertions into a
 *       {@code Consumer<ValidatableResponse>} and passing it to
 *       {@code filter.addAssertionsForLastRequest(...)} in each test.</li>
 *   <li>Calling {@link #runMutationAnalysisAndReport()} once in {@code @AfterAll}
 *       to actually execute mutations and print a summary report.</li>
 * </ul>
 *
 * <p>If the {@code @AfterAll} method does not call
 * {@code filter.runAllMutations()}, then this class behaves exactly like the
 * original RestAssured test suite:
 * no mutants are generated, no extra requests are sent, and there is no
 * additional testing cost beyond the baseline tests.</p>
 */
public class WithHttpMutatorTest {

    private static String baseUrlOfSut = "http://localhost:43103";

    /**
     * Single shared filter instance used to:
     * <ul>
     *     <li>observe all original requests/responses</li>
     *     <li>store the user-provided assertions for each request</li>
     *     <li>later drive mutation analysis in {@code @AfterAll}</li>
     * </ul>
     */
    private static HttpMutatorRestAssuredFilter filter;

    @BeforeAll
    public static void initClass() {
        // Standard RestAssured configuration from the original test suite
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.urlEncodingEnabled = false;
        RestAssured.config = RestAssured.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))
                .redirect(redirectConfig().followRedirects(false));

        // --- Minimal addition for HttpMutator integration ---
        // Register a single filter so that HttpMutator can intercept requests
        // and later re-use the same tests as mutation oracles.
        filter = new HttpMutatorRestAssuredFilter();
        RestAssured.filters(filter);
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_0() {
        // Original RestAssured call kept as-is
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "offset=580&"
                        + "q=7TenZT")
                .then();

        // Original assertion, now stored as a reusable lambda
        Consumer<ValidatableResponse> assertFunc = vr ->
                vr.statusCode(200)
                        .assertThat()
                        .contentType("application/json")
                        .body("size()", equalTo(0));

        // Minimal change: instead of executing the assertion only once here,
        // we register it so HttpMutator can re-apply it to mutated responses.
        filter.addAssertionsForLastRequest(assertFunc);
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_1() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "limit=365&"
                        + "q=vmVXdJkjBlSs1E")
                .then();

        Consumer<ValidatableResponse> assertFunc = vr ->
                vr.statusCode(200)
                        .assertThat()
                        .contentType("application/json")
                        .body("size()", equalTo(0));

        filter.addAssertionsForLastRequest(assertFunc);
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_2() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "limit=228&"
                        + "offset=909&"
                        + "sortBy=Z4Q0lxF")
                .then();

        Consumer<ValidatableResponse> assertFunc = vr ->
                vr.statusCode(200)
                        .assertThat()
                        .contentType("application/json")
                        .body("size()", equalTo(0));

        filter.addAssertionsForLastRequest(assertFunc);
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_3() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "organizations=&"
                        + "limit=62&"
                        + "offset=673&"
                        + "q=VM2gh")
                .then();

        Consumer<ValidatableResponse> assertFunc = vr ->
                vr.statusCode(200)
                        .assertThat()
                        .contentType("application/json")
                        .body("size()", equalTo(0));

        filter.addAssertionsForLastRequest(assertFunc);
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_4() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "limit=714&"
                        + "offset=505&"
                        + "sortBy=q28qJ&"
                        + "q=VnqXMFohUX&"
                        + "language=R3cNd6TNB")
                .then();

        Consumer<ValidatableResponse> assertFunc = vr ->
                vr.statusCode(200)
                        .assertThat()
                        .contentType("application/json")
                        .body("size()", equalTo(0));

        filter.addAssertionsForLastRequest(assertFunc);
    }

    /**
     * Runs after all original tests have been executed.
     * <p>
     * This method is responsible for:
     * <ul>
     *     <li>Triggering HttpMutator to generate and execute mutants based on
     *         the observed responses and stored assertions.</li>
     *     <li>Producing a human-readable summary of mutation results for
     *         developers.</li>
     * </ul>
     *
     * <p>If the call to {@code filter.runAllMutations()} is removed or
     * commented out, this class effectively falls back to a standard
     * RestAssured test suite: no mutation testing work is performed and
     * no additional requests are sent.</p>
     */
    @AfterAll
    public static void runMutationAnalysisAndReport() {
        HttpMutatorRestAssuredFilter.MutationSummary summary = filter.runAllMutations();

        // Print a human-readable summary for developers
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

        List<HttpMutatorRestAssuredFilter.RequestMutationResult> results = summary.getPerRequestResults();
        for (HttpMutatorRestAssuredFilter.RequestMutationResult r : results) {
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
}