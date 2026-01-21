package es.us.isa.httpmutator.core.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.converter.ConversionException;
import es.us.isa.httpmutator.core.converter.impl.HarConverter;
import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.StandardHttpRequest;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

/**
 * A {@link HttpExchangeReader} implementation that reads HTTP interactions from
 * a HAR (HTTP Archive) file.
 *
 * <p>HAR files store a sequence of request–response pairs under
 * {@code log.entries[*]}. This reader extracts each entry, converts the HAR
 * request/response into canonical {@link StandardHttpRequest} and
 * {@link StandardHttpResponse} objects via {@link HarConverter}, and streams
 * them as {@link HttpExchange} instances.</p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *     <li>Read and parse the HAR JSON structure.</li>
 *     <li>For each {@code log.entries[*]} element:
 *         <ul>
 *             <li>Extract {@code entry.request} and convert it to
 *                 {@link StandardHttpRequest} (may be {@code null} depending on content).</li>
 *             <li>Extract {@code entry.response} and convert it to
 *                 {@link StandardHttpResponse} (required for mutation).</li>
 *             <li>Create an {@link HttpExchange} where:
 *                 <ul>
 *                     <li>{@code request} = canonical {@code StandardHttpRequest}</li>
 *                     <li>{@code response} = canonical {@code StandardHttpResponse}</li>
 *                 </ul>
 *             </li>
 *             <li>Assign a stable ID (HAR entry {@code id} if present, otherwise index).</li>
 *         </ul>
 *     </li>
 *     <li>Stream each {@link HttpExchange} to the provided consumer without
 *         holding all entries in memory.</li>
 * </ul>
 *
 * <h2>Normalization</h2>
 * <p>The raw HAR structures are not directly compatible with the mutation
 * engine. This reader relies on {@link HarConverter} to transform HAR
 * {@code request} / {@code response} objects into the canonical models
 * {@link StandardHttpRequest} and {@link StandardHttpResponse} expected by
 * {@code HttpMutatorEngine} and downstream writers.</p>
 *
 * <h2>Failure Behavior</h2>
 * <ul>
 *     <li>Malformed HAR input results in {@link IOException}.</li>
 *     <li>Missing or invalid response fields result in an error, as a
 *         response is required for mutation.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 * Reader in = Files.newBufferedReader(Paths.get("traffic.har"));
 * HttpExchangeReader reader = new HarExchangeReader();
 * reader.read(in, exchange -> {
 *     // pass exchange to HttpMutator pipeline
 * });
 * </pre>
 */
public class HarExchangeReader implements HttpExchangeReader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HarConverter converter = new HarConverter();

    @Override
    public void read(Reader in, Consumer<HttpExchange> consumer) throws IOException {

        JsonNode root = objectMapper.readTree(
                in instanceof BufferedReader ? in : new BufferedReader(in)
        );

        JsonNode entries = root.path("log").path("entries");

        if (!entries.isArray()) {
            throw new IOException("Invalid HAR: expected log.entries array");
        }

        int index = 0;
        for (JsonNode entry : entries) {
            index++;

            JsonNode rawRequestNode  = entry.path("request");
            JsonNode rawResponseNode = entry.path("response");

            // Convert HAR request -> StandardHttpRequest (may be null)
            StandardHttpRequest canonicalRequest = null;
            try {
                if (rawRequestNode != null && !rawRequestNode.isMissingNode() && !rawRequestNode.isNull()) {
                    canonicalRequest = converter.toStandardRequest(rawRequestNode);
                }
            } catch (ConversionException e) {
                throw new IOException("Failed to convert HAR request at index " + index, e);
            }

            // Convert HAR response -> StandardHttpResponse (required)
            StandardHttpResponse canonicalResponse;
            try {
                canonicalResponse = converter.toStandardResponse(rawResponseNode);
            } catch (ConversionException e) {
                throw new IOException("Failed to convert HAR response at index " + index, e);
            }
            if (canonicalResponse == null) {
                throw new IOException("HAR entry missing valid response at index " + index);
            }

            // Resolve entry ID (use explicit HAR id if present)
            String id = entry.has("id")
                    ? entry.get("id").asText()
                    : String.valueOf(index);

            HttpExchange exchange = new HttpExchange(canonicalRequest, canonicalResponse, id);

            consumer.accept(exchange);
        }
    }
}