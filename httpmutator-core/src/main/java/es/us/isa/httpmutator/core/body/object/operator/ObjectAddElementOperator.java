package es.us.isa.httpmutator.core.body.object.operator;

import java.util.Random;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import es.us.isa.httpmutator.core.util.RandomUtils;

/**
 * Operator that mutates an object by adding a number of properties to it.
 *
 * @author Alberto Martin-Lopez
 */
public class ObjectAddElementOperator extends AbstractOperator {

    private long minLong;
    private long maxLong;
    private double minDouble;
    private double maxDouble;
    private int minLength;
    private int maxLength;
    private int maxAddedProperties;     // Maximum number of properties to add to the object
    private int minAddedProperties;     // Minimum number of properties to add to the object

    public ObjectAddElementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.object.weight." + OperatorNames.ADD_ELEMENT));
        maxAddedProperties = Integer.parseInt(readProperty("operator.object.addedElements.max"));
        minAddedProperties = Integer.parseInt(readProperty("operator.object.addedElements.min"));
        minLong = Long.parseLong(readProperty("operator.value.long.min"));
        maxLong = Long.parseLong(readProperty("operator.value.long.max"));
        minDouble = Double.parseDouble(readProperty("operator.value.double.min"));
        maxDouble = Double.parseDouble(readProperty("operator.value.double.max"));
        minLength = Integer.parseInt(readProperty("operator.value.string.length.min"));
        maxLength = Integer.parseInt(readProperty("operator.value.string.length.max"));
    }

    public int getMaxAddedProperties() {
        return maxAddedProperties;
    }

    public void setMaxAddedProperties(int maxAddedProperties) {
        this.maxAddedProperties = maxAddedProperties;
    }

    public int getMinAddedProperties() {
        return minAddedProperties;
    }

    public void setMinAddedProperties(int minAddedProperties) {
        this.minAddedProperties = minAddedProperties;
    }

    @Override
    protected Object doMutate(Object objectNodeObject) {
        ObjectNode objectNode = (ObjectNode)objectNodeObject;
        float randomValue;
        int addedProperties = rand1.nextInt(minAddedProperties, maxAddedProperties); // Add between min and max properties to object

        for (int i=1; i<=addedProperties; i++) {
            randomValue = rand2.nextFloat();
            if (randomValue <= 1f/7) {
                objectNode.put("randomLong"+i, rand1.nextLong(minLong, maxLong));
            } else if (randomValue <= 2f/7) {
                objectNode.put("randomFloat"+i, rand1.nextUniform(minDouble, maxDouble));
            } else if (randomValue <= 3f/7) {
                objectNode.put("randomBoolean"+i, rand2.nextBoolean());
            } else if (randomValue <= 4f/7) {
                objectNode.put("randomString"+i, RandomStringUtils.random(rand1.nextInt(minLength, maxLength), 0, 0, true, true, null, RandomUtils.getRandom()));
            } else if (randomValue <= 5f/7) {
                objectNode.putNull("randomNull"+i);
            } else if (randomValue <= 6f/7) {
                objectNode.putObject("randomObject"+i);
            } else {
                objectNode.putArray("randomArray"+i);
            }
        }

        return objectNode;
    }
}
