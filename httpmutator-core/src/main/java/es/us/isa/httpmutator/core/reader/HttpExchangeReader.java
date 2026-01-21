package es.us.isa.httpmutator.core.reader;

import es.us.isa.httpmutator.core.model.HttpExchange;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

/**
 * A reader that streams HTTP interactions ({@link HttpExchange}) from an input source.
 *
 * <p>This abstraction decouples input formats (JSONL, HAR, Java-specific
 * response logs, custom trace files, etc.) from the mutation engine. Each
 * implementation is responsible for:</p>
 *
 * <ul>
 *     <li>Reading data from an input {@link Reader}</li>
 *     <li>Parsing it into a sequence of {@link HttpExchange} objects</li>
 *     <li>Providing a canonical request (which may be {@code null}) and a
 *         non-null canonical response embedded in {@link HttpExchange}</li>
 *     <li>Assigning a stable identifier to each exchange (line number, HAR entry ID, ...)</li>
 *     <li>Feeding each exchange into the provided {@link Consumer}</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * Implementations should:
 * <ul>
 *     <li>Never store all interactions in memory; processing must be streaming.</li>
 *     <li>Normalize the response into the canonical format expected by the
 *         mutation engine (typically via converter utilities).</li>
 *     <li>Ensure that each {@link HttpExchange} contains a non-null canonical response.</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * Implementations may throw {@link IOException} for input or parsing errors.
 *
 * <h2>Example Implementations</h2>
 * <ul>
 *     <li>{@code JsonlExchangeReader} — reads JSONL where each line is a response
 *         (request is {@code null}).</li>
 *     <li>{@code HarExchangeReader} — reads HAR entries; request/response are
 *         converted to {@code StandardHttpRequest} / {@code StandardHttpResponse}.</li>
 *     <li>Custom readers — for API gateway logs, proxy dumps, bespoke monitoring formats, etc.</li>
 * </ul>
 */
public interface HttpExchangeReader {

    /**
     * Reads HTTP interactions from the input and streams each interaction as an
     * {@link HttpExchange} to the provided {@code consumer}.
     *
     * @param in       the input source (streamed)
     * @param consumer callback invoked for each parsed {@link HttpExchange}
     * @throws IOException if an input or parsing error occurs
     */
    void read(Reader in, Consumer<HttpExchange> consumer) throws IOException;
}