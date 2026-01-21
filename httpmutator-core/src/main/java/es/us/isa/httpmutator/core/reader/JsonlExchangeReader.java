package es.us.isa.httpmutator.core.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Streaming reader for JSONL input (one JSON response per line).
 *
 * <p>Each non-empty line in the input must be a JSON object representing a
 * canonical HTTP response:
 *
 * {
 *   "Status Code": ...,
 *   "Headers": {...},
 *   "Body": ...
 * }
 *
 * <p>Optional: if the JSON object has a top-level field {@code "id"}, that value
 * is used as the {@link HttpExchange#getId()}.
 * Otherwise, the line number (1-based) is used as the id.</p>
 *
 * <p>For each line:
 * request  = null
 * response = parsed {@link StandardHttpResponse}
 * id       = JSON["id"] or line number
 *
 * <p>Fully streaming, no in-memory accumulation.</p>
 */
public class JsonlExchangeReader implements HttpExchangeReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void read(Reader in, Consumer<HttpExchange> consumer) throws IOException {
        Objects.requireNonNull(in, "reader must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        BufferedReader br = (in instanceof BufferedReader)
                ? (BufferedReader) in
                : new BufferedReader(in);

        String line;
        int lineNumber = 0;

        while ((line = br.readLine()) != null) {
            lineNumber++;

            if (line.isEmpty()) {
                continue;
            }

            JsonNode node;
            try {
                node = MAPPER.readTree(line);
            } catch (Exception e) {
                throw new IOException("Invalid JSON at line " + lineNumber, e);
            }

            // Determine id: prefer explicit "id" field, otherwise line number
            String id;
            JsonNode idNode = node.get("id");
            if (idNode != null && !idNode.isNull()) {
                id = idNode.asText();
            } else {
                id = String.valueOf(lineNumber);
            }

            StandardHttpResponse response;
            try {
                response = StandardHttpResponse.fromJsonNode(node);
            } catch (Exception e) {
                throw new IOException("Invalid canonical StandardHttpResponse at line " + lineNumber, e);
            }

            HttpExchange exchange = new HttpExchange(null, response, id);

            consumer.accept(exchange);
        }
    }
}