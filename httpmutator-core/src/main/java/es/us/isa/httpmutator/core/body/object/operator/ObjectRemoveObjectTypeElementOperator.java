package es.us.isa.httpmutator.core.body.object.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Operator that mutates an object by removing a number of object-type
 * properties from it.
 *
 * @author Ana Belén Sánchez
 */
public class ObjectRemoveObjectTypeElementOperator extends AbstractOperator {
    private static final Logger logger = LogManager.getLogger(ObjectRemoveObjectTypeElementOperator.class.getName());

    private int maxRemovedProperties; // Maximum number of object-type properties to remove to the object
    private int minRemovedProperties; // Minimum number of object-type properties to remove to the object

    public ObjectRemoveObjectTypeElementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.object.weight." + OperatorNames.REMOVE_OBJECT_ELEMENT));
        maxRemovedProperties = Integer.parseInt(readProperty("operator.object.removeObjectElement.max"));
        minRemovedProperties = Integer.parseInt(readProperty("operator.object.removeObjectElement.min"));
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
        if (!(objectNodeObject instanceof ObjectNode)) {
            return false;
        }

        ObjectNode objectNode = (ObjectNode) objectNodeObject;
        if (objectNode.size() == 0) {
            return false;
        }

        // Check if at least one property is of type ObjectNode
        Iterator<Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            if (entry.getValue().isObject()) {
                // logger.debug("Object has object-type properties: {}", entry.getKey());
                return true;
            }
        }

        return false;
    }

    @Override
    protected Object doMutate(Object objectNodeObject) {
        // logger.debug("Mutating object by removing object-type properties: {}", objectNodeObject);
        ObjectNode objectNode = (ObjectNode) objectNodeObject;
        int randomProperty;
        int removedProperties = rand1.nextInt(minRemovedProperties, maxRemovedProperties); // Remove between min and max
                                                                                           // properties to object
        List<String> objectPropertyNames = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> it = objectNode.fields();
        Entry<String, JsonNode> property;

        while (it.hasNext()) { // Identify the object-type properties
            property = it.next();
            if (property.getValue().isObject()) {
                objectPropertyNames = Lists.newArrayList(property.getKey());
            }
        }

        for (int i = 1; i <= removedProperties; i++) {
            if (!objectPropertyNames.isEmpty()) { // If there are object-type properties identified
                randomProperty = rand2.nextInt(objectPropertyNames.size());
                objectNode.remove(objectPropertyNames.get(randomProperty)); // Remove a random object-type property
                objectPropertyNames.remove(randomProperty);
            }
        }
        return objectNode;
    }
}
