package es.us.isa.httpmutator.core.reporter;

import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.IOException;

/**
 * Callback-style reporter that observes mutants produced during mutation.
 *
 * <p>Implementations can use this hook to:
 * <ul>
 *     <li>Collect statistics (e.g. per-operator counts)</li>
 *     <li>Generate CSV/JSON reports</li>
 *     <li>Emit logs or metrics</li>
 * </ul>
 * </p>
 *
 * <p>The reporter is intended to be used by a high-level mutation pipeline:
 * <ol>
 *     <li>For every selected {@link Mutant}, {@link #onMutant(HttpExchange, StandardHttpResponse, Mutant)} is called</li>
 *     <li>When the pipeline finishes, {@link #onFinished()} is called exactly once</li>
 * </ol>
 * </p>
 */
public interface MutantReporter {

    /**
     * Called for every selected mutant.
     *
     * @param exchange        original HTTP exchange (id + canonical request/response)
     * @param mutatedResponse the mutated canonical response
     * @param mutant          metadata describing the applied mutation
     */
    void onMutant(HttpExchange exchange,
                  StandardHttpResponse mutatedResponse,
                  Mutant mutant);

    /**
     * Called once after all mutants have been processed.
     *
     * <p>Reporters should flush and close any underlying resources here
     * (e.g. write CSV files, close writers, etc.).</p>
     *
     * @throws IOException if flushing/writing fails
     */
    default void onFinished() throws IOException {
        // default no-op
    }
}