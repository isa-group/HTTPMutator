package es.us.isa.httpmutator.core.body.value.string0.operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Operator that mutates a string by adding special characters like "/", "*", and ",".
 *
 * @author Ana Belén Sánchez
 */
public class StringAddSpecialCharactersMutationOperator extends AbstractOperator {
    private static final List<String> SPECIAL_CHARACTERS = Arrays.asList(
            "/", "*", ",", "´", "´*", "/*", "/,", "*,", "´/", "´,"
    );

    public StringAddSpecialCharactersMutationOperator() {
    	 super();
         weight = Float.parseFloat(readProperty("operator.value.string.weight." + OperatorNames.ADD_SPECIAL_CHARACTERS));
     }

     @Override
     protected Object doMutate(Object stringObject) {
         String string = (String) stringObject;
         StringBuilder sb = new StringBuilder(string);

         int length = string.length();

         // For empty string, always insert at position 0
         int charPosition;
         if (length == 0) {
             charPosition = 0;
         } else {
             // RandomDataGenerator.nextInt(lower, upper) is inclusive on both ends
             // So to insert safely, upper bound should be `length` (not length-1)
             charPosition = rand1.nextInt(0, length);
         }

         int posRandomCharacter = rand1.nextInt(0, SPECIAL_CHARACTERS.size() - 1);

         sb.insert(charPosition, SPECIAL_CHARACTERS.get(posRandomCharacter));

         return sb.toString();
     }
 } 