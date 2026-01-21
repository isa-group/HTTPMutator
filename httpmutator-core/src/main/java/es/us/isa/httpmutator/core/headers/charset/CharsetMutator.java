package es.us.isa.httpmutator.core.headers.charset;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.charset.operator.CharsetReplacementOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class CharsetMutator extends AbstractMutator {
    public CharsetMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.sc.prob"));
        operators.put(OperatorNames.REPLACE, new CharsetReplacementOperator());
        operators.put(OperatorNames.NULL, new NullOperator(CharsetMutator.class));
    }

}
