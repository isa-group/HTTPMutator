package es.us.isa.httpmutator.core.body.object;

import com.fasterxml.jackson.databind.node.ObjectNode;

import es.us.isa.httpmutator.core.body.AbstractObjectOrArrayMutator;
import es.us.isa.httpmutator.core.body.object.operator.ObjectAddElementOperator;
import es.us.isa.httpmutator.core.body.object.operator.ObjectRemoveElementOperator;
import es.us.isa.httpmutator.core.body.object.operator.ObjectRemoveObjectTypeElementOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

/**
 * Given a set of object mutation operators, the ObjectMutator selects one based
 * on their weights and returns the mutated object.
 *
 * @author Alberto Martin-Lopez
 */
public class ObjectMutator extends AbstractObjectOrArrayMutator {

    public ObjectMutator() {
        super();
    }

    public void resetOperators() {
        operators.clear();
        addOperatorIfEnabled(
                "operator.object.removeElement.enabled",
                OperatorNames.REMOVE_ELEMENT,
                ObjectRemoveElementOperator::new);
        addOperatorIfEnabled(
                "operator.object.removeObjectElement.enabled",
                OperatorNames.REMOVE_OBJECT_ELEMENT,
                ObjectRemoveObjectTypeElementOperator::new);
        addOperatorIfEnabled(
                "operator.object.addElement.enabled",
                OperatorNames.ADD_ELEMENT,
                ObjectAddElementOperator::new);
        addOperatorIfEnabled(
                "operator.object.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(ObjectNode.class));
        addOperatorIfEnabled(
                "operator.object.changeType.enabled",
                OperatorNames.CHANGE_TYPE,
                () -> new ChangeTypeOperator(ObjectNode.class));
    }

    public void resetFirstLevelOperators() {
        operators.remove(OperatorNames.NULL);
        operators.remove(OperatorNames.CHANGE_TYPE);
    }
}
