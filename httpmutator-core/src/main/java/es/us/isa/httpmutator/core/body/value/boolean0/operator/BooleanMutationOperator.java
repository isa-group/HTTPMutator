package es.us.isa.httpmutator.core.body.value.boolean0.operator;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Operator that mutates a boolean by inverting its value
 *
 * @author Alberto Martin-Lopez
 */
public class BooleanMutationOperator extends AbstractOperator {

    public BooleanMutationOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.value.boolean.weight." + OperatorNames.MUTATE));
    }

    @Override
    protected Object doMutate(Object boolObject) {
        Boolean bool = (Boolean)boolObject;
        return !bool;
    }
}
