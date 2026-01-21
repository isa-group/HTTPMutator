package es.us.isa.httpmutator.core.body.value.common.operator;

import com.fasterxml.jackson.databind.node.NullNode;

import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;
import static es.us.isa.httpmutator.core.util.Utilities.assignWeight;

/**
 * Operator that mutates an element by returning null
 *
 * @author Alberto Martin-Lopez
 */
public class NullOperator extends AbstractOperator {

    public NullOperator(Class classType) {
        super();
        weight = assignWeight(classType.getSimpleName(), OperatorNames.NULL);
    }
    
    @Override
    protected Object doMutate(Object elementObject) {
        return NullNode.instance;
    }
}
