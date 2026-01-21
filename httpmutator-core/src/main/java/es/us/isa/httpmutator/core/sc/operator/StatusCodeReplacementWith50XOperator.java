package es.us.isa.httpmutator.core.sc.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;


public class StatusCodeReplacementWith50XOperator extends AbstractOperator {
    private final int[] SC50X = {500, 501, 502, 503, 504};

    public StatusCodeReplacementWith50XOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.sc.weight." + OperatorNames.REPLACE_WITH_50X));
    }

    @Override
    protected Object doMutate(Object statusCode) {
        int statusCodeInteger = (int)statusCode;
        int newStatusCode = SC50X[rand2.nextInt(SC50X.length)];
        while (newStatusCode == statusCodeInteger) {
            newStatusCode = SC50X[rand2.nextInt(SC50X.length)];
        }
        return newStatusCode;
    }

}
