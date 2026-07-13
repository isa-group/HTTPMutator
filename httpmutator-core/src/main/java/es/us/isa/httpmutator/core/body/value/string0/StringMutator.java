package es.us.isa.httpmutator.core.body.value.string0;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.body.value.string0.operator.StringAddSpecialCharactersMutationOperator;
import es.us.isa.httpmutator.core.body.value.string0.operator.StringBoundaryOperator;
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
        addOperatorIfEnabled(
                "operator.value.string.replace.enabled",
                OperatorNames.REPLACE,
                StringReplacementOperator::new);
        addOperatorIfEnabled(
                "operator.value.string.addSpecialCharacters.enabled",
                OperatorNames.ADD_SPECIAL_CHARACTERS,
                StringAddSpecialCharactersMutationOperator::new);
        addOperatorIfEnabled(
                "operator.value.string.boundary.enabled",
                OperatorNames.BOUNDARY,
                StringBoundaryOperator::new);
        addOperatorIfEnabled(
                "operator.value.string.null.enabled",
                OperatorNames.NULL,
                () -> new NullOperator(String.class));
        addOperatorIfEnabled(
                "operator.value.string.changeType.enabled",
                OperatorNames.CHANGE_TYPE,
                () -> new ChangeTypeOperator(String.class));
    }
}
