package es.us.isa.httpmutator.core.body;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.us.isa.httpmutator.core.AbstractMutator;
import static es.us.isa.httpmutator.core.util.JsonManager.insertElement;
import static es.us.isa.httpmutator.core.util.OperatorNames.DISORDER_ELEMENTS;
import static es.us.isa.httpmutator.core.util.OperatorNames.EMPTY;
import static es.us.isa.httpmutator.core.util.OperatorNames.REMOVE_ELEMENT;
import static es.us.isa.httpmutator.core.util.OperatorNames.REMOVE_OBJECT_ELEMENT;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Superclass for object and array mutators. Both share common functionalities,
 * especially the {@link AbstractObjectOrArrayMutator#getMutatedNode} method, which
 * allows to perform mutations on first-level object or arrays.
 *
 * @author Alberto Martin-Lopez
 */
public abstract class AbstractObjectOrArrayMutator extends AbstractMutator {

    protected int minMutations;
    protected int maxMutations;

    public AbstractObjectOrArrayMutator() {
        super();
        switch(this.getClass().getSimpleName()) {
            case "ArrayMutator":
                prob = Float.parseFloat(readProperty("operator.array.prob"));
                minMutations = Integer.parseInt(readProperty("operator.array.mutations.min"));
                maxMutations = Integer.parseInt(readProperty("operator.array.mutations.max"));
                break;
            case "ObjectMutator":
                prob = Float.parseFloat(readProperty("operator.object.prob"));
                minMutations = Integer.parseInt(readProperty("operator.object.mutations.min"));
                maxMutations = Integer.parseInt(readProperty("operator.object.mutations.max"));
                break;
            default:
                throw new IllegalArgumentException("Wrong class: " + this.getClass().getSimpleName() +
                        ". The abstract class 'AbstractObjectOrArrayMutator' can only be extended by " +
                        "ArrayMutator or ObjectMutator.");
        }
        resetOperators();
    }

    public int getMinMutations() {
        return minMutations;
    }

    public void setMinMutations(int minMutations) {
        this.minMutations = minMutations;
    }

    public int getMaxMutations() {
        return maxMutations;
    }

    public void setMaxMutations(int maxMutations) {
        this.maxMutations = maxMutations;
    }

    /**
     * Auxiliary function to clear the map of operators and add all of them again.
     * Needed because when one type of mutation is applied, it is removed from the
     * map so that it is not selected again.
     */
    public abstract void resetOperators();

    /**
     * This function is like {@link AbstractObjectOrArrayMutator#resetOperators}
     * but setting only operators that apply to the first level of a JSON array,
     * i.e. addProperty, removeProperty and empty.
     */
    public abstract void resetFirstLevelOperators();

    /**
     * The mutate method of the ObjectOrArrayMutator is a bit different from others,
     * since multiple mutations (between {@link AbstractObjectOrArrayMutator#minMutations}
     * and {@link AbstractObjectOrArrayMutator#maxMutations}) can be applied in the
     * same call. Given an object and the name of a property, mutate the value of that
     * property with probability {@link AbstractObjectOrArrayMutator#prob}
     *
     * @return True if at least one mutation was applied, false otherwise
     */
    @Override
    public boolean mutate(ObjectNode objectNode, String propertyName) {
        return mutate(objectNode, propertyName, null);
    }

    /**
     * The mutate method of the ObjectOrArrayMutator is a bit different from others,
     * since multiple mutations (between {@link AbstractObjectOrArrayMutator#minMutations}
     * and {@link AbstractObjectOrArrayMutator#maxMutations}) can be applied in the
     * same call. Given an array and the index of an element, mutate that element
     * with probability {@link AbstractObjectOrArrayMutator#prob}
     *
     * @return True if at least one mutation was applied, false otherwise
     */
    @Override
    public boolean mutate(ArrayNode arrayNode, int index) {
        return mutate(arrayNode, null, index);
    }

    /**
     * Auxiliary function to manage object or array mutations regardless of whether
     * the element to mutate is inside an object or inside an array.
     *
     * @param jsonNode ObjectNode or ArrayNode where the property containing an
     *                 object or array is located
     * @param propertyName Name of the property of the jsonNode (ObjectNode)
     *                     containing an object or array. Must be null if jsonNode
     *                     is an ArrayNode
     * @param index Index of the element of the jsonNode (ArrayNode) that is an
     *              object or array. Must be null if jsonNode is an ObjectNode
     *
     * @return True if at least one mutation was applied, false otherwise
     */
    protected boolean mutate(JsonNode jsonNode, String propertyName, Integer index) {
        boolean isObj = index==null; // If index==null, jsonNode is an object, otherwise it is an array
        Boolean elementWasObj = null; // Whether the elementToMutate was an object in the previous iteration or not
        int nMutations = rand1.nextInt(minMutations, maxMutations);
        boolean wasMutated = false;
        for (int i=0; i<nMutations; i++) {
            JsonNode elementToMutate = isObj ? jsonNode.get(propertyName) : jsonNode.get(index);
            // The mutation could make the object or array null or of other type. Also, it could convert an
            // object into an array or vice versa. In any case, stop mutating:
            if (!(elementToMutate.isObject() || elementToMutate.isArray()) || (elementWasObj != null &&
                            ((elementToMutate.isObject() && !elementWasObj) ||
                            (elementToMutate.isArray() && elementWasObj)))) {
                break;
            } else {
                elementWasObj = elementToMutate.isObject(); // Update elementWasObj value for next iteration
                if (shouldApplyMutation()) {
                    // Mutate element by randomly choosing one mutation operator among 'operators' and applying the mutation:
                    String operator = getOperator();
                    // If node is empty and an operator that will make no changes is selected
                    if (elementToMutate.size() == 0 && (operator.equals(REMOVE_ELEMENT)  || operator.equals(REMOVE_OBJECT_ELEMENT) || operator.equals(DISORDER_ELEMENTS) || operator.equals(EMPTY))) {
                        operators.remove(REMOVE_ELEMENT); // Discard all those operators
                        operators.remove(REMOVE_OBJECT_ELEMENT);
                        operators.remove(DISORDER_ELEMENTS);
                        operators.remove(EMPTY);
                        operator = getOperator(); // And select other
                    }
                    if (operator != null) {
                        Object mutatedElement = operators.get(operator).mutate(elementToMutate);
                        operators.remove(operator); // Remove that operator so that the mutation isn't applied twice
                        // Replace original element with mutated element:
                        insertElement(jsonNode, mutatedElement, propertyName, index);
                        if (!wasMutated)
                            wasMutated = true;
                    }
                }
            }
        }
        resetOperators(); // After all mutations are applied, reset operators to have all of them in the map again

        return wasMutated;
    }

    /**
     * This function is to be called for mutating a first-level JSON object or array,
     * otherwise the mutate() method should be called. The reason is that a first-level
     * object is not contained inside any object or array, therefore it is not possible
     * to use the mutate() method.
     *
     * @param jsonNode The JSON object or array to mutate
     * @return The mutated JSON object or array
     */
    public JsonNode getMutatedNode(JsonNode jsonNode) {
        resetFirstLevelOperators(); // Use only first level operators
        int nMutations = rand1.nextInt(minMutations, maxMutations);
        for (int i=0; i<nMutations; i++) {
            if (shouldApplyMutation()) {
                // Mutate element by randomly choosing one mutation operator among 'operators' and applying the mutation:
                String operator = getOperator();

                // If node is empty and an operator that will make no changes is selected
                if (jsonNode.size() == 0 && (operator.equals(REMOVE_ELEMENT) || operator.equals(REMOVE_OBJECT_ELEMENT) || operator.equals(DISORDER_ELEMENTS) || operator.equals(EMPTY))) {
                    operators.remove(REMOVE_ELEMENT); // Discard all those operators
                    operators.remove(REMOVE_OBJECT_ELEMENT);
                    operators.remove(DISORDER_ELEMENTS);
                    operators.remove(EMPTY);
                    operator = getOperator(); // And select other
                }

                // If node is an object and REMOVE_OBJECT_ELEMENT op. was selected, check that there are nested objects, otherwise choose a different op.
                if (operator.equals(REMOVE_OBJECT_ELEMENT)) {
                    boolean containsObjects = false;
                    Iterator<JsonNode> jsonIterator = jsonNode.elements();
                    while (jsonIterator.hasNext()) {
                        if (jsonIterator.next().isObject()) {
                            containsObjects = true;
                            break;
                        }
                    }
                    if (!containsObjects) {
                        operators.remove(REMOVE_OBJECT_ELEMENT); // This operator can't be applied, discard it
                        operator = getOperator(); // And select other
                    }
                }

                if (operator != null) {
                    jsonNode = (JsonNode)operators.get(operator).mutate(jsonNode);
                    operators.remove(operator); // Remove that operator so that the mutation isn't applied twice
                }
            }
        }
        resetOperators(); // After all mutations are applied, reset operators to have all of them in the map again

        return jsonNode;
    }
}
