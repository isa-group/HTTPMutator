package es.us.isa.httpmutator.core.writer;

import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * Writes mutated HTTP responses produced by the mutation engine.
 *
 * <p>Implementations are responsible for emitting mutated interactions in a
 * specific output format (JSONL, HAR, database records, logs, etc.).</p>
 *
 * <p>The writer receives:</p>
 * <ul>
 *     <li>The original {@link HttpExchange} (including request, if available)</li>
 *     <li>The mutated {@link StandardHttpResponse}</li>
 *     <li>The {@link Mutant} metadata describing which mutator/operator was applied</li>
 * </ul>
 */
public interface MutantWriter extends Flushable, Closeable {

    /**
     * Writes a single mutated response associated with the original exchange.
     *
     * @param exchange        original HTTP exchange (with canonical request/response and id)
     * @param mutatedResponse mutated canonical response (Status, Headers, Body)
     * @param mutant          metadata about the applied mutation
     * @throws IOException if a write error occurs
     */
    void write(HttpExchange exchange,
               StandardHttpResponse mutatedResponse,
               Mutant mutant) throws IOException;
}