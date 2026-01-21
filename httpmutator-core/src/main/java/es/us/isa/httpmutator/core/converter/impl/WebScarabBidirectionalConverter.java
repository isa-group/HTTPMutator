// ========================================
// WebScarab Plain-Text Format Converter
// ========================================
package es.us.isa.httpmutator.core.converter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.converter.BidirectionalConverter;
import es.us.isa.httpmutator.core.converter.ConversionException;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bidirectional converter for WebScarab plain-text HTTP response format.
 * 
 * WebScarab stores HTTP responses in standard HTTP wire format:
 * - Status line: HTTP/1.1 200 OK
 * - Headers: Header-Name: Header-Value
 * - Empty line
 * - Response body
 */
public class WebScarabBidirectionalConverter implements BidirectionalConverter<String> {

    private static final Pattern STATUS_LINE_PATTERN = Pattern.compile("^HTTP/\\d\\.\\d\\s+(\\d+)\\s*(.*)$");
    private static final String CRLF = "\r\n";
    private static final String LF = "\n";
    
    private final ObjectMapper objectMapper;

    public WebScarabBidirectionalConverter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "WebScarabPlainText";
    }

    @Override
    public boolean supports(Class<?> responseType) {
        return String.class.isAssignableFrom(responseType);
    }

    @Override
    public StandardHttpResponse toStandardResponse(String webScarabResponse) throws ConversionException {
        if (webScarabResponse == null || webScarabResponse.trim().isEmpty()) {
            throw new ConversionException("WebScarab response string cannot be null or empty");
        }

        try {
            BufferedReader reader = new BufferedReader(new StringReader(webScarabResponse));
            
            // Parse status line
            String statusLine = reader.readLine();
            if (statusLine == null) {
                throw new ConversionException("Invalid WebScarab format: missing status line");
            }
            
            int statusCode = parseStatusCode(statusLine);
            
            // Parse headers
            Map<String, Object> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.trim().isEmpty()) {
                parseHeaderLine(headerLine, headers);
            }
            
            // Parse body (everything after empty line)
            StringBuilder bodyBuilder = new StringBuilder();
            String bodyLine;
            boolean firstLine = true;
            while ((bodyLine = reader.readLine()) != null) {
                if (!firstLine) {
                    bodyBuilder.append(LF);
                }
                bodyBuilder.append(bodyLine);
                firstLine = false;
            }
            
            String bodyContent = bodyBuilder.toString();
            JsonNode bodyNode = parseResponseBody(bodyContent, headers);
            
            return StandardHttpResponse.of(statusCode, headers, bodyNode);
            
        } catch (IOException e) {
            throw new ConversionException("Failed to parse WebScarab response format", e);
        }
    }

    @Override
    public String fromStandardResponse(StandardHttpResponse standardResponse) throws ConversionException {
        if (standardResponse == null) {
            throw new ConversionException("StandardHttpResponse cannot be null");
        }

        StringBuilder webScarabResponse = new StringBuilder();
        
        // Build status line
        webScarabResponse.append("HTTP/1.1 ")
                        .append(standardResponse.getStatusCode())
                        .append(" ")
                        .append(getReasonPhrase(standardResponse.getStatusCode()))
                        .append(CRLF);
        
        // Build headers
        if (standardResponse.getHeaders() != null) {
            for (Map.Entry<String, Object> header : standardResponse.getHeaders().entrySet()) {
                webScarabResponse.append(header.getKey())
                               .append(": ")
                               .append(header.getValue())
                               .append(CRLF);
            }
        }
        
        // Empty line separating headers from body
        webScarabResponse.append(CRLF);
        
        // Build body
        if (standardResponse.getBody() != null) {
            String bodyContent = standardResponse.getBody().toString();
            webScarabResponse.append(bodyContent);
        }
        
        return webScarabResponse.toString();
    }

    private int parseStatusCode(String statusLine) throws ConversionException {
        Matcher matcher = STATUS_LINE_PATTERN.matcher(statusLine.trim());
        if (!matcher.matches()) {
            throw new ConversionException("Invalid status line format: " + statusLine);
        }
        
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new ConversionException("Invalid status code in line: " + statusLine, e);
        }
    }

    private void parseHeaderLine(String headerLine, Map<String, Object> headers) throws ConversionException {
        int colonIndex = headerLine.indexOf(':');
        if (colonIndex == -1) {
            throw new ConversionException("Invalid header format: " + headerLine);
        }
        
        String headerName = headerLine.substring(0, colonIndex).trim();
        String headerValue = headerLine.substring(colonIndex + 1).trim();
        
        if (headerName.isEmpty()) {
            throw new ConversionException("Empty header name in line: " + headerLine);
        }
        
        headers.put(headerName, headerValue);
    }

    private JsonNode parseResponseBody(String bodyContent, Map<String, Object> headers) {
        if (bodyContent == null || bodyContent.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        
        // Check Content-Type to determine parsing strategy
        String contentType = getContentType(headers);
        
        // Try to parse as JSON if content type suggests it or if parsing succeeds
        if (contentType != null && 
            (contentType.contains("application/json") || contentType.contains("text/json"))) {
            try {
                return objectMapper.readTree(bodyContent);
            } catch (Exception e) {
                // If JSON parsing fails, treat as plain text
                return objectMapper.valueToTree(bodyContent);
            }
        }
        
        // For other content types, try JSON parsing first, then fallback to text
        try {
            return objectMapper.readTree(bodyContent);
        } catch (Exception e) {
            // If not valid JSON, wrap as text node
            return objectMapper.valueToTree(bodyContent);
        }
    }

    private String getContentType(Map<String, Object> headers) {
        if (headers == null) {
            return null;
        }
        
        // Case-insensitive search for Content-Type header
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if ("content-type".equalsIgnoreCase(entry.getKey())) {
                return String.valueOf(entry.getValue()).toLowerCase();
            }
        }
        
        return null;
    }

    private String getReasonPhrase(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 422: return "Unprocessable Entity";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "Unknown";
        }
    }
}