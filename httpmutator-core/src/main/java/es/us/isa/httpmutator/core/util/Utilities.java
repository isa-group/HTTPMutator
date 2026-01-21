package es.us.isa.httpmutator.core.util;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

public class Utilities {

    public static float assignWeight(String type, String mutationOperator) {
        switch (type) {
            case "Long":
                return Float.parseFloat(readProperty("operator.value.long.weight." + mutationOperator));
            case "Double":
                return Float.parseFloat(readProperty("operator.value.double.weight." + mutationOperator));
            case "Boolean":
                return Float.parseFloat(readProperty("operator.value.boolean.weight." + mutationOperator));
            case "String":
                return Float.parseFloat(readProperty("operator.value.string.weight." + mutationOperator));
            case "NullNode":
                return Float.parseFloat(readProperty("operator.value.null.weight." + mutationOperator));
            case "ObjectNode":
                return Float.parseFloat(readProperty("operator.object.weight." + mutationOperator));
            case "ArrayNode":
                return Float.parseFloat(readProperty("operator.array.weight." + mutationOperator));
            // mediaTypeMutator
            case "MediaTypeMutator":
                return Float.parseFloat(readProperty("operator.header.mediaType.weight." + mutationOperator));
            // charsetMutator
            case "CharsetMutator":
                return Float.parseFloat(readProperty("operator.header.charset.weight." + mutationOperator));
            // lacationMutator
            case "LocationMutator":
                return Float.parseFloat(readProperty("operator.header.location.weight." + mutationOperator));
            default:
                throw new IllegalArgumentException("Wrong class: "+ type +". The class passed to the " +
                        "constructor must be Long, Double, Boolean, String, NullNode, ObjectNode or ArrayNode.");
        }
    }
}
