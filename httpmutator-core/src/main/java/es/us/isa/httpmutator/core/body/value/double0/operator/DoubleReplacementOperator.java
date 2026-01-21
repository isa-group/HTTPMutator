package es.us.isa.httpmutator.core.body.value.double0.operator;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Operator that mutates a double by completely replacing it.
 *
 * @author Alberto Martin-Lopez
 */
public class DoubleReplacementOperator extends AbstractOperator {

    private double minDouble;
    private double maxDouble;

    public DoubleReplacementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.value.double.weight." + OperatorNames.REPLACE));
        minDouble = Double.parseDouble(readProperty("operator.value.double.min"));
        maxDouble = Double.parseDouble(readProperty("operator.value.double.max"));
    }

    @Override
    protected Object doMutate(Object longObject) {
        return rand1.nextUniform(minDouble, maxDouble);
    }
}
