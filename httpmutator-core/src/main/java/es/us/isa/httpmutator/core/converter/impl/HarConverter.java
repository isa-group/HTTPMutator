package es.us.isa.httpmutator.core.converter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.httpmutator.core.converter.BidirectionalConverter;
import es.us.isa.httpmutator.core.converter.ConversionException;
import es.us.isa.httpmutator.core.model.StandardHttpRequest;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * HAR-specific implementation of {@link BidirectionalConverter} that converts
 * between HAR request/response objects (represented as {@link JsonNode}) and
 * {@link StandardHttpRequest}/{@link StandardHttpResponse}.
 *
 * <p>On the HAR side, this converter expects/produces JSON structures following
 * the HAR 1.2 specification:</p>
 *
 * <pre>
 * // Response
 * {
 *   "status": 200,
 *   "headers": [ { "name": "...", "value": "..." }, ... ],
 *   "content": {
 *       "mimeType": "application/json",
 *       "text": "{ ... raw or JSON-encoded body ... }"
 *   }
 * }
 *
 * // Request
 * {
 *   "method": "GET",
 *   "url": "https://example.com/api",
 *   "headers": [ { "name": "...", "value": "..." }, ... ],
 *   "postData": {
 *       "mimeType": "application/json",
 *       "text": "{ ... raw or JSON-encoded body ... }"
 *   }
 * }
 * </pre>
 *
 * <p>On the standard side, it uses the canonical representations handled by
 * {@link StandardHttpRequest} and {@link StandardHttpResponse}:</p>
 *
 * <pre>
 * // StandardHttpResponse JSON
 * {
 *   "Status Code": 200,
 *   "Headers": { "Content-Type": "application/json", ... },
 *   "Body": { ... } or "raw string"
 * }
 *
 * // StandardHttpRequest JSON
 * {
 *   "Method": "GET",
 *   "URL": "https://example.com/api",
 *   "Headers": { "Accept": "application/json", ... },
 *   "Body": { ... } or "raw string"
 * }
 * </pre>
 */
