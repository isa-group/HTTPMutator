package es.us.isa.httpmutator.core.body.array.operator;

import java.util.Random;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import es.us.isa.httpmutator.core.util.RandomUtils;

/**
 * Operator that mutates an array by adding a number of elements to it.
 *
 * @author Alberto Martin-Lopez
 */
public class ArrayAddElementOperator extends AbstractOperator {

    private long minLong;
    private long maxLong;
    private double minDouble;
    private double maxDouble;
    private int minLength;
    private int maxLength;
    private int maxAddedElements;     // Maximum number of elements to add to the array
    private int minAddedElements;     // Minimum number of elements to add to the array

    public ArrayAddElementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.array.weight." + OperatorNames.ADD_ELEMENT));
        maxAddedElements = Integer.parseInt(readProperty("operator.array.addedElements.max"));
        minAddedElements = Integer.parseInt(readProperty("operator.array.addedElements.min"));
        minLong = Long.parseLong(readProperty("operator.value.long.min"));
        maxLong = Long.parseLong(readProperty("operator.value.long.max"));
        minDouble = Double.parseDouble(readProperty("operator.value.double.min"));
        maxDouble = Double.parseDouble(readProperty("operator.value.double.max"));
        minLength = Integer.parseInt(readProperty("operator.value.string.length.min"));
        maxLength = Integer.parseInt(readProperty("operator.value.string.length.max"));
    }

    public int getMaxAddedElements() {
        return maxAddedElements;
    }

    public void setMaxAddedElements(int maxAddedElements) {
        this.maxAddedElements = maxAddedElements;
    }

    public int getMinAddedElements() {
        return minAddedElements;
    }

    public void setMinAddedElements(int minAddedElements) {
        this.minAddedElements = minAddedElements;
    }

    @Override
    protected Object doMutate(Object arrayNodeObject) {
        ArrayNode arrayNode = (ArrayNode)arrayNodeObject;
        float randomValue;
        int addedProperties = rand1.nextInt(minAddedElements, maxAddedElements); // Add between min and max elements to array

        for (int i=1; i<=addedProperties; i++) {
            randomValue = rand2.nextFloat();
            if (randomValue <= 1f/7) {
                arrayNode.add(rand1.nextLong(minLong, maxLong));
            } else if (randomValue <= 2f/7) {
                arrayNode.add(rand1.nextUniform(minDouble, maxDouble));
            } else if (randomValue <= 3f/7) {
                arrayNode.add(rand2.nextBoolean());
            } else if (randomValue <= 4f/7) {
                arrayNode.add(RandomStringUtils.random(rand1.nextInt(minLength, maxLength), 0, 0, true, true, null, RandomUtils.getRandom()));
            } else if (randomValue <= 5f/7) {
                arrayNode.addNull();
            } else if (randomValue <= 6f/7) {
                arrayNode.addObject();
            } else {
                arrayNode.addArray();
            }
        }

        return arrayNode;
    }
}
