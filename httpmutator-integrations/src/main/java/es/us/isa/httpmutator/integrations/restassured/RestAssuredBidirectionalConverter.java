// ========================================
// REST Assured Response Converter
// ========================================
package es.us.isa.httpmutator.integrations.restassured;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.converter.BidirectionalConverter;
import es.us.isa.httpmutator.core.converter.ConversionException;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bidirectional converter between RestAssured {@link Response}
 * and HttpMutator's {@link StandardHttpResponse}.
 */
public final class RestAssuredBidirectionalConverter implements BidirectionalConverter<Response> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Singleton instance for convenience. */
    public static final RestAssuredBidirectionalConverter INSTANCE =
            new RestAssuredBidirectionalConverter();

    private RestAssuredBidirectionalConverter() {}

    @Override
    public String getName() {
        return "RestAssuredResponse";
    }

    @Override
    public boolean supports(Class<?> responseType) {
        return Response.class.isAssignableFrom(responseType);
    }

    @Override
    public StandardHttpResponse toStandardResponse(Response response) throws ConversionException {
        try {
            Map<String, Object> headers = extractHeaders(response);

            JsonNode bodyNode = parseResponseBodySafely(response);

            return StandardHttpResponse.of(response.getStatusCode(), headers, bodyNode);
        } catch (Exception e) {
            throw new ConversionException("Failed to convert REST Assured Response", e);
        }
    }

    @Override
    public Response fromStandardResponse(StandardHttpResponse std) throws ConversionException {
        try {
            /* ---------- 1. Headers ---------- */
            List<Header> headerList = new ArrayList<>();
            String contentType = null;

            Map<String, Object> stdHeaders = std.getHeaders();
            if (stdHeaders != null) {
                for (Map.Entry<String, Object> entry : stdHeaders.entrySet()) {
                    String headerName = entry.getKey();
                    String headerValue = String.valueOf(entry.getValue());

                    if ("content-type".equalsIgnoreCase(headerName)) {
                        contentType = headerValue;
                    }

                    headerList.add(new Header(headerName, headerValue));
                }
            }
            Headers headers = new Headers(headerList);

            /* ---------- 2. Body ---------- */
            // JsonNode ➜ String（Rest-Assured handle body in string）
            String bodyAsString = "";
            if (std.getBody() != null) {
                bodyAsString = std.getBody().toString();
            }

            /* ---------- 3. Build Response ---------- */
            ResponseBuilder builder = new ResponseBuilder();
            builder.setStatusCode(std.getStatusCode());
            builder.setStatusLine("HTTP/1.1 " + std.getStatusCode());
            builder.setHeaders(headers);
            builder.setBody(bodyAsString);

            if (contentType != null) {
                builder.setContentType(contentType);
            }

            return builder.build();

        } catch (Exception e) {
            throw new ConversionException("Failed to convert StandardHttpResponse to Response", e);
        }
    }

    private static Map<String, Object> extractHeaders(Response response) {
        Map<String, Object> headers = new LinkedHashMap<>();
        if (response == null || response.getHeaders() == null) {
            return headers;
        }
        for (Header header : response.getHeaders()) {
            headers.putIfAbsent(header.getName(), header.getValue());
        }
        return headers;
    }

    private static JsonNode parseResponseBodySafely(Response response) {
        String raw;
        try {
            raw = (response == null) ? "" : response.asString();
        } catch (Exception e) {
            raw = "";
        }

        if (raw == null || raw.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(raw);
        } catch (Exception e) {
            return OBJECT_MAPPER.valueToTree(raw);
        }
    }
}



