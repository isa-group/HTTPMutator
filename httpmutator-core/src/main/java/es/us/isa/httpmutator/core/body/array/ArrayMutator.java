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
        addOperatorIfEnabled(
                "operator.array.removeElement.enabled",
                OperatorNames.REMOVE_ELEMENT,
                ArrayRemoveElementOperator::new);
        addOperatorIfEnabled(
                "operator.array.empty.enabled",
                OperatorNames.EMPTY,
                ArrayEmptyOperator::new);
        addOperatorIfEnabled(
                "operator.array.addElement.enabled",
                OperatorNames.ADD_ELEMENT,
                ArrayAddElementOperator::new);
        addOperatorIfEnabled(
                "operator.array.disorderElements.enabled",
                OperatorNames.DISORDER_ELEMENTS,
                ArrayDisorderElementsOperator::new);
        addOperatorIfEnabled(
                "operator.array.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(ArrayNode.class));
        addOperatorIfEnabled(
                "operator.array.changeType.enabled",
                OperatorNames.CHANGE_TYPE,
                () -> new ChangeTypeOperator(ArrayNode.class));
    }

    public void resetFirstLevelOperators() {
        operators.remove(OperatorNames.NULL);
        operators.remove(OperatorNames.CHANGE_TYPE);
    }
}
