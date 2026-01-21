package es.us.isa.httpmutator.core.body.value.string0;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.body.value.string0.operator.StringAddSpecialCharactersMutationOperator;
import es.us.isa.httpmutator.core.body.value.string0.operator.StringBoundaryOperator;
import es.us.isa.httpmutator.core.body.value.string0.operator.StringMutationOperator;
import es.us.isa.httpmutator.core.body.value.string0.operator.StringReplacementOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

/**
 * Given a set of string mutation operators, the StringMutator selects one based
 * on their weights and returns the mutated string.
 *
 * @author Alberto Martin-Lopez
 */
public class StringMutator extends AbstractMutator {

    public StringMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.value.string.prob"));
        operators.put(OperatorNames.REPLACE, new StringReplacementOperator());
        operators.put(OperatorNames.ADD_SPECIAL_CHARACTERS, new StringAddSpecialCharactersMutationOperator());
//        operators.put(OperatorNames.MUTATE, new StringMutationOperator());
        operators.put(OperatorNames.BOUNDARY, new StringBoundaryOperator());
        operators.put(OperatorNames.NULL, new NullOperator(String.class));
        operators.put(OperatorNames.CHANGE_TYPE, new ChangeTypeOperator(String.class));
    }
}
