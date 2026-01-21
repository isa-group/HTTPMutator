package es.us.isa.httpmutator.core.sc.operator;


import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;


/**
 * Operator that changes the status code class of an HTTP response by changing it to a different group: 20X group, 40X group, and 500 group.
 */
public class StatusCodeReplacementWith20XOperator extends AbstractOperator {
    private final int[] SC20X = {200, 201, 202, 204};

    public StatusCodeReplacementWith20XOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.sc.weight." + OperatorNames.REPLACE_WITH_20X));
    }

    @Override
    protected Object doMutate(Object statusCode) {
        int newStatusCode = SC20X[rand2.nextInt(SC20X.length)];
        while (newStatusCode == (int) statusCode) {
            newStatusCode = SC20X[rand2.nextInt(SC20X.length)];
        }
        return newStatusCode;
    }
}
