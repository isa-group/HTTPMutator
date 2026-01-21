package es.us.isa.httpmutator.core.body.array.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

/**
 * Operator that mutates an array by disordering the elements in it.
 *
 * @author Alberto Martin-Lopez
 */
public class ArrayDisorderElementsOperator extends AbstractOperator {

    public ArrayDisorderElementsOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.array.weight." + OperatorNames.DISORDER_ELEMENTS));
    }

    @Override
    public boolean isApplicable(Object arrayNodeObject) {
        return arrayNodeObject instanceof ArrayNode && ((ArrayNode)arrayNodeObject).size() > 1;
    }

    @Override
    protected Object doMutate(Object arrayNodeObject) {
        ArrayNode arrayNode = (ArrayNode)arrayNodeObject;
        if (arrayNode.size() > 1) { // Apply this mutation only if array contains more than 1 element (doesn't make sense otherwise)
            int elementToDisorderIndex = rand1.nextInt(0, arrayNode.size() - 1);
            int whereToInsert;
            do whereToInsert = rand1.nextInt(0, arrayNode.size() - 1);
            while(whereToInsert == elementToDisorderIndex);
            JsonNode elementToDisorder = arrayNode.remove(elementToDisorderIndex); // Remove an element
            arrayNode.insert(whereToInsert, elementToDisorder); // Insert it elsewhere
        }

        return arrayNode;
    }
}
