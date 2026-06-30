package es.us.isa.httpmutator.core.headers.location;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.location.operator.LocationMutationOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;


public class LocationMutator extends AbstractMutator {
    public LocationMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.header.location.prob"));
        addOperatorIfEnabled(
                "operator.header.location.mutate.enabled",
                OperatorNames.MUTATE,
                LocationMutationOperator::new);
        addOperatorIfEnabled(
                "operator.header.location.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(LocationMutator.class));
    }
}
