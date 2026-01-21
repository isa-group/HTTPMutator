package es.us.isa.httpmutator.core.headers.mediaType;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.mediaType.operator.MediaTypeReplacementOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

public class MediaTypeMutator extends AbstractMutator {
    public MediaTypeMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.header.mediaType.prob"));
        operators.put(OperatorNames.REPLACE, new MediaTypeReplacementOperator());
        operators.put(OperatorNames.NULL, new NullOperator(MediaTypeMutator.class));
    }
}
