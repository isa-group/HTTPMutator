package es.us.isa.httpmutator.core.body.value.string0.operator;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import es.us.isa.httpmutator.core.util.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Operator that mutates a string by replacing it with a boundary value, namely
 * an empty string, an uppercase string, a lowercase string, a string of minimum
 * length or a string of maximum length.
 *
 * @author Alberto Martin-Lopez
 */
public class StringBoundaryOperator extends AbstractOperator {

    private int minLength;          // Minimum length of the randomly generated string
    private int maxLength;          // Maximum length of the randomly generated string
    private String uppercaseString;
    private String lowercaseString;

    public StringBoundaryOperator() {
        super();
        weight = Float.parseFloat(readProperty("operator.value.string.weight." + OperatorNames.BOUNDARY));
        minLength = Integer.parseInt(readProperty("operator.value.string.length.min"));
        maxLength = Integer.parseInt(readProperty("operator.value.string.length.max"));
        uppercaseString = readProperty("operator.value.string.uppercase");
        lowercaseString = readProperty("operator.value.string.lowercase");
    }

    @Override
    protected Object doMutate(Object element) {
        String original = (String) element;
        float randomValue = rand2.nextFloat();

        if (randomValue <= 1f / 5) { // Empty string
            if (original.isEmpty()) {
                return " ";
            } else {
                return "";
            }
        } else if (randomValue <= 2f / 5) { // String of minLength
            return RandomStringUtils.random(minLength, 0, 0, true, true, null, RandomUtils.getRandom());
        } else if (randomValue <= 3f / 5) { // String of maxLength
            return RandomStringUtils.random(maxLength, 0, 0, true, true, null, RandomUtils.getRandom());
        } else if (randomValue <= 4f / 5) { // Lowercase string
            return lowercaseString;
        } else { // Uppercase string
            return uppercaseString;
        }
    }


}
