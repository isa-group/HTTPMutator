package es.us.isa.httpmutator.core.converter;

import es.us.isa.httpmutator.core.model.StandardHttpRequest;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

/**
 * Bidirectional converter interface for converting between
 * original HTTP types and HttpMutator canonical models
 * ({@link StandardHttpRequest} and {@link StandardHttpResponse}).
 *
 * <p>This converter supports:</p>
 * <ul>
 *     <li>Original → StandardHttpResponse</li>
 *     <li>StandardHttpResponse → Original</li>
 *     <li>Original → StandardHttpRequest (optional)</li>
 *     <li>StandardHttpRequest → Original (optional)</li>
 * </ul>
 *
 * <p>Request conversion is optional. By default, request methods are no-ops
 * returning {@code null}. Implementors may override them for formats such as HAR.</p>
 *
 * @param <T> The original HTTP format (HAR node, Java type, raw JSON node, etc.)
 */
public interface BidirectionalConverter<T> {

    // ======================================================
    // Response Conversion (Required)
    // ======================================================

    /**
     * Convert from original response to canonical {@link StandardHttpResponse}.
     */
    StandardHttpResponse toStandardResponse(T originalResponse) throws ConversionException;

    /**
     * Convert from canonical {@link StandardHttpResponse} back to original response type.
     */
    T fromStandardResponse(StandardHttpResponse standardResponse) throws ConversionException;


    /**
     * Whether the converter can produce an original response from the canonical model.
     */
    default boolean supportsReverseConversion(StandardHttpResponse standardResponse) {
        return true;
    }


    // ======================================================
    // Request Conversion (Optional — defaults do nothing)
    // ======================================================

    /**
     * Convert original request format to canonical {@link StandardHttpRequest}.
     *
     * <p>Default: return {@code null}, meaning request conversion is not supported.</p>
     */
    default StandardHttpRequest toStandardRequest(T originalRequest) throws ConversionException {
        return null;
    }

    /**
     * Convert canonical {@link StandardHttpRequest} back to the original request type.
     *
     * <p>Default: return {@code null}, meaning reverse request conversion is not supported.</p>
     */
    default T fromStandardRequest(StandardHttpRequest standardRequest) throws ConversionException {
        return null;
    }

    /**
     * Check if this converter supports converting requests (default: false).
     */
    default boolean supportsRequestConversion() {
        return false;
    }


    // ======================================================
    // Metadata
    // ======================================================

    /**
     * Name of this converter for identification.
     */
    String getName();

    /**
     * Whether this converter supports the given response class type.
     */
    boolean supports(Class<?> responseType);
}