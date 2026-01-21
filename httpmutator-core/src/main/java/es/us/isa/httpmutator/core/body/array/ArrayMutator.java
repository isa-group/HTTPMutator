package es.us.isa.httpmutator.core.body.array;

import com.fasterxml.jackson.databind.node.ArrayNode;

import es.us.isa.httpmutator.core.body.AbstractObjectOrArrayMutator;
import es.us.isa.httpmutator.core.body.array.operator.ArrayAddElementOperator;
import es.us.isa.httpmutator.core.body.array.operator.ArrayDisorderElementsOperator;
import es.us.isa.httpmutator.core.body.array.operator.ArrayEmptyOperator;
import es.us.isa.httpmutator.core.body.array.operator.ArrayRemoveElementOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

/**
 * Given a set of array mutation operators, the ArrayMutator selects one based
 * on their weights and returns the mutated array.
 *
 * @author Alberto Martin-Lopez
 */
public class ArrayMutator extends AbstractObjectOrArrayMutator {

    public ArrayMutator() {
        super();
    }

    public void resetOperators() {
        operators.clear();
        operators.put(OperatorNames.REMOVE_ELEMENT, new ArrayRemoveElementOperator());
        operators.put(OperatorNames.EMPTY, new ArrayEmptyOperator());
        operators.put(OperatorNames.ADD_ELEMENT, new ArrayAddElementOperator());
        operators.put(OperatorNames.DISORDER_ELEMENTS, new ArrayDisorderElementsOperator());
        operators.put(OperatorNames.NULL, new NullOperator(ArrayNode.class));
        operators.put(OperatorNames.CHANGE_TYPE, new ChangeTypeOperator(ArrayNode.class));
    }

    public void resetFirstLevelOperators() {
        operators.remove(OperatorNames.NULL);
        operators.remove(OperatorNames.CHANGE_TYPE);
    }
}
