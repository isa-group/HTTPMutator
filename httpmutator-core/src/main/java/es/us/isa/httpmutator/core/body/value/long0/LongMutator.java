package es.us.isa.httpmutator.core.body.value.long0;


import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.body.value.long0.operator.LongMutationOperator;
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
        operators.put(OperatorNames.REPLACE, new LongReplacementOperator());
        // operators.put(OperatorNames.MUTATE, new LongMutationOperator());
        operators.put(OperatorNames.NULL, new NullOperator(Long.class));
        operators.put(OperatorNames.CHANGE_TYPE, new ChangeTypeOperator(Long.class));
    }
}
