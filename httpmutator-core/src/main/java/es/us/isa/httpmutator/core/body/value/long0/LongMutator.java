package es.us.isa.httpmutator.core.body.value.long0;


import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.body.value.long0.operator.LongReplacementOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Given a set of long mutation operators, the LongMutator selects one based
 * on their weights and returns the mutated long.
 *
 * @author Alberto Martin-Lopez
 */
public class LongMutator extends AbstractMutator {

    public LongMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.value.long.prob"));
        addOperatorIfEnabled(
                "operator.value.long.replace.enabled",
                OperatorNames.REPLACE,
                LongReplacementOperator::new);
        addOperatorIfEnabled(
                "operator.value.long.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(Long.class));
        addOperatorIfEnabled(
                "operator.value.long.changeType.enabled",
                OperatorNames.CHANGE_TYPE,
                () -> new ChangeTypeOperator(Long.class));
    }
}
