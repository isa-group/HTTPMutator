package es.us.isa.httpmutator.core;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import es.us.isa.httpmutator.core.body.value.boolean0.BooleanMutator;
import es.us.isa.httpmutator.core.body.value.double0.DoubleMutator;
import es.us.isa.httpmutator.core.body.value.long0.LongMutator;
import es.us.isa.httpmutator.core.body.value.null0.NullMutator;
import es.us.isa.httpmutator.core.body.value.string0.StringMutator;
import es.us.isa.httpmutator.core.util.JsonManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.us.isa.httpmutator.core.body.BodyMutator;
import es.us.isa.httpmutator.core.headers.HeaderMutator;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.sc.StatusCodeMutator;


/**
 * Core mutation engine, containing all concrete mutation logic and assembly.
 *
 * Package-private: not intended to be used directly by library users.
 * Use {@link HttpMutator} as the public facade.
 */
public class HttpMutatorEngine {
    private static final Logger logger = LogManager.getLogger(HttpMutatorEngine.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StatusCodeMutator statusCodeMutator;
    private HeaderMutator headerMutator;
    private BodyMutator bodyMutator;

    private BooleanMutator booleanMutator;
    private DoubleMutator doubleMutator;
    private LongMutator longMutator;
    private StringMutator stringMutator;
    private NullMutator nullMutator;

    private final static double defaultPossibility = 1.0;

    public HttpMutatorEngine() {
        resetMutators();
    }

    private void resetMutators() {
        statusCodeMutator = Boolean.parseBoolean(readProperty("operator.sc.enabled")) ? new StatusCodeMutator() : null;
        headerMutator = Boolean.parseBoolean(readProperty("operator.header.enabled")) ? new HeaderMutator() : null;
        bodyMutator = Boolean.parseBoolean(readProperty("operator.body.enabled")) ? new BodyMutator() : null;

        booleanMutator = Boolean.parseBoolean(readProperty("operator.value.boolean.enabled")) ? new BooleanMutator() : null;
        doubleMutator = Boolean.parseBoolean(readProperty("operator.value.double.enabled")) ? new DoubleMutator() : null;
        longMutator = Boolean.parseBoolean(readProperty("operator.value.long.enabled")) ? new LongMutator() : null;
        stringMutator = Boolean.parseBoolean(readProperty("operator.value.string.enabled")) ? new StringMutator() : null;
        nullMutator = Boolean.parseBoolean(readProperty("operator.value.null.enabled")) ? new NullMutator() : null;
    }

    public void getAllMutants(String response, Consumer<MutantGroup> consumer) {
        JsonNode responseNode = null;
        try {
            responseNode = objectMapper.readTree(response);
        } catch (IOException e) {
            logger.warn("Error parsing response: " + e.getMessage());
        }
        getAllMutants(responseNode, consumer);
    }

    /**
     * Process all mutants in streaming fashion to avoid memory explosion.
     * This method processes status code, headers, and body mutants one by one.
     *
     * @param node        the response JsonNode to mutate
     * @param consumer    consumer to process each mutant as it's generated
     */
    public void getAllMutants(JsonNode node, Consumer<MutantGroup> consumer) {
        if (!isValidResponse(node)) {
            logger.warn("Response must include status code, headers, and body");
            return;
        }

        // Process status code mutants
        processStatusCodeMutants(node, consumer);

        // Process header mutants
        processHeaderMutants(node, consumer);

        // Process body mutants (using new streaming approach)
        processBodyMutants(node, consumer);
    }

    // ========== Component-specific processing methods ==========

    void processStatusCodeMutants(JsonNode node, Consumer<MutantGroup> consumer) {
        if (statusCodeMutator != null) {
            int statusCode = node.get("Status Code").asInt();
            statusCodeMutator.getAllMutants(statusCode, defaultPossibility, mutantGroup -> {
                // Assemble complete response with mutated status code
                MutantGroup assembledGroup = assembleStatusCodeMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            });
        }
    }

    void processHeaderMutants(JsonNode node, Consumer<MutantGroup> consumer) {
        if (headerMutator != null) {
            JsonNode headers = node.get("Headers");
            headerMutator.getAllMutants(headers, defaultPossibility, mutantGroup -> {
                // Assemble complete response with mutated headers
                MutantGroup assembledGroup = assembleHeaderMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            });
        }
    }

    void processBodyMutants(JsonNode node, Consumer<MutantGroup> consumer) {
        JsonNode body = node.get("Body");
        if (bodyMutator != null && (body.isArray() || body.isObject())) {
            bodyMutator.getAllMutants(body, defaultPossibility, mutantGroup -> {
                // Assemble complete response with mutated body
                MutantGroup assembledGroup = assembleBodyMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            });
        } else if (longMutator != null && (body.isLong() || body.isInt())) {
            List<Mutant> currentPathMutants = new ArrayList<>();
            longMutator.getOperators().forEach((n, operator) -> {
                Object v = operator.mutate(body.asLong());
                Mutant mutant = new Mutant("Body", JsonManager.toJsonNode(v, objectMapper), LongMutator.class, operator.getClass());
                currentPathMutants.add(mutant);
            });
            if (!currentPathMutants.isEmpty()) {
                MutantGroup mutantGroup = new MutantGroup("Body", currentPathMutants);
                MutantGroup assembledGroup = assembleBodyMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            }
        } else if (doubleMutator != null && body.isDouble()) {
            List<Mutant> currentPathMutants = new ArrayList<>();
            doubleMutator.getOperators().forEach((n, operator) -> {
                Object v = operator.mutate(body.asDouble());
                Mutant mutant = new Mutant("Body", JsonManager.toJsonNode(v, objectMapper), DoubleMutator.class, operator.getClass());
                currentPathMutants.add(mutant);
            });
            if (!currentPathMutants.isEmpty()) {
                MutantGroup mutantGroup = new MutantGroup("Body", currentPathMutants);
                MutantGroup assembledGroup = assembleBodyMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            }
        } else if (stringMutator != null && body.isTextual()) {
            List<Mutant> currentPathMutants = new ArrayList<>();
            stringMutator.getOperators().forEach((n, operator) -> {
                Object v = operator.mutate(body.asText());
                Mutant mutant = new Mutant("Body", JsonManager.toJsonNode(v, objectMapper), StringMutator.class, operator.getClass());
                currentPathMutants.add(mutant);
            });
            if (!currentPathMutants.isEmpty()) {
                MutantGroup mutantGroup = new MutantGroup("Body", currentPathMutants);
                MutantGroup assembledGroup = assembleBodyMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            }
        } else if (nullMutator != null && body.isNull()) {
            List<Mutant> currentPathMutants = new ArrayList<>();
            nullMutator.getOperators().forEach((n, operator) -> {
                Object v = operator.mutate(null);
                Mutant mutant = new Mutant("Body", JsonManager.toJsonNode(v, objectMapper), NullMutator.class, operator.getClass());
                currentPathMutants.add(mutant);
            });
            if (!currentPathMutants.isEmpty()) {
                MutantGroup mutantGroup = new MutantGroup("Body", currentPathMutants);
                MutantGroup assembledGroup = assembleBodyMutants(node, mutantGroup);
                consumer.accept(assembledGroup);
            }
        } else {
            throw new IllegalArgumentException("Body must be an object, array, string, long, or double to be mutated: " + body.getNodeType());
        }
    }

    // ========== Response assembly methods ==========

    /**
     * Assembles complete HTTP response with mutated status codes
     */
    private MutantGroup assembleStatusCodeMutants(JsonNode originalResponse, MutantGroup statusCodeMutants) {
        List<Mutant> assembled = new ArrayList<>();
        for (Mutant statusCodeMutant : statusCodeMutants.getMutants()) {
            try {
                // Create complete response JSON
                ObjectNode completeResponse = objectMapper.createObjectNode();

                // Set mutated status code
                completeResponse.set("Status Code", statusCodeMutant.getMutatedNode());

                // Keep original headers and body
                completeResponse.set("Headers", originalResponse.get("Headers"));
                completeResponse.set("Body", originalResponse.get("Body"));

                // Create new mutant with complete response
                Mutant completeMutant = new Mutant(statusCodeMutant.getOriginalJsonPath(), completeResponse, statusCodeMutant.getMutatorClass(), statusCodeMutant.getOperatorClass());

                assembled.add(completeMutant);

            } catch (Exception e) {
                logger.warn("Failed to assemble status code mutant: " + e.getMessage());
            }
        }

        return new MutantGroup(statusCodeMutants.getIdentifier(), assembled);
    }

    /**
     * Assembles complete HTTP response with mutated headers
     */
    private MutantGroup assembleHeaderMutants(JsonNode originalResponse, MutantGroup headerMutants) {
        List<Mutant> assembled = new ArrayList<>();

        for (Mutant headerMutant : headerMutants.getMutants()) {
            try {
                // Create complete response JSON
                ObjectNode completeResponse = objectMapper.createObjectNode();

                // Keep original status code
                completeResponse.set("Status Code", originalResponse.get("Status Code"));

                // Set mutated headers
                completeResponse.set("Headers", headerMutant.getMutatedNode());

                // Keep original body
                completeResponse.set("Body", originalResponse.get("Body"));

                // Create new mutant with complete response
                Mutant completeMutant = new Mutant(headerMutant.getOriginalJsonPath(), completeResponse, headerMutant.getMutatorClass(), headerMutant.getOperatorClass());

                assembled.add(completeMutant);

            } catch (Exception e) {
                logger.warn("Failed to assemble header mutant: " + e.getMessage());
            }
        }

        return new MutantGroup(headerMutants.getIdentifier(), assembled);
    }

    /**
     * Assembles complete HTTP response with mutated body
     */
    private MutantGroup assembleBodyMutants(JsonNode originalResponse, MutantGroup bodyMutants) {
        List<Mutant> assembled = new ArrayList<>();

        for (Mutant bodyMutant : bodyMutants.getMutants()) {
            try {
                // Create complete response JSON
                ObjectNode completeResponse = objectMapper.createObjectNode();

                // Keep original status code and headers
                completeResponse.set("Status Code", originalResponse.get("Status Code"));
                completeResponse.set("Headers", originalResponse.get("Headers"));

                // Set mutated body
                completeResponse.set("Body", bodyMutant.getMutatedNode());

                // Create new mutant with complete response
                Mutant completeMutant = new Mutant(bodyMutant.getOriginalJsonPath(), completeResponse, bodyMutant.getMutatorClass(), bodyMutant.getOperatorClass());

                assembled.add(completeMutant);

            } catch (Exception e) {
                logger.warn("Failed to assemble body mutant: {}", e.getMessage());
            }
        }

        return new MutantGroup(bodyMutants.getIdentifier(), assembled);
    }

    private boolean isValidResponse(JsonNode node) {
        return node.isObject() && node.has("Status Code") && node.get("Status Code").isInt() && node.has("Headers") && node.get("Headers").isObject() && node.has("Body");
    }
}