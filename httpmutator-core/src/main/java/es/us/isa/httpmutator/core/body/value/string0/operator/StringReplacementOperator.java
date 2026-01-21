package es.us.isa.httpmutator.core.body.value.string0.operator;

import java.util.Random;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import org.apache.commons.lang3.RandomStringUtils;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import es.us.isa.httpmutator.core.util.RandomUtils;

/**
 * Operator that mutates a string by completely replacing it.
 *
 * @author Alberto Martin-Lopez
 */
public class StringReplacementOperator extends AbstractOperator {

    private int minLength;      // Minimum length of the randomly generated string
    private int maxLength;      // Maximum length of the randomly generated string

    public StringReplacementOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.value.string.weight." + OperatorNames.REPLACE));
        minLength = Integer.parseInt(readProperty("operator.value.string.length.min"));
        maxLength = Integer.parseInt(readProperty("operator.value.string.length.max"));
    }

    @Override
    protected Object doMutate(Object stringObject) {
        if (Boolean.parseBoolean(readProperty("operator.value.string.includeAscii"))) {
            return RandomStringUtils.random(RandomUtils.nextInt(minLength, maxLength), 32, 127, false, false, null, RandomUtils.getRandom()); 
        }else {
            return RandomStringUtils.random(rand1.nextInt(minLength, maxLength),
                    0, 0,
                    Boolean.parseBoolean(readProperty("operator.value.string.includeLetters")),
                    Boolean.parseBoolean(readProperty("operator.value.string.includeNumbers")),
                    null, RandomUtils.getRandom());
        }
    }
}