public class HarConverter implements BidirectionalConverter<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ======================================================
    // Response Conversion
    // ======================================================

    @Override
    public StandardHttpResponse toStandardResponse(JsonNode originalResponse) throws ConversionException {
        if (originalResponse == null || originalResponse.isNull()) {
            throw new ConversionException("HAR response JsonNode must not be null");
        }

        try {
            // Build canonical JSON: Status, Headers, Body
            ObjectNode canonical = MAPPER.createObjectNode();

            // Status
            int status = originalResponse.path("status").asInt(0);
            canonical.put("Status Code", status);

            // Headers: HAR "headers" array -> canonical "Headers" object
            ObjectNode headers = MAPPER.createObjectNode();
            for (JsonNode h : originalResponse.path("headers")) {
                String name = h.path("name").asText();
                String value = h.path("value").asText();
                if (!name.isEmpty()) {
                    headers.put(name, value);
                }
            }
            canonical.set("Headers", headers);

            // Body: HAR "content.text"
            JsonNode content = originalResponse.path("content");
            if (!content.isMissingNode() && content.has("text")) {
                String bodyText = content.path("text").asText();
                // Try parsing body as JSON; fall back to raw string
                try {
                    JsonNode bodyNode = MAPPER.readTree(bodyText);
                    canonical.set("Body", bodyNode);
                } catch (Exception e) {
                    canonical.put("Body", bodyText);
                }
            } else {
                canonical.putNull("Body");
            }

            // Delegate to StandardHttpResponse factory
            return StandardHttpResponse.fromJsonNode(canonical);

        } catch (Exception e) {
            throw new ConversionException("Failed to convert HAR response to StandardHttpResponse", e);
        }
    }

    @Override
    public JsonNode fromStandardResponse(StandardHttpResponse standardResponse) throws ConversionException {
        if (standardResponse == null) {
            throw new ConversionException("StandardHttpResponse must not be null");
        }

        try {
            // Convert StandardHttpResponse to its canonical JSON representation
            JsonNode canonical = standardResponse.toJsonNode();
            if (canonical == null || canonical.isNull()) {
                throw new ConversionException("StandardHttpResponse.toJsonNode() returned null");
            }

            // HAR response object to build
            ObjectNode harResponse = MAPPER.createObjectNode();

            // Status
            int status = canonical.path("Status Code").asInt(0);
            harResponse.put("status", status);

            // Headers: canonical Headers object -> HAR headers array
            ObjectNode canonicalHeaders = asObjectNodeOrEmpty(canonical.path("Headers"));
            ArrayNode harHeaders = harResponse.putArray("headers");
            canonicalHeaders.fields().forEachRemaining(entry -> {
                ObjectNode h = harResponse.objectNode();
                h.put("name", entry.getKey());
                h.put("value", entry.getValue().asText());
                harHeaders.add(h);
            });

            // Body: canonical Body -> HAR content.text
            JsonNode body = canonical.path("Body");
            ObjectNode content = harResponse.putObject("content");

            // Best-effort MIME type; callers may post-process this if needed
            content.put("mimeType", "application/json");
            content.put("text", body.isValueNode() ? body.asText() : body.toString());

            return harResponse;

        } catch (Exception e) {
            throw new ConversionException("Failed to convert StandardHttpResponse to HAR response JsonNode", e);
        }
    }

    // ======================================================
    // Request Conversion
    // ======================================================

    @Override
    public StandardHttpRequest toStandardRequest(JsonNode originalRequest) throws ConversionException {
        if (originalRequest == null || originalRequest.isNull()) {
            // For formats without request, the caller can decide to pass null
            throw new ConversionException("HAR request JsonNode must not be null");
        }

        try {
            // Method
            String method = originalRequest.path("method").asText("");
            // URL
            String url = originalRequest.path("url").asText("");

            // Headers: HAR headers[] -> Map<String,Object>
            Map<String, Object> headers = new HashMap<>();
            JsonNode harHeaders = originalRequest.path("headers");
            if (harHeaders.isArray()) {
                for (JsonNode h : harHeaders) {
                    String name = h.path("name").asText();
                    String value = h.path("value").asText();
                    if (!name.isEmpty()) {
                        headers.put(name, value);
                    }
                }
            }

            // Body: HAR postData.text
            JsonNode postData = originalRequest.path("postData");
            JsonNode bodyNode = null;
            if (!postData.isMissingNode() && postData.has("text")) {
                String bodyText = postData.path("text").asText();
                if (bodyText != null && !bodyText.isEmpty()) {
                    try {
                        bodyNode = MAPPER.readTree(bodyText);
                    } catch (Exception e) {
                        // Not valid JSON → keep as raw string
                        bodyNode = MAPPER.getNodeFactory().textNode(bodyText);
                    }
                }
            }

            return new StandardHttpRequest(method, url, headers, bodyNode);

        } catch (Exception e) {
            throw new ConversionException("Failed to convert HAR request to StandardHttpRequest", e);
        }
    }

    @Override
    public JsonNode fromStandardRequest(StandardHttpRequest standardRequest) throws ConversionException {
        if (standardRequest == null) {
            throw new ConversionException("StandardHttpRequest must not be null");
        }

        try {
            ObjectNode harRequest = MAPPER.createObjectNode();

            // Method + URL
            harRequest.put("method", standardRequest.getMethod());
            harRequest.put("url", standardRequest.getUrl());

            // Headers map -> HAR headers[]
            ArrayNode harHeaders = harRequest.putArray("headers");
            Map<String, Object> headers = standardRequest.getHeaders();
            if (headers != null) {
                for (Map.Entry<String, Object> e : headers.entrySet()) {
                    ObjectNode h = harRequest.objectNode();
                    h.put("name", e.getKey());
                    h.put("value", e.getValue() != null ? e.getValue().toString() : "");
                    harHeaders.add(h);
                }
            }

            // Body: StandardHttpRequest.body -> HAR postData.text
            JsonNode body = standardRequest.getBody();
            if (body != null && !body.isNull()) {
                ObjectNode postData = harRequest.putObject("postData");
                postData.put("mimeType", "application/json");
                postData.put("text", body.isValueNode() ? body.asText() : body.toString());
            }

            return harRequest;

        } catch (Exception e) {
            throw new ConversionException("Failed to convert StandardHttpRequest to HAR request JsonNode", e);
        }
    }

    @Override
    public boolean supportsRequestConversion() {
        return true;
    }

    // ======================================================
    // Metadata
    // ======================================================

    @Override
    public String getName() {
        return "HAR_CONVERTER";
    }

    @Override
    public boolean supports(Class<?> responseType) {
        // This converter is intended for HAR JsonNode structures
        return JsonNode.class.isAssignableFrom(responseType);
    }

    // ======================================================
    // Helpers
    // ======================================================

    /**
     * Helper to safely treat a JsonNode as an ObjectNode.
     */
    private static ObjectNode asObjectNodeOrEmpty(JsonNode node) {
        if (node instanceof ObjectNode) {
            return (ObjectNode) node;
        }
        return MAPPER.createObjectNode();
    }
}
