package es.us.isa.httpmutator.core.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.converter.impl.WebScarabBidirectionalConverter;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class WebScarabBidirectionalConverterTest {

    private WebScarabBidirectionalConverter converter;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        converter = new WebScarabBidirectionalConverter();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGetName() {
        assertEquals("WebScarabPlainText", converter.getName());
    }

    @Test
    public void testSupports() {
        assertTrue(converter.supports(String.class));
        assertFalse(converter.supports(Integer.class));
        assertFalse(converter.supports(Object.class));
    }

    @Test
    public void testToStandardResponse_SimpleJsonResponse() throws Exception {
        String webScarabResponse = "HTTP/1.1 200 OK\r\n" +
                                  "Content-Type: application/json\r\n" +
                                  "Content-Length: 25\r\n" +
                                  "\r\n" +
                                  "{\"message\":\"Hello World\"}";

        StandardHttpResponse result = converter.toStandardResponse(webScarabResponse);

        assertEquals(200, result.getStatusCode());
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
        assertEquals("25", result.getHeaders().get("Content-Length"));
        
        JsonNode bodyNode = result.getBody();
        assertTrue(bodyNode.isObject());
        assertEquals("Hello World", bodyNode.get("message").asText());
    }

    @Test
    public void testToStandardResponse_PlainTextResponse() throws Exception {
        String webScarabResponse = "HTTP/1.1 200 OK\r\n" +
                                  "Content-Type: text/plain\r\n" +
                                  "\r\n" +
                                  "Simple text response";

        StandardHttpResponse result = converter.toStandardResponse(webScarabResponse);

        assertEquals(200, result.getStatusCode());
        assertEquals("text/plain", result.getHeaders().get("Content-Type"));
        assertEquals("Simple text response", result.getBody().asText());
    }

    @Test
    public void testToStandardResponse_EmptyBody() throws Exception {
        String webScarabResponse = "HTTP/1.1 204 No Content\r\n" +
                                  "Server: nginx/1.18.0\r\n" +
                                  "\r\n";

        StandardHttpResponse result = converter.toStandardResponse(webScarabResponse);

        assertEquals(204, result.getStatusCode());
        assertEquals("nginx/1.18.0", result.getHeaders().get("Server"));
        assertTrue(result.getBody().isObject());
        assertEquals(0, result.getBody().size());
    }

    @Test
    public void testToStandardResponse_MultilineBody() throws Exception {
        String webScarabResponse = "HTTP/1.1 200 OK\r\n" +
                                  "Content-Type: text/plain\r\n" +
                                  "\r\n" +
                                  "Line 1\n" +
                                  "Line 2\n" +
                                  "Line 3";

        StandardHttpResponse result = converter.toStandardResponse(webScarabResponse);

        assertEquals(200, result.getStatusCode());
        String expectedBody = "Line 1\nLine 2\nLine 3";
        assertEquals(expectedBody, result.getBody().asText());
    }

    @Test
    public void testFromStandardResponse_JsonResponse() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Content-Length", "25");

        JsonNode jsonBody = objectMapper.readTree("{\"message\":\"Hello World\"}");
        StandardHttpResponse standardResponse = StandardHttpResponse.of(200, headers, jsonBody);

        String result = converter.fromStandardResponse(standardResponse);

        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.contains("Content-Type: application/json\r\n"));
        assertTrue(result.contains("Content-Length: 25\r\n"));
        assertTrue(result.contains("\r\n\r\n"));
        assertTrue(result.endsWith("{\"message\":\"Hello World\"}"));
    }

    @Test
    public void testFromStandardResponse_PlainTextResponse() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");

        JsonNode textBody = objectMapper.valueToTree("Simple text response");
        StandardHttpResponse standardResponse = StandardHttpResponse.of(204, headers, textBody);

        String result = converter.fromStandardResponse(standardResponse);

        assertTrue(result.startsWith("HTTP/1.1 204 No Content\r\n"));
        assertTrue(result.contains("Content-Type: text/plain\r\n"));
        assertTrue(result.endsWith("\"Simple text response\""));
    }

    @Test
    public void testFromStandardResponse_EmptyBody() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Server", "nginx/1.18.0");

        StandardHttpResponse standardResponse = StandardHttpResponse.of(204, headers, null);

        String result = converter.fromStandardResponse(standardResponse);

        assertTrue(result.startsWith("HTTP/1.1 204 No Content\r\n"));
        assertTrue(result.contains("Server: nginx/1.18.0\r\n"));
        assertTrue(result.endsWith("\r\n\r\n"));
    }

    @Test
    public void testBidirectionalConversion_JsonResponse() throws Exception {
        String originalWebScarab = "HTTP/1.1 201 Created\r\n" +
                                  "Content-Type: application/json\r\n" +
                                  "Location: /api/users/123\r\n" +
                                  "\r\n" +
                                  "{\"id\":123,\"name\":\"John Doe\"}";

        // Convert to standard and back
        StandardHttpResponse standard = converter.toStandardResponse(originalWebScarab);
        String backToWebScarab = converter.fromStandardResponse(standard);

        // Verify round-trip conversion preserves key information
        assertTrue(backToWebScarab.contains("HTTP/1.1 201 Created"));
        assertTrue(backToWebScarab.contains("Content-Type: application/json"));
        assertTrue(backToWebScarab.contains("Location: /api/users/123"));
        assertTrue(backToWebScarab.contains("{\"id\":123,\"name\":\"John Doe\"}"));
    }

    @Test
    public void testBidirectionalConversion_PlainTextResponse() throws Exception {
        String originalWebScarab = "HTTP/1.1 400 Bad Request\r\n" +
                                  "Content-Type: text/plain\r\n" +
                                  "\r\n" +
                                  "Invalid request format";

        // Convert to standard and back
        StandardHttpResponse standard = converter.toStandardResponse(originalWebScarab);
        String backToWebScarab = converter.fromStandardResponse(standard);

        // Verify round-trip conversion
        assertTrue(backToWebScarab.contains("HTTP/1.1 400 Bad Request"));
        assertTrue(backToWebScarab.contains("Content-Type: text/plain"));
        assertTrue(backToWebScarab.contains("Invalid request format"));
    }

    @Test
    public void testToStandardResponse_InvalidFormat_NullInput() {
        try {
            converter.toStandardResponse(null);
            fail("Expected ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }

    @Test
    public void testToStandardResponse_InvalidFormat_EmptyInput() {
        try {
            converter.toStandardResponse("");
            fail("Expected ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }

    @Test
    public void testToStandardResponse_InvalidFormat_NoStatusLine() {
        try {
            converter.toStandardResponse("Content-Type: text/plain\r\n\r\nBody content");
            fail("Expected ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getMessage().contains("Invalid status line format"));
        }
    }

    @Test
    public void testToStandardResponse_InvalidFormat_BadStatusLine() {
        try {
            converter.toStandardResponse("INVALID STATUS LINE\r\n\r\nBody");
            fail("Expected ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getMessage().contains("Invalid status line format"));
        }
    }

    @Test
    public void testToStandardResponse_InvalidFormat_BadHeader() {
        try {
            converter.toStandardResponse("HTTP/1.1 200 OK\r\nInvalidHeaderWithoutColon\r\n\r\nBody");
            fail("Expected ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getMessage().contains("Invalid header format"));
        }
    }

    @Test
    public void testFromStandardResponse_NullInput() {
        try {
            converter.fromStandardResponse(null);
            fail("Expected ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void testStatusCodeReasonPhrases() throws Exception {
        // Test various status codes and their reason phrases
        int[] statusCodes = {200, 201, 204, 400, 401, 404, 500, 999};
        String[] expectedPhrases = {"OK", "Created", "No Content", "Bad Request", 
                                   "Unauthorized", "Not Found", "Internal Server Error", "Unknown"};

        for (int i = 0; i < statusCodes.length; i++) {
            StandardHttpResponse response = StandardHttpResponse.of(statusCodes[i], new HashMap<>(), null);
            String webScarabResponse = converter.fromStandardResponse(response);
            assertTrue(webScarabResponse.contains("HTTP/1.1 " + statusCodes[i] + " " + expectedPhrases[i]));
        }
    }

    @Test
    public void testCaseInsensitiveContentTypeHeader() throws Exception {
        String webScarabResponse = "HTTP/1.1 200 OK\r\n" +
                                  "content-type: application/json\r\n" +
                                  "\r\n" +
                                  "{\"test\":\"value\"}";

        StandardHttpResponse result = converter.toStandardResponse(webScarabResponse);
        JsonNode bodyNode = result.getBody();
        assertTrue(bodyNode.isObject());
        assertEquals("value", bodyNode.get("test").asText());
    }

    @Test
    public void testInvalidJsonFallsBackToText() throws Exception {
        String webScarabResponse = "HTTP/1.1 200 OK\r\n" +
                                  "Content-Type: application/json\r\n" +
                                  "\r\n" +
                                  "Invalid JSON content {";

        StandardHttpResponse result = converter.toStandardResponse(webScarabResponse);
        assertEquals("Invalid JSON content {", result.getBody().asText());
    }
}