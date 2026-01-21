package es.us.isa.httpmutator.core.headers.mediaType.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class MediaTypeReplacementOperator extends AbstractOperator {
    private static final String[] CT_STRINGS = {"application/json", "application/xml", "text/plain", "text/html", "text/css", "text/javascript", "application/x-www-form-urlencoded"};

    public MediaTypeReplacementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.header.mediaType.weight." + OperatorNames.REPLACE));
    }

    @Override
    protected  Object doMutate(Object stringObject) {
        String mediaType = (String) stringObject;
        String newMediaType = CT_STRINGS[rand2.nextInt(CT_STRINGS.length)];
        while (newMediaType.equalsIgnoreCase(mediaType)) {
            newMediaType = CT_STRINGS[rand2.nextInt(CT_STRINGS.length)];
        }
        return newMediaType;
    }
}
