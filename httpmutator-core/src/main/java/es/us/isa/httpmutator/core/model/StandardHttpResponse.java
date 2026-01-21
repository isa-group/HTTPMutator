// ========================================
// StandardHttpResponse - HttpMutator Specialized Response Model
// ========================================
package es.us.isa.httpmutator.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * HttpMutator specialized response model.
 * Corresponds to JsonNode structure:
 * {
 * "Status Code": 200,
 * "Headers": {"content-type": "application/json"},
 * "Body": {...}
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandardHttpResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("Status Code")
    private int statusCode;

    @JsonProperty("Headers")
    private Map<String, Object> headers;

    @JsonProperty("Body")
    private JsonNode body;

    // ========================================
    // Constructors
    // ========================================

    private StandardHttpResponse() {}

    public StandardHttpResponse(int statusCode, Map<String, Object> headers, JsonNode body) {
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body;
    }

    // ========================================
    // Static Factory Methods (Convenient Creation)
    // ========================================

    /**
     * Create a basic response
     */
    public static StandardHttpResponse of(int statusCode, JsonNode body) {
        return new StandardHttpResponse(statusCode, new HashMap<>(), body);
    }

    /**
     * Create a response with headers
     */
    public static StandardHttpResponse of(int statusCode, Map<String, Object> headers, JsonNode body) {
        return new StandardHttpResponse(statusCode, headers, body);
    }

    /**
     * Create from JsonNode (adapt existing data)
     */
    public static StandardHttpResponse fromJsonNode(JsonNode node) {
        try {
            return OBJECT_MAPPER.treeToValue(node, StandardHttpResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to convert JsonNode to StandardHttpResponse", e);
        }
    }

    /**
     * Create from JSON string
     */
    public static StandardHttpResponse fromJson(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return fromJsonNode(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }

    // ========================================
    // Getter/Setter Methods
    // ========================================

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public JsonNode getBody() {
        return body;
    }

    public void setBody(JsonNode body) {
        this.body = body;
    }

        /**
     * Convert to JsonNode (format required by HttpMutator)
     */
    public JsonNode toJsonNode() {
        return OBJECT_MAPPER.valueToTree(this);
    }
    
    /**
     * Convert to JSON string
     */
    public String toJsonString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON string", e);
        }
    }
    
    /**
     * Validate if it's a valid input format for status code mutation
     */
    @JsonIgnore
    public boolean isValidForStatusCodeMutator() {
        return 200 <= statusCode && statusCode < 600;
    }

    @JsonIgnore
    public boolean isValidForBodyMutator() {
        return body != null;
    }

    @JsonIgnore
    public boolean isValidForHeadersMutator() {
        return headers != null && !headers.isEmpty();
    }

}
