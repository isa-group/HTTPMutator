package es.us.isa.httpmutator.core.body.array.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import com.fasterxml.jackson.databind.node.ArrayNode;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

/**
 * Operator that mutates an array by removing a number of elements from it.
 *
 * @author Alberto Martin-Lopez
 */
public class ArrayRemoveElementOperator extends AbstractOperator {

    private int maxRemovedElements;     // Maximum number of elements to remove from the array
    private int minRemovedElements;     // Minimum number of elements to remove from the array

    public ArrayRemoveElementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.array.weight." + OperatorNames.REMOVE_ELEMENT));
        maxRemovedElements = Integer.parseInt(readProperty("operator.array.removedElements.max"));
        minRemovedElements = Integer.parseInt(readProperty("operator.array.removedElements.min"));
    }

    public int getMaxRemovedElements() {
        return maxRemovedElements;
    }

    public void setMaxRemovedElements(int maxRemovedElements) {
        this.maxRemovedElements = maxRemovedElements;
    }

    public int getMinRemovedElements() {
        return minRemovedElements;
    }

    public void setMinRemovedElements(int minRemovedElements) {
        this.minRemovedElements = minRemovedElements;
    }

    @Override
    public boolean isApplicable(Object arrayNodeObject) {
        return arrayNodeObject instanceof ArrayNode && ((ArrayNode)arrayNodeObject).size() > maxRemovedElements;
    }

    @Override
    protected Object doMutate(Object arrayNodeObject) {
        ArrayNode arrayNode = (ArrayNode)arrayNodeObject;
        int removedElements = rand1.nextInt(minRemovedElements, maxRemovedElements); // Remove between min and max elements to array

        for (int i=1; i<=removedElements; i++)
            if (arrayNode.size() > 0)
                arrayNode.remove(rand2.nextInt(arrayNode.size())); // Remove a random element

        return arrayNode;
    }
}
