// ========================================
// HttpExchange - Canonical Request–Response Pair
// ========================================
package es.us.isa.httpmutator.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single HTTP interaction (request–response pair) in a unified,
 * canonical structure used throughout the HttpMutator pipeline.
 *
 * <p>The {@link StandardHttpResponse} is always present and is the normalized
 * structure consumed by the mutation engine.</p>
 *
 * <p>The {@link StandardHttpRequest} is optional. Some input formats (e.g., JSONL)
 * may not contain requests, whereas others (e.g., HAR) provide full request data.
 * Writers (such as HAR writers) may rely on this preserved request when emitting
 * mutated interactions.</p>
 *
 * <h2>Canonical Model</h2>
 * <ul>
 *     <li>{@code request} – optional; raw or converted request data</li>
 *     <li>{@code response} – required; canonical structure used for mutation</li>
 *     <li>{@code id} – stable identifier for the interaction</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *     <li>Request is never mutated by HttpMutator.</li>
 *     <li>Response is always normalized to {@link StandardHttpResponse}.</li>
 *     <li>{@code id} is propagated to writers and reporters for traceability.</li>
 * </ul>
 */
public final class HttpExchange {

    /** Canonical HTTP request (may be null depending on input format). */
    private final StandardHttpRequest request;

    /** Canonical HTTP response (never null, used by mutation engine). */
    private final StandardHttpResponse response;

    /** Stable identifier for correlation across readers/writers. */
    private final String id;

    /**
     * Constructs a canonical HTTP exchange.
     *
     * @param request  optional canonical request (may be null)
     * @param response canonical response (must not be null)
     * @param id       stable identifier (auto-generated if null/blank)
     */
    public HttpExchange(StandardHttpRequest request,
                        StandardHttpResponse response,
                        String id) {

        this.request = request;
        this.response = Objects.requireNonNull(response, "response must not be null");

        // Auto-generate id if missing
        if (id == null || id.trim().isEmpty()) {
            this.id = generateId();
        } else {
            this.id = id;
        }
    }

    public HttpExchange(StandardHttpRequest request, StandardHttpResponse response) {
        this(request, response, null);
    }

    /**
     * Unique ID generator for HttpExchange.
     * Current strategy: UUID-based, prefixed for readability.
     *
     * Can be replaced later with:
     * - sequential ID
     * - SHA-1 hash of request+response
     * - timestamp-based ID
     */
    private static String generateId() {
        return "ex-" + UUID.randomUUID();
    }

    public StandardHttpRequest getRequest() {
        return request;
    }

    public StandardHttpResponse getResponse() {
        return response;
    }

    public String getId() {
        return id;
    }
}