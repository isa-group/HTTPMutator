package es.us.isa.httpmutator.core;

import static es.us.isa.httpmutator.core.util.JsonManager.insertElement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Superclass for mutators. A mutator decides on the type of mutation to be
 * applied
 * to an element (string, int, etc.) based on the different mutation operators
 * enabled for that element (boundary, replacement, etc.).
 *
 * @author Alberto Martin-Lopez
 */
public abstract class AbstractMutator extends RandomManager {
    protected LinkedHashMap<String, AbstractOperator> operators; // Map of mutation operators. The key should be equal to the
                                                           // one in the properties file
    protected float prob; // Probability to apply this mutation to an element. Configured in properties
                          // file

    public AbstractMutator() {
        super();
        operators = new LinkedHashMap<>(); // to produce operators in a deterministic order
    }

    public float getProb() {
        return prob;
    }

    public void setProb(float prob) {
        this.prob = prob;
    }

    public HashMap<String, AbstractOperator> getOperators() {
        return operators;
    }

    public void setOperators(LinkedHashMap<String, AbstractOperator> operators) {
        this.operators = operators;
    }

    protected boolean shouldApplyMutation() {
        return rand2.nextFloat() <= prob;
    }

    /**
     * Given a list (map) of mutation operators, selects one. To do so, it generates
     * a random float between 0 and {@code sum(weights_of_operators)} and chooses
     * the
     * operator whose weight is in that range. Example: <br>
     * 1.- StringReplacement. {@code weight=0.1}. <br>
     * 2.- StringMutation. {@code weight=0.4}. <br>
     * 3.- StringBoundary. {@code weight=0.2}. <br>
     * - {@code randomFloat=0.367}: StringMutation is selected, because the float
     * falls in the range 0.1-0.5 (0.1+0.4)
     *
     * @return The name of the mutation operator selected, or null if the map
     *         doesn't
     *         contain any operator
     */
    public String getOperator() {
        float sumWeights = operators.values().stream() // Sum all weights
                .map(AbstractOperator::getWeight)
                .reduce(0f, Float::sum);
        float randomFloat = rand2.nextFloat() * sumWeights; // Generate random float between 0 and sumWeights

        float acc = 0;
        String operatorName = null;
        Iterator<Map.Entry<String, AbstractOperator>> operatorIterator = operators.entrySet().iterator();
        Map.Entry<String, AbstractOperator> currentOperator;
        while (operatorIterator.hasNext() && sumWeights != 0) { // Iterate over mutation operators if sum of weights is
                                                                // greater than 0
            currentOperator = operatorIterator.next();
            acc += currentOperator.getValue().getWeight();
            if (randomFloat <= acc) { // When the condition is met, get operator name and break loop
                operatorName = currentOperator.getKey();
                break;
            }
        }

        return operatorName;
    }

    /**
     * Given an object and the name of a property, mutate the value of that property
     * with probability {@link AbstractMutator#prob}
     *
     * @param objectNode   the object to be mutated
     * @param propertyName the property name of the object to be mutated
     * @return True if the mutation was applied, false otherwise
     */
    public boolean mutate(ObjectNode objectNode, String propertyName) {
        return mutate(objectNode, propertyName, null);
    }

    /**
     * Given an array and the index of an element, mutate that element
     * with probability {@link AbstractMutator#prob}
     *
     * @param arrayNode the array that contains the element to be mutated
     * @param index     the index of the element to be mutated
     * @return True if the mutation was applied, false otherwise
     */
    public boolean mutate(ArrayNode arrayNode, int index) {
        return mutate(arrayNode, null, index);
    }

    private boolean mutate(JsonNode jsonNode, String propertyName, Integer index) {
        if (shouldApplyMutation()) {
            boolean isObj = index == null; // If index==null, jsonNode is an object, otherwise it is an array
            JsonNode element = isObj ? jsonNode.get(propertyName) : jsonNode.get(index);
            Object elementToMutate;
            if (element.isIntegralNumber()) {
                elementToMutate = element.asLong(); // Get number to mutate
            } else if (element.isFloatingPointNumber()) {
                elementToMutate = element.asDouble(); // Get floating number to mutate
            } else if (element.isTextual()) {
                elementToMutate = element.asText(); // Get string to mutate
            } else if (element.isBoolean()) {
                elementToMutate = element.asBoolean(); // Get boolean to mutate
            } else if (element.isNull()) {
                elementToMutate = element; // Get null to mutate
            } else if (element.isObject() || element.isArray()) {
                elementToMutate = element; // Get object or array to mutate
            } else {
                throw new IllegalArgumentException("The element " + element.toString() +
                        " cannot be mutated. Allowed mutations: strings, ints, floats, booleans," +
                        " nulls, objects or arrays.");
            }

            // Mutate element by randomly choosing one mutation operator among 'operators'
            // and applying the mutation:
            String operator = getOperator();
            if (operator != null) {
                Object mutatedElement = operators.get(operator).mutate(elementToMutate);
                insertElement(jsonNode, mutatedElement, propertyName, index); // Replace original element with mutated
                                                                              // element
                return true;
            }
        }
        return false;
    }

}
