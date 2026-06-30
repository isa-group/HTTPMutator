package es.us.isa.httpmutator.core.body.value.boolean0;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.boolean0.operator.BooleanMutationOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Given a set of boolean mutation operators, the BooleanMutator selects one based
 * on their weights and returns the mutated boolean.
 *
 * @author Alberto Martin-Lopez
 */
public class BooleanMutator extends AbstractMutator {

    public BooleanMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.value.boolean.prob"));
        addOperatorIfEnabled(
                "operator.value.boolean.mutate.enabled",
                OperatorNames.MUTATE,
                BooleanMutationOperator::new);
        addOperatorIfEnabled(
                "operator.value.boolean.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(Boolean.class));
        addOperatorIfEnabled(
                "operator.value.boolean.changeType.enabled",
                OperatorNames.CHANGE_TYPE,
                () -> new ChangeTypeOperator(Boolean.class));
    }
}
