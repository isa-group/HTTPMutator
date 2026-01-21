package es.us.isa.httpmutator.core.headers.charset.operator;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class CharsetReplacementOperator extends AbstractOperator {
    private final String[] CHARSET_VALUES = {
        "UTF-8", 
        "utf-8", 
        "UTF8", 
        "utf8", 
        "UTF-16", 
        "utf-16", 
        "UTF16", 
        "utf16", 
        "UTF-32", 
        "utf-32", 
        "UTF32", 
        "utf32",
        "ISO-8859-15",
        "iso-8859-15",
        "Latin-9",
        "latin9"
    };
    
    public CharsetReplacementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.header.charset.weight." + OperatorNames.REPLACE));;
    }

    @Override
    protected Object doMutate(Object charset) {
        if (charset == null) {
            return CHARSET_VALUES[rand2.nextInt(CHARSET_VALUES.length)];
        }
        
        String charsetString = (String) charset;
        String newCharsetString = CHARSET_VALUES[rand2.nextInt(CHARSET_VALUES.length)];
        while (charsetString.equals(newCharsetString)) {
            newCharsetString = CHARSET_VALUES[rand2.nextInt(CHARSET_VALUES.length)];
        }
        return newCharsetString;
    }
}
