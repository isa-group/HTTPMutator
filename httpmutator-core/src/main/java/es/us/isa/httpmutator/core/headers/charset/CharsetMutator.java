package es.us.isa.httpmutator.core.headers.charset;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.charset.operator.CharsetReplacementOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class CharsetMutator extends AbstractMutator {
    public CharsetMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.header.charset.prob"));
        addOperatorIfEnabled(
                "operator.header.charset.replace.enabled",
                OperatorNames.REPLACE,
                CharsetReplacementOperator::new);
        addOperatorIfEnabled(
                "operator.header.charset.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(CharsetMutator.class));
    }

}
