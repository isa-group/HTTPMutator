package es.us.isa.httpmutator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Class to manage insertion, deletion and replacement of elements in JSON (objects
 * and arrays).
 *
 * @author Alberto Martin-Lopez
 */
public class JsonManager {

    /**
     * Replace element in an object or array. NOTE: In the case of an object, if the
     * property didn't exist, it creates it (unlike with arrays, where the element is
     * always replaced).
     *
     * @param jsonNode     The object or array where to replace the element
     * @param element      The value of the element after being replaced
     * @param propertyName The name of the property to replace. Must be null if jsonNode
     *                     is an ArrayNode
     * @param index        The index position in the array where to replace the element. Must
     *                     be null if jsonNode is an ObjectNode
     */
    public static void insertElement(JsonNode jsonNode, Object element, String propertyName, Integer index) {
        boolean isObj = index == null; // If index==null, jsonNode is an object, otherwise it is an array
        if (element instanceof String) {
            if (isObj) ((ObjectNode) jsonNode).put(propertyName, (String) element);
            else ((ArrayNode) jsonNode).set(index, new TextNode((String) element));
        } else if (element instanceof Long) {
            if (isObj) ((ObjectNode) jsonNode).put(propertyName, (Long) element);
            else ((ArrayNode) jsonNode).set(index, new LongNode((Long) element));
        } else if (element instanceof Double) {
            if (isObj) ((ObjectNode) jsonNode).put(propertyName, (Double) element);
            else ((ArrayNode) jsonNode).set(index, new DoubleNode((Double) element));
        } else if (element instanceof Boolean) {
            if (isObj) ((ObjectNode) jsonNode).put(propertyName, (Boolean) element);
            else ((ArrayNode) jsonNode).set(index, (Boolean) element ? BooleanNode.TRUE : BooleanNode.FALSE);
        } else if (element instanceof NullNode) {
            if (isObj) ((ObjectNode) jsonNode).putNull(propertyName);
            else ((ArrayNode) jsonNode).set(index, (NullNode) element);
        } else if (element instanceof ObjectNode) {
            if (isObj) ((ObjectNode) jsonNode).replace(propertyName, (ObjectNode) element);
            else ((ArrayNode) jsonNode).set(index, (ObjectNode) element);
        } else if (element instanceof ArrayNode) {
            if (isObj) ((ObjectNode) jsonNode).replace(propertyName, (ArrayNode) element);
            else ((ArrayNode) jsonNode).set(index, (ArrayNode) element);
        } else {
            throw new IllegalArgumentException("The element to insert must be a string, int, float, boolean, " +
                    "object, array or null value.");
        }
    }

    /**
     * Given a JsonNode (e.g., TextNode, NumberNode, etc.), returns the value
     * of such node as a basic type (e.g., String, Float, etc.)
     *
     * @param jsonNode The object or array whose value will be returned as a basic type
     * @return The value of the element as a basic type
     */
    public static Object getNodeElement(JsonNode jsonNode) {
        if (jsonNode.isTextual())
            return jsonNode.asText();
        else if (jsonNode.isIntegralNumber())
            return jsonNode.asLong();
        else if (jsonNode.isFloatingPointNumber())
            return jsonNode.asDouble();
        else if (jsonNode.isBoolean())
            return jsonNode.asBoolean();
        else if (jsonNode.isNull())
            return null;
        else if (jsonNode.isContainerNode())
            return jsonNode;
        else
            throw new IllegalArgumentException("Element not supported. It must be a string, int, float, boolean, " +
                    "object, array or null value.");
    }

    public static JsonNode toJsonNode(Object v, ObjectMapper mapper) {
        final JsonNodeFactory f = mapper.getNodeFactory();
        if (v == null) return f.nullNode();
        if (v instanceof JsonNode) return (JsonNode) v;

        // primitives & common
        if (v instanceof String)     return f.textNode((String) v);
        if (v instanceof Boolean)    return f.booleanNode((Boolean) v);
        if (v instanceof byte[])     return f.binaryNode((byte[]) v);

        // numbers
        if (v instanceof BigDecimal) return f.numberNode((BigDecimal) v);
        if (v instanceof BigInteger) return f.numberNode((BigInteger) v);
        if (v instanceof Long)       return f.numberNode((Long) v);
        if (v instanceof Integer)    return f.numberNode((Integer) v);
        if (v instanceof Short)      return f.numberNode(((Short) v).intValue());
        if (v instanceof Byte)       return f.numberNode(((Byte) v).intValue());
        if (v instanceof Double)     return f.numberNode((Double) v);
        if (v instanceof Float)      return f.numberNode(((Float) v).doubleValue());

        return null;
    }
}
