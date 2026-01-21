package es.us.isa.httpmutator.core.body.array.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import com.fasterxml.jackson.databind.node.ArrayNode;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

/**
 * Operator that converts an array to empty by removing all elements from it.
 *
 * @author Ana Belén Sánchez
 */
public class ArrayEmptyOperator extends AbstractOperator {
	    
	public ArrayEmptyOperator() {
		super();
	    weight = Float.parseFloat(readProperty("operator.array.weight." + OperatorNames.EMPTY));
	}

	@Override
	public boolean isApplicable(Object arrayNodeObject) {
		return arrayNodeObject instanceof ArrayNode && ((ArrayNode)arrayNodeObject).size() > 0;
	}

	@Override
	protected Object doMutate(Object arrayNodeObject) {
	    ArrayNode arrayNode = (ArrayNode)arrayNodeObject;

	    if (arrayNode.size() > 0)
	    	arrayNode.removeAll(); // Remove all elements in the array
	    
	    return arrayNode;
	    }
}
