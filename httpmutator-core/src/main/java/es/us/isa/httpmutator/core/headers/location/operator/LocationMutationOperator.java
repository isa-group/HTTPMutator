package es.us.isa.httpmutator.core.headers.location.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import java.net.URI;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class LocationMutationOperator extends AbstractOperator {

    public LocationMutationOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.header.location.weight." + OperatorNames.MUTATE));
    }

    @Override
    protected Object doMutate(Object location) {
        String locationString = (String) location;
        URI originalUri = URI.create(locationString);
        String newPath = originalUri.getPath() + "/" + System.currentTimeMillis();
        return URI.create(
            originalUri.getScheme() + "://" +
            originalUri.getAuthority() +
            newPath +
            (originalUri.getQuery() != null ? "?" + originalUri.getQuery() : "") +
            (originalUri.getFragment() != null ? "#" + originalUri.getFragment() : "")
        ).toString();        
    }
}
