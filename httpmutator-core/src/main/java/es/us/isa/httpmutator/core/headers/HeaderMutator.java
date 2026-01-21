package es.us.isa.httpmutator.core.headers;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.headers.charset.CharsetMutator;
import es.us.isa.httpmutator.core.headers.location.LocationMutator;
import es.us.isa.httpmutator.core.headers.mediaType.MediaTypeMutator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class HeaderMutator extends AbstractMutator {

    private static final String CONTENT_TYPE_HEADER = "content-type";
    private static final String LOCATION_HEADER = "location";
    private static final Set<String> MEDIA_TYPE_PREFIXES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "application", "audio", "image", "message", "model", "multipart", "text", "video")));

    private final Random rand = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CharsetMutator charsetMutator;
    private MediaTypeMutator mediaTypeMutator;
    private LocationMutator locationMutator;

    public HeaderMutator() {
        resetMutators();
    }

    private void resetMutators() {
        charsetMutator = Boolean.parseBoolean(readProperty("operator.header.charset.enabled")) ? new CharsetMutator()
                : null;
        mediaTypeMutator = Boolean. parseBoolean(readProperty("operator.header.mediaType.enabled"))
                ? new MediaTypeMutator()
                : null;
        locationMutator = Boolean.parseBoolean(readProperty("operator.header.location.enabled")) ? new LocationMutator()
                : null;
    }

     /**
     * Process header mutants by path in streaming fashion.
     * Each header component (media-type, charset, location) is processed separately.
     * 
     * @param node the headers JsonNode to mutate
     * @param probability the probability for mutation generation  
     * @param consumer consumer to process each MutantGroup
     */
    public void getAllMutants(JsonNode node, double probability, Consumer<MutantGroup> consumer) {
        adjustMutatorsBasedOnPresence(node);
        
        // Process Content-Type header components
        processContentTypeMutants(node, probability, consumer);
        
        // Process Location header
        processLocationMutants(node, probability, consumer);
    }

    /**
     * String version with streaming support.
     */
    public void getAllMutants(String headers, double probability, Consumer<MutantGroup> consumer) {
        JsonNode responseNode;
        try {
            responseNode = objectMapper.readTree(headers);
            getAllMutants(responseNode, probability, consumer);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error parsing response: " + e.getMessage(), e);
        }
    }

    // Adjust operators based on header presence
    private void adjustMutatorsBasedOnPresence(JsonNode node) {
        // Handle Content-Type header
        if (hasHeader(node, CONTENT_TYPE_HEADER)) {
            String contentType = getHeaderValue(node, CONTENT_TYPE_HEADER).toLowerCase();
            boolean hasMediaType = MEDIA_TYPE_PREFIXES.stream().anyMatch(contentType::startsWith);
            boolean hasCharset = contentType.contains("charset=");

            if (mediaTypeMutator != null && !hasMediaType) {
                mediaTypeMutator.getOperators().remove(OperatorNames.NULL);
            }
            if (charsetMutator != null && !hasCharset) {
                charsetMutator.getOperators().remove(OperatorNames.NULL);
            }
        } else {
            if (mediaTypeMutator != null) {
                mediaTypeMutator.getOperators().remove(OperatorNames.NULL);
            }
            if (charsetMutator != null) {
                charsetMutator.getOperators().remove(OperatorNames.NULL);
            }
        }

        // Handle Location header
        if (locationMutator != null && !hasHeader(node, LOCATION_HEADER)) {
            locationMutator.getOperators().remove(OperatorNames.NULL);
        }
    }

    /**
     * Process Content-Type header mutants (media-type and charset components).
     */
    private void processContentTypeMutants(JsonNode node, double probability, Consumer<MutantGroup> consumer) {
        if (!hasHeader(node, CONTENT_TYPE_HEADER)) {
            return;
        }

        String contentType = getHeaderValue(node, CONTENT_TYPE_HEADER);
        ContentTypeComponents components = new ContentTypeComponents(contentType);

        // Process media type mutants
        processMediaTypeMutants(node, components, probability, consumer);
        
        // Process charset mutants  
        processCharsetMutants(node, components, probability, consumer);
    }

    /**
     * Process media type mutants for Content-Type header.
     */
    private void processMediaTypeMutants(JsonNode node, ContentTypeComponents components, 
                                        double probability, Consumer<MutantGroup> consumer) {
        if (mediaTypeMutator == null) {
            return;
        }

        List<Mutant> mediaTypeMutants = new ArrayList<>();
        
        for (AbstractOperator operator : mediaTypeMutator.getOperators().values()) {
            if (shouldSkipMutation(probability)) {
                continue;
            }
            Mutant mutant = mutateComponent(node, components, operator, true);
            mediaTypeMutants.add(mutant);
        }

        if (!mediaTypeMutants.isEmpty()) {
            MutantGroup mutantGroup = new MutantGroup("Headers/" + CONTENT_TYPE_HEADER + "/mediaType", mediaTypeMutants);
            consumer.accept(mutantGroup);
        }
    }

    /**
     * Process charset mutants for Content-Type header.
     */
    private void processCharsetMutants(JsonNode node, ContentTypeComponents components, 
                                      double probability, Consumer<MutantGroup> consumer) {
        if (charsetMutator == null) {
            return;
        }

        List<Mutant> charsetMutants = new ArrayList<>();
        
        for (AbstractOperator operator : charsetMutator.getOperators().values()) {
            if (shouldSkipMutation(probability)) {
                continue;
            }
            Mutant mutant = mutateComponent(node, components, operator, false);
            charsetMutants.add(mutant);
        }

        if (!charsetMutants.isEmpty()) {
            MutantGroup mutantGroup = new MutantGroup("Headers/" + CONTENT_TYPE_HEADER + "/charset", charsetMutants);
            consumer.accept(mutantGroup);
        }
    }

    /**
     * Process Location header mutants.
     */
    private void processLocationMutants(JsonNode node, double probability, Consumer<MutantGroup> consumer) {
        if (!hasHeader(node, LOCATION_HEADER) || locationMutator == null) {
            return;
        }

        List<Mutant> locationMutants = new ArrayList<>();
        String location = getHeaderValue(node, LOCATION_HEADER);
        
        for (AbstractOperator operator : locationMutator.getOperators().values()) {
            if (shouldSkipMutation(probability)) {
                continue;
            }

            Object mutated = operator.mutate(location);
            ObjectNode copiedNode = ((ObjectNode) node).deepCopy();
            updateHeaderField(copiedNode, LOCATION_HEADER, mutated);

            Mutant mutant = new Mutant(
                "Headers/" + LOCATION_HEADER,
                copiedNode,
                this.getClass(),
                operator.getClass()
            );
            locationMutants.add(mutant);
        }

        if (!locationMutants.isEmpty()) {
            MutantGroup mutantGroup = new MutantGroup("Headers/" + LOCATION_HEADER, locationMutants);
            consumer.accept(mutantGroup);
        }
    }


    // Helper method for component mutation
    private Mutant mutateComponent(
            JsonNode node,
            ContentTypeComponents components,
            AbstractOperator operator,
            boolean isMediaType) {
        String originalValue = isMediaType ? components.mediaType : components.charsetValue;
        Object mutatedValue = operator.mutate(originalValue);

        ContentTypeComponents newComponents = new ContentTypeComponents(components);
        if (isMediaType) {
            newComponents.mediaType = mutatedValue != null && !mutatedValue.toString().equals("null")
                    ? mutatedValue.toString()
                    : null;
        } else {
            newComponents.charsetValue = mutatedValue != null && !mutatedValue.toString().equals("null")
                    ? mutatedValue.toString()
                    : null;
        }

        ObjectNode copiedNode = ((ObjectNode) node).deepCopy();
        updateHeaderField(copiedNode, CONTENT_TYPE_HEADER, newComponents.buildHeaderValue());
        return new Mutant(
                "Headers/" + CONTENT_TYPE_HEADER + "/" + (isMediaType ? "mediaType" : "charset"),
                copiedNode,
                this.getClass(),
                operator.getClass());
    }

    // Utility class for Content-Type decomposition
    private static class ContentTypeComponents {

        String mediaType;
        String charsetValue;
        final Map<String, String> otherParams = new LinkedHashMap<>();

        ContentTypeComponents(String headerValue) {
            String[] parts = headerValue.split(";");

            // Extract media type
            this.mediaType = parts.length > 0 ? parseMediaType(parts[0].trim()) : null;

            // Process remaining parameters
            for (int i = 0; i < parts.length; i++) {
                String[] kv = parts[i].trim().split("=", 2);
                if (kv.length == 2) {
                    if (kv[0].equalsIgnoreCase("charset")) {
                        this.charsetValue = kv[1].trim();
                    } else {
                        this.otherParams.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        }

        // Copy constructor
        ContentTypeComponents(ContentTypeComponents other) {
            this.mediaType = other.mediaType;
            this.charsetValue = other.charsetValue;
            this.otherParams.putAll(other.otherParams);
        }

        String buildHeaderValue() {
            StringBuilder sb = new StringBuilder();
            if (mediaType != null) {
                sb.append(mediaType);
            }

            if (charsetValue != null) {
                if (mediaType != null) {
                    sb.append("; ");
                }
                sb.append("charset=").append(charsetValue);
            }

            for (Map.Entry<String, String> entry : otherParams.entrySet()) {
                sb.append("; ").append(entry.getKey()).append("=").append(entry.getValue());
            }
            return sb.toString();
        }

        private String parseMediaType(String candidate) {
            return MEDIA_TYPE_PREFIXES.stream().anyMatch(candidate::startsWith) ? candidate : null;
        }
    }

    // Utility methods
    private boolean shouldSkipMutation(double probability) {
        return rand.nextFloat() >= probability;
    }

    private void updateHeaderField(ObjectNode node, String headerName, Object value) {
        String actualFieldName = getHeaderFieldName(node, headerName);
        if (value == null || value.toString().equals("null") || value.toString().isEmpty()) {
            node.remove(actualFieldName);
        } else {
            node.put(actualFieldName, value.toString());
        }
    }

    private String getHeaderFieldName(JsonNode node, String headerName) {
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (field.equalsIgnoreCase(headerName)) {
                return field;
            }
        }
        return headerName; // For new headers
    }

    private boolean hasHeader(JsonNode node, String headerName) {
        if (node == null || !node.isObject()) {
            return false;
        }
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
                .anyMatch(name -> name.equalsIgnoreCase(headerName));
    }

    private String getHeaderValue(JsonNode node, String headerName) {
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (field.equalsIgnoreCase(headerName)) {
                return node.get(field).asText();
            }
        }
        return "";
    }

}
