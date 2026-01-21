// ========================================
// StandardHttpRequest - HttpMutator Specialized Request Model
// ========================================
package es.us.isa.httpmutator.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HttpMutator specialized request model.
 *
 * Canonical structure:
 * {
 *   "Method": "GET",
 *   "URL": "https://example.com/api",
 *   "Headers": { "Accept": "application/json" },
 *   "Body": {...}  // may be null
 * }
 *
 * Notes:
 * - Request is never mutated by HttpMutator.
 * - Provided only for readers/writers (HAR, JSONL, REST-assured integrations).
 */
public class StandardHttpRequest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("Method")
    private String method;

    @JsonProperty("URL")
    private String url;

    /**
     * Headers allow Object because HAR values may be arrays or strings.
     */
    @JsonProperty("Headers")
    private Map<String, Object> headers;

    /**
     * Body is canonical JsonNode (null allowed).
     */
    @JsonProperty("Body")
    private JsonNode body;

    // ========================================
    // Constructors
    // ========================================

    private StandardHttpRequest() {}

    public StandardHttpRequest(String method,
                               String url,
                               Map<String, Object> headers,
                               JsonNode body) {

        this.method = Objects.requireNonNull(method, "method must not be null");
        this.url = Objects.requireNonNull(url, "url must not be null");
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body;  // may be null
    }

    // ========================================
    // Factory Methods
    // ========================================

    public static StandardHttpRequest of(String method, String url) {
        return new StandardHttpRequest(method, url, new HashMap<>(), null);
    }

    public static StandardHttpRequest of(String method, String url, Map<String, Object> headers) {
        return new StandardHttpRequest(method, url, headers, null);
    }

    public static StandardHttpRequest of(String method,
                                         String url,
                                         Map<String, Object> headers,
                                         JsonNode body) {
        return new StandardHttpRequest(method, url, headers, body);
    }

    public static StandardHttpRequest fromJsonNode(JsonNode node) {
        try {
            return OBJECT_MAPPER.treeToValue(node, StandardHttpRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to convert JsonNode to StandardHttpRequest", e);
        }
    }

    public static StandardHttpRequest fromJson(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return fromJsonNode(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }

    // ========================================
    // Getters / Setters
    // ========================================

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = Objects.requireNonNull(method);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = Objects.requireNonNull(url);
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }

    public JsonNode getBody() {
        return body;
    }

    public void setBody(JsonNode body) {
        this.body = body;
    }

    // ========================================
    // Serialization
    // ========================================

    public JsonNode toJsonNode() {
        return OBJECT_MAPPER.valueToTree(this);
    }

    public String toJsonString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert request to JSON string", e);
        }
    }

    // ========================================
    // Validation helpers (optional)
    // ========================================

    public boolean hasBody() {
        return body != null;
    }

    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }
}