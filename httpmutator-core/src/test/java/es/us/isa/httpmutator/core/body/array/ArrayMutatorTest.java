package es.us.isa.httpmutator.core.body.array;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import es.us.isa.httpmutator.core.AbstractOperator;

@RunWith(JUnit4.class)
public class ArrayMutatorTest {
    private ArrayMutator arrayMutator;
    private JsonNodeFactory nodeFactory;

    @Before
    public void setUp() {
        arrayMutator = new ArrayMutator();
        arrayMutator.resetOperators();
        nodeFactory = JsonNodeFactory.instance;
    }

    @Test
    public void testOperatorsOnEmptyArray() {
        System.out.println("Testing operators on an empty array:");
        System.out.println("=========================================");
        // Create an empty array node
        ArrayNode emptyArray = nodeFactory.arrayNode();

        for (AbstractOperator operator : arrayMutator.getOperators().values()) {
            if (operator.isApplicable(emptyArray)) {
                System.out.println("applying operator: " + operator.getClass().getSimpleName() + " the result is: " + operator.mutate(emptyArray.deepCopy()));
            } else {
                // If not applicable, skip this operator
                System.out.println("Operator " + operator.getClass().getSimpleName() + " is not applicable to an empty array.");
            }
           
        }
    }

    @Test
    public void testOperatorsOnSingleElementArray() {
        // Create an array node with a single element
        System.out.println("Testing operators on a single element array:");
        System.out.println("=========================================");
        ArrayNode singleElementArray = nodeFactory.arrayNode();
        singleElementArray.add(nodeFactory.textNode("test"));

        for (AbstractOperator operator : arrayMutator.getOperators().values()) {
            if (operator.isApplicable(singleElementArray)) {
                System.out.println("applying operator: " + operator.getClass().getSimpleName() + " the result is: " + operator.mutate(singleElementArray.deepCopy()));
            } else {
                // If not applicable, skip this operator
                System.out.println("Operator " + operator.getClass().getSimpleName() + " is not applicable to a single element array.");    
            }
        }
    }
}
