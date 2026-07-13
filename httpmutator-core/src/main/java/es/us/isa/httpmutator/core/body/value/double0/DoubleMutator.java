package es.us.isa.httpmutator.core.body.value.double0;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.body.value.double0.operator.DoubleReplacementOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Given a set of double mutation operators, the DoubleMutator selects one based
 * on their weights and returns the mutated double.
 *
 * @author Alberto Martin-Lopez
 */
public class DoubleMutator extends AbstractMutator {

    public DoubleMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.value.double.prob"));
        addOperatorIfEnabled(
                "operator.value.double.replace.enabled",
                OperatorNames.REPLACE,
                DoubleReplacementOperator::new);
        addOperatorIfEnabled(
                "operator.value.double.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(Double.class));
        addOperatorIfEnabled(
                "operator.value.double.changeType.enabled",
                OperatorNames.CHANGE_TYPE,
                () -> new ChangeTypeOperator(Double.class));
    }
}
