package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.JsonNode;
import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.reader.HttpExchangeReader;
import es.us.isa.httpmutator.core.reporter.MutantReporter;
import es.us.isa.httpmutator.core.strategy.MutationStrategy;
import es.us.isa.httpmutator.core.util.RandomUtils;
import es.us.isa.httpmutator.core.writer.MutantWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * High-level orchestration class for running HttpMutator.
 */
public class HttpMutator implements AutoCloseable {

    private final HttpMutatorEngine engine;

    /**
     * Strategy for selecting which mutants to keep.
     */
    private MutationStrategy strategy;

    /**
     * Optional writers that emit mutated responses.
     */
    private final List<MutantWriter> writers = new ArrayList<>();

    /**
     * Optional reporters that collect statistics / metrics.
     */
    private final List<MutantReporter> reporters = new ArrayList<>();

    /**
     * Random seed used by mutation utilities.
     */
    private long randomSeed;

    private boolean closed = false;

    public HttpMutator() {
        this(42L);
    }

    public HttpMutator(long randomSeed) {
        this.engine = new HttpMutatorEngine();
        this.randomSeed = randomSeed;
        RandomUtils.setSeed(randomSeed);
    }

    public HttpMutator withMutationStrategy(MutationStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        return this;
    }

    public HttpMutator withWriters(List<MutantWriter> writers) {
        this.writers.clear();
        if (writers != null) {
            this.writers.addAll(writers);
        }
        return this;
    }

    public HttpMutator addWriter(MutantWriter writer) {
        if (writer != null) {
            this.writers.add(writer);
        }
        return this;
    }

    public HttpMutator withReporters(List<MutantReporter> reporters) {
        this.reporters.clear();
        if (reporters != null) {
            this.reporters.addAll(reporters);
        }
        return this;
    }

    public HttpMutator addReporter(MutantReporter reporter) {
        if (reporter != null) {
            this.reporters.add(reporter);
        }
        return this;
    }

    public HttpMutator withRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
        RandomUtils.setSeed(randomSeed);
        return this;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public List<MutantWriter> getWriters() {
        return Collections.unmodifiableList(writers);
    }

    public List<MutantReporter> getReporters() {
        return Collections.unmodifiableList(reporters);
    }

    // ===================== core: engine + strategy =====================
    private void ensureStrategyConfigured() {
        if (strategy == null) {
            throw new IllegalStateException(
                    "MutationStrategy must be configured via withMutationStrategy(...)");
        }
    }

