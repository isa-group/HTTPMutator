package es.us.isa.httpmutator.core.writer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.luben.zstd.ZstdOutputStream;
import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Sharded, Zstandard-compressed JSONL MutantWriter.
 *
 * Key properties (final version):
 *  - Writes JSONL as UTF-8 bytes via JsonGenerator (no writeValueAsString per row).
 *  - Avoids deep-copy of huge JSON trees: if canonical is ObjectNode, we stream its fields.
 *  - Appends only "_hm_original_id" to each line.
 *  - Writes to "*.tmp" first and then moves to final name on shard commit.
 *  - Uses a large BufferedOutputStream to improve throughput on large records.
 *  - Maintains an approximate "uncompressed bytes" counter (bytes emitted to generator),
 *    allowing rotation by bytes as well as by line count.
 */
public final class ShardedZstdJsonlMutantWriter implements MutantWriter {

    // -----------------------------
    // Defaults tuned for throughput
    // -----------------------------
    public static final long DEFAULT_MAX_LINES_PER_SHARD = 50_000;     // large records => smaller shards
    public static final long DEFAULT_MAX_UNCOMPRESSED_BYTES = 1L << 30; // 1 GiB (approx)
    public static final int  DEFAULT_ZSTD_LEVEL = 3;                  // throughput-friendly
    public static final int  DEFAULT_BUFFER_BYTES = 1 << 20;          // 1 MiB buffer

    private final ObjectMapper mapper;
    private final JsonFactory factory;

    private final Path outputDir;
    private final String shardPrefix;

    private final long maxLinesPerShard;
    private final long maxUncompressedBytesApprox;
    private final int zstdLevel;
    private final int bufferBytes;

    private int shardIndex = 0;
    private long currentLines = 0;

    // counts bytes emitted to JsonGenerator (pre-compression bytes), approximates "uncompressed size"
    private long currentUncompressedBytes = 0;

    private Path currentTmpPath;
    private Path currentFinalPath;

    private OutputStream fileOut;
    private ZstdOutputStream zstdOut;
    private CountingOutputStream countOut;
    private JsonGenerator gen;

    private boolean closed = false;

    // -----------------------------
    // Constructors
    // -----------------------------
    public ShardedZstdJsonlMutantWriter(Path outputDir, String shardPrefix) throws IOException {
        this(
                outputDir,
                shardPrefix,
                DEFAULT_MAX_LINES_PER_SHARD,
                DEFAULT_MAX_UNCOMPRESSED_BYTES,
                DEFAULT_ZSTD_LEVEL,
                DEFAULT_BUFFER_BYTES
        );
    }

    public ShardedZstdJsonlMutantWriter(
            Path outputDir,
            String shardPrefix,
            long maxLinesPerShard,
            long maxUncompressedBytesApprox,
            int zstdLevel
    ) throws IOException {
        this(
                outputDir,
                shardPrefix,
                maxLinesPerShard,
                maxUncompressedBytesApprox,
                zstdLevel,
                DEFAULT_BUFFER_BYTES
        );
    }

    public ShardedZstdJsonlMutantWriter(
            Path outputDir,
            String shardPrefix,
            long maxLinesPerShard,
            long maxUncompressedBytesApprox,
            int zstdLevel,
            int bufferBytes
    ) throws IOException {
        this.outputDir = Objects.requireNonNull(outputDir, "outputDir must not be null");
        this.shardPrefix = Objects.requireNonNull(shardPrefix, "shardPrefix must not be null");
        if (maxLinesPerShard <= 0) throw new IllegalArgumentException("maxLinesPerShard must be > 0");
        if (maxUncompressedBytesApprox <= 0) throw new IllegalArgumentException("maxUncompressedBytesApprox must be > 0");
        if (bufferBytes <= 0) throw new IllegalArgumentException("bufferBytes must be > 0");

        this.maxLinesPerShard = maxLinesPerShard;
        this.maxUncompressedBytesApprox = maxUncompressedBytesApprox;
        this.zstdLevel = zstdLevel;
        this.bufferBytes = bufferBytes;

        this.mapper = new ObjectMapper();
        this.factory = mapper.getFactory();

        Files.createDirectories(this.outputDir);
        openNextShard();
    }

    // -----------------------------
    // MutantWriter implementation
    // -----------------------------
    @Override
    public void write(HttpExchange exchange,
                      StandardHttpResponse mutatedResponse,
                      Mutant mutant) throws IOException {

        if (closed) {
            throw new IOException("ShardedZstdJsonlMutantWriter is already closed");
        }

        final JsonNode canonical = mutatedResponse.toJsonNode();
        if (canonical == null || canonical.isNull()) {
            return;
        }

        // JSONL: exactly one JSON object per line, followed by '\n'
        writeOneJsonlObject(exchange, canonical);

        currentLines++;
        currentUncompressedBytes = countOut.getCount(); // bytes emitted so far in this shard

        // Rotate AFTER writing (so a single huge record is allowed; it just triggers a rotate right after)
        if (shouldRotateShard()) {
            rotateShard();
        }
    }

    @Override
    public void flush() throws IOException {
        if (closed) return;
        if (gen != null) gen.flush();
        if (zstdOut != null) zstdOut.flush();
        if (fileOut != null) fileOut.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        closeCurrentShardAndCommit();
    }

    // -----------------------------
    // Core writing logic (no deep-copy)
    // -----------------------------
    private void writeOneJsonlObject(HttpExchange exchange, JsonNode canonical) throws IOException {
        gen.writeStartObject();

        if (canonical instanceof ObjectNode) {
            // Stream existing top-level fields without copying the tree
            Iterator<Map.Entry<String, JsonNode>> it = canonical.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String fieldName = e.getKey();

                // Safety: if input already contains _hm_original_id, we will overwrite with our value later.
                // We still stream it now; last write wins.
                gen.writeFieldName(fieldName);
                gen.writeTree(e.getValue());
            }
        } else {
            // Keep prior semantics: wrap non-object canonical under "Body"
            gen.writeFieldName("Body");
            gen.writeTree(canonical);
        }

        String originalId = exchange.getId();
        if (originalId != null) {
            gen.writeStringField("_hm_original_id", originalId);
        }

        gen.writeEndObject();
        gen.writeRaw('\n');
    }

    // -----------------------------
    // Sharding & lifecycle
    // -----------------------------
    private boolean shouldRotateShard() {
        return currentLines >= maxLinesPerShard
                || currentUncompressedBytes >= maxUncompressedBytesApprox;
    }

    private void rotateShard() throws IOException {
        closeCurrentShardAndCommit();
        openNextShard();
    }

    private void openNextShard() throws IOException {
        String baseName = String.format("%s-%05d.jsonl.zst", shardPrefix, shardIndex++);
        currentFinalPath = outputDir.resolve(baseName);
        currentTmpPath = outputDir.resolve(baseName + ".tmp");

        Files.deleteIfExists(currentTmpPath);

        fileOut = Files.newOutputStream(currentTmpPath);
        OutputStream buffered = new BufferedOutputStream(fileOut, bufferBytes);

        zstdOut = new ZstdOutputStream(buffered, zstdLevel);

        // Count bytes BEFORE compression: place counter ABOVE zstdOut (so it sees uncompressed JSONL bytes).
        // Note: we count "bytes emitted by JsonGenerator", which is the uncompressed UTF-8 JSONL stream.
        countOut = new CountingOutputStream(zstdOut);

        gen = factory.createGenerator(countOut);
        // We want explicit control over closing underlying streams
        gen.configure(Feature.AUTO_CLOSE_TARGET, false);

        currentLines = 0;
        currentUncompressedBytes = 0;
    }

    private void closeCurrentShardAndCommit() throws IOException {
        if (gen == null) {
            return;
        }

        IOException closeError = null;

        // 1) Flush generator buffers
        try {
            gen.flush();
        } catch (IOException e) {
            closeError = e;
        }

        // 2) Close generator (won't close streams due to AUTO_CLOSE_TARGET=false)
        try {
            gen.close();
        } catch (IOException e) {
            if (closeError == null) closeError = e;
        } finally {
            gen = null;
        }

        // 3) Close streams (in order)
        closeQuietly(countOut, closeError);
        countOut = null;

        closeQuietly(zstdOut, closeError);
        zstdOut = null;

        closeQuietly(fileOut, closeError);
        fileOut = null;

        if (closeError != null) {
            throw closeError;
        }

        // 4) Commit tmp -> final
        try {
            Files.move(currentTmpPath, currentFinalPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(currentTmpPath, currentFinalPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void closeQuietly(OutputStream os, IOException prior) throws IOException {
        if (os == null) return;
        try {
            os.close();
        } catch (IOException e) {
            // prefer the first error, but if there wasn't one, propagate this
            if (prior == null) throw e;
        }
    }

    // -----------------------------
    // CountingOutputStream
    // -----------------------------
    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream delegate;
        private long count = 0;

        CountingOutputStream(OutputStream delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        long getCount() {
            return count;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            count++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.write(b);
            count += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            count += len;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
