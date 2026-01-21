package es.us.isa.httpmutator.core.body.value.long0.operator;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Operator that mutates a long by adding or subtracting a delta number
 * to the original number
 *
 * @author Alberto Martin-Lopez
 */
public class LongMutationOperator extends AbstractOperator {

    private long delta;

    public LongMutationOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.value.long.weight." + OperatorNames.MUTATE));
        delta = Long.parseLong(readProperty("operator.value.long.delta"));
    }

    protected Object doMutate(Object longObject) {
        Long longValue = (Long)longObject;
        float randomValue = rand2.nextFloat();

        if (randomValue <= 1f/2) { // Mutation: subtract delta
            return longValue + delta;
        } else { // Mutation: add delta
            return longValue - delta;
        }
    }
}
