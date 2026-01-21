package es.us.isa.httpmutator.core.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * A {@link MutantWriter} implementation that writes mutated responses as JSONL
 * (one JSON object per line), using an internal buffer to batch writes and
 * reduce IO overhead.
 *
 * <p>This writer is agnostic to the request: it never inspects or serializes
 * {@link HttpExchange#getRequest()}, and only uses:</p>
 *
 * <ul>
 *     <li>The mutated {@link StandardHttpResponse}</li>
 *     <li>The exchange identifier (for metadata, if enabled)</li>
 *     <li>The mutation metadata from {@link Mutant}</li>
 * </ul>
 */
public class JsonlMutantWriter implements MutantWriter{

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Writer out;
    private final boolean includeMeta;

    /** Internal buffer used to batch JSONL writes. */
    private final StringBuilder buffer;

    /** Threshold (in characters) at which the buffer is flushed to the underlying writer. */
    private final int flushThreshold;

    private boolean closed = false;

    /**
     * Creates a JSONL writer with a default 1 MB buffer.
     *
     * @param out         the underlying writer to which JSONL lines will be written
     * @param includeMeta whether to attach mutation metadata fields to each JSON object
     */
    public JsonlMutantWriter(Writer out, boolean includeMeta) {
        this(out, includeMeta, 1_048_576); // 1 MB default
    }

    /**
     * Creates a JSONL writer with a configurable buffer size.
     *
     * @param out            the underlying writer to which JSONL lines will be written
     * @param includeMeta    whether to attach mutation metadata fields to each JSON object
     * @param flushThreshold buffer size (in characters) at which data is flushed
     */
    public JsonlMutantWriter(Writer out, boolean includeMeta, int flushThreshold) {
        this.out = Objects.requireNonNull(out, "out must not be null");
        this.includeMeta = includeMeta;
        this.flushThreshold = flushThreshold;
        this.buffer = new StringBuilder(flushThreshold);
    }

    /**
     * Writes a single mutated response as one JSONL line.
     *
     * @param exchange        original HTTP exchange (only {@code id} is used)
     * @param mutatedResponse mutated standard HTTP response
     * @param mutant          mutation metadata
     */
    @Override
    public void write(HttpExchange exchange,
                      StandardHttpResponse mutatedResponse,
                      Mutant mutant) throws IOException {

        if (closed) {
            throw new IOException("JsonlMutantWriter is already closed");
        }

        // 1) Convert StandardHttpResponse to canonical JsonNode
        JsonNode canonical = mutatedResponse.toJsonNode();
        if (canonical == null || canonical.isNull()) {
            // Defensive: should not happen in normal pipelines
            return;
        }

        // 2) Ensure we always serialize an ObjectNode
        final ObjectNode lineObject;
        if (canonical instanceof ObjectNode) {
            lineObject = (ObjectNode) canonical;
        } else {
            lineObject = objectMapper.createObjectNode();
            lineObject.set("Body", canonical);
        }

        // 3) Optionally attach metadata
        if (includeMeta) {
            String originalId = exchange.getId();
            if (originalId != null) {
                lineObject.put("_hm_original_id", originalId);
            }
            lineObject.put("_hm_original_json_path", mutant.getOriginalJsonPath());
            lineObject.put("_hm_mutator", mutant.getMutatorClassName());
            lineObject.put("_hm_operator", mutant.getOperatorClassName());
        }

        // 4) Append one JSON line into buffer
        buffer.append(objectMapper.writeValueAsString(lineObject))
                .append('\n');

        // 5) Flush when exceeding the threshold
        if (buffer.length() >= flushThreshold) {
            flushBuffer();
        }
    }

    private void flushBuffer() throws IOException {
        if (buffer.length() == 0) {
            return;
        }
        out.write(buffer.toString());
        buffer.setLength(0);
        out.flush();
    }

    @Override
    public void flush() throws IOException {
        if (closed) {
            return;
        }
        flushBuffer();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                flushBuffer();
            } finally {
                closed = true;
                out.close();
            }
        }
    }
}