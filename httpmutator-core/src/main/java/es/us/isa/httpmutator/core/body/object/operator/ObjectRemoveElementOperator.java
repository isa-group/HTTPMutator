package es.us.isa.httpmutator.core.body.object.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

/**
 * Operator that mutates an object by removing a number of properties from it.
 *
 * @author Alberto Martin-Lopez
 */
public class ObjectRemoveElementOperator extends AbstractOperator {

    private int maxRemovedProperties;     // Maximum number of properties to remove to the object
    private int minRemovedProperties;     // Minimum number of properties to remove to the object

    public ObjectRemoveElementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.object.weight." + OperatorNames.REMOVE_ELEMENT));
        maxRemovedProperties = Integer.parseInt(readProperty("operator.object.removedElements.max"));
        minRemovedProperties = Integer.parseInt(readProperty("operator.object.removedElements.min"));
    }

    public int getMaxRemovedProperties() {
        return maxRemovedProperties;
    }

    public void setMaxRemovedProperties(int maxRemovedProperties) {
        this.maxRemovedProperties = maxRemovedProperties;
    }

    public int getMinRemovedProperties() {
        return minRemovedProperties;
    }

    public void setMinRemovedProperties(int minRemovedProperties) {
        this.minRemovedProperties = minRemovedProperties;
    }

    @Override
    public boolean isApplicable(Object objectNodeObject) {
        return objectNodeObject instanceof ObjectNode && ((ObjectNode)objectNodeObject).size() > 0;
    }
    
    @Override
    protected Object doMutate(Object objectNodeObject) {
        ObjectNode objectNode = (ObjectNode)objectNodeObject;
        int randomValue;
        int removedProperties = rand1.nextInt(minRemovedProperties, maxRemovedProperties); // Remove between min and max properties to object
        List<String> propertyNames = Lists.newArrayList(objectNode.fieldNames());

        for (int i=1; i<=removedProperties; i++)
            if (objectNode.size() > 0) {
                randomValue = rand2.nextInt(propertyNames.size());
                objectNode.remove(propertyNames.get(randomValue)); // Remove a random property
                propertyNames.remove(randomValue); // Remove property from the list of properties to possibly remove
            }

        return objectNode;
    }
}