    /**
     * Core pipeline:
     * - engine.getAllMutants
     * - strategy.selectMutants
     * - build StandardHttpResponse for each mutant
     * - notify reporters
     * - invoke extraHandler (per context)
     */
    private void processExchange(HttpExchange exchange, Consumer<StandardHttpResponse> perMutantConsumer) {

        Objects.requireNonNull(exchange, "exchange must not be null");
        ensureStrategyConfigured();

        StandardHttpResponse original = exchange.getResponse();
        JsonNode responseNode = original.toJsonNode();

        try {
            engine.getAllMutants(responseNode, (MutantGroup group) -> {
                for (Mutant mutant : strategy.selectMutants(group)) {
                    JsonNode mutatedNode = mutant.getMutatedNode();
                    StandardHttpResponse mutated =
                            StandardHttpResponse.fromJsonNode(mutatedNode);

                    for (MutantWriter writer : writers) {
                        try {
                            writer.write(exchange, mutated, mutant);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }

                    for (MutantReporter reporter : reporters) {
                        reporter.onMutant(exchange, mutated, mutant);
                    }

                    if (perMutantConsumer != null) {
                        perMutantConsumer.accept(mutated);
                    }
                }
            });
        } catch (UncheckedIOException e) {
            throw new RuntimeException("I/O error while writing mutated responses", e.getCause());
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;

        IOException firstException = null;

        // 1. Close writers
        for (MutantWriter writer : writers) {
            try {
                writer.close();
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                }
            }
        }

        // 2. Notify reporters
        for (MutantReporter reporter : reporters) {
            try {
                reporter.onFinished();
            } catch (RuntimeException e) {
                if (firstException == null) {
                    firstException = new IOException("Reporter failed: " + e.getMessage(), e);
                }
            }
        }

        // 3. Throw the first exception encountered
        if (firstException != null) {
            throw firstException;
        }
    }


    // ===================== Streaming API (reader + writers + reporters) =====================

    public void mutateStream(HttpExchangeReader exchangeReader, Reader in) throws IOException {
        Objects.requireNonNull(exchangeReader, "exchangeReader must not be null");
        Objects.requireNonNull(in, "in must not be null");

        try {
            exchangeReader.read(in, httpExchange -> processExchange(httpExchange, null));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        } finally {
            close();
        }
    }

        // ===================== In-memory: StandardHttpResponse → List =====================

    public List<StandardHttpResponse> mutate(StandardHttpResponse original) {
        return mutate(original, "in-memory");
    }

    public List<StandardHttpResponse> mutate(StandardHttpResponse original, String label) {
        Objects.requireNonNull(original, "original must not be null");
        ensureStrategyConfigured();

        List<StandardHttpResponse> results = new ArrayList<>();
        String id = (label == null || label.isEmpty()) ? "in-memory" : label;
        HttpExchange exchange = new HttpExchange(null, original, id);

        processExchange(exchange, results::add);

        return results;
    }

    // ===================== In-memory: JsonNode → List<JsonNode> =====================

    public List<JsonNode> mutate(JsonNode canonicalResponseNode, String label) {
        Objects.requireNonNull(canonicalResponseNode, "canonicalResponseNode must not be null");
        ensureStrategyConfigured();

        StandardHttpResponse original =
                StandardHttpResponse.fromJsonNode(canonicalResponseNode);
        String id = resolveIdForJsonNode(canonicalResponseNode, label);
        HttpExchange exchange = new HttpExchange(null, original, id);

        List<JsonNode> results = new ArrayList<>();

        processExchange(exchange, mutated -> results.add(mutated.toJsonNode()));

        return results;
    }

    public List<JsonNode> mutate(JsonNode canonicalResponseNode) {
        return mutate(canonicalResponseNode, "");
    }

    // ===================== In-memory streaming: StandardHttpResponse =====================

    public void mutate(StandardHttpResponse original, String label, Consumer<StandardHttpResponse> consumer) {

        Objects.requireNonNull(original, "original must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        ensureStrategyConfigured();

        String id = label == null || label.isEmpty() ? "in-memory" : label;
        HttpExchange exchange = new HttpExchange(null, original, id);

        processExchange(exchange, consumer);
    }

    public void mutate(StandardHttpResponse original, Consumer<StandardHttpResponse> consumer) {
        mutate(original, "", consumer);
    }

    // ===================== In-memory streaming: JsonNode =====================

    public void mutate(JsonNode canonicalResponseNode, Consumer<JsonNode> consumer) {
        mutate(canonicalResponseNode, null, consumer);
    }

    public void mutate(JsonNode canonicalResponseNode, String label, Consumer<JsonNode> consumer) {
        Objects.requireNonNull(canonicalResponseNode, "canonicalResponseNode must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        ensureStrategyConfigured();

        String id = resolveIdForJsonNode(canonicalResponseNode, label);
        StandardHttpResponse original =
                StandardHttpResponse.fromJsonNode(canonicalResponseNode);
        HttpExchange exchange = new HttpExchange(null, original, id);

        processExchange(exchange, mutated -> consumer.accept(mutated.toJsonNode()));
    }

    /**
     * Resolve an identifier for an in-memory JsonNode response.
     * Priority:
     * 1) explicitLabel if not null/empty
     * 2) "id" field inside the JsonNode (if present and non-empty)
     * 3) fallback "in-memory"
     */
    private String resolveIdForJsonNode(JsonNode canonicalResponseNode, String explicitLabel) {
        // 1) label
        if (explicitLabel != null && !explicitLabel.isEmpty()) {
            return explicitLabel;
        }

        // 2) try to read "id" from JsonNode
        if (canonicalResponseNode != null && canonicalResponseNode.has("id")) {
            JsonNode idNode = canonicalResponseNode.get("id");
            if (idNode != null && !idNode.isNull()) {
                String id = idNode.asText();
                if (id != null && !id.isEmpty()) {
                    return id;
                }
            }
        }

        // 3) default value
        return "in-memory";
    }
}