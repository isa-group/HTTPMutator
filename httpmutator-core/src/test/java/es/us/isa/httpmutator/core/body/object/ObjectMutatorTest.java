package es.us.isa.httpmutator.core.body.object;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.us.isa.httpmutator.core.AbstractOperator;

@RunWith(JUnit4.class)
public class ObjectMutatorTest {
    private ObjectMutator objectMutator;
    private JsonNodeFactory nodeFactory;

    @Before
    public void setUp() {
        objectMutator = new ObjectMutator();
        objectMutator.resetOperators();
        nodeFactory = JsonNodeFactory.instance;
    }

    @Test
    public void testOperatorsOnEmptyObject() {
        System.out.println("Testing operators on an empty object:");
        System.out.println("=========================================");
        // Create an empty object node
        ObjectNode emptyObject = nodeFactory.objectNode();

        for (AbstractOperator operator : objectMutator.getOperators().values()) {
            if (operator.isApplicable(emptyObject)) {
                System.out.println("applying operator: " + operator.getClass().getSimpleName() + " the result is: " + operator.mutate(emptyObject.deepCopy()));
            } else {
                // If not applicable, skip this operator
                System.out.println("Operator " + operator.getClass().getSimpleName() + " is not applicable to an empty object.");
            }                    
        }
    }

    @Test
    public void testOperatorsOnSinglePropertyObject() {
        // Create an object node with a single property
        System.out.println("Testing operators on a single property object:");
        System.out.println("=========================================");
        ObjectNode singlePropertyObject = nodeFactory.objectNode();
        singlePropertyObject.put("testKey", "testValue");

        for (AbstractOperator operator : objectMutator.getOperators().values()) {
            if (operator.isApplicable(singlePropertyObject)) {
                System.out.println("applying operator: " + operator.getClass().getSimpleName() + " the result is: " + operator.mutate(singlePropertyObject.deepCopy()));
            } else {
                // If not applicable, skip this operator
                System.out.println("Operator " + operator.getClass().getSimpleName() + " is not applicable to a single property object.");
            }
        }
    }

    @Test
    public void testOperatorsOnMultiplePropertyObject() {
        // Create an object node with multiple properties
        System.out.println("Testing operators on a multiple property object:");
        System.out.println("=========================================");
        ObjectNode multiplePropertyObject = nodeFactory.objectNode();
        multiplePropertyObject.put("key1", "value1");
        multiplePropertyObject.put("key2", 42);
        multiplePropertyObject.put("key3", true);

        for (AbstractOperator operator : objectMutator.getOperators().values()) {
            if (operator.isApplicable(multiplePropertyObject)) {
                System.out.println("applying operator: " + operator.getClass().getSimpleName() + " the result is: " + operator.mutate(multiplePropertyObject.deepCopy()));
            } else {
                // If not applicable, skip this operator
                System.out.println("Operator " + operator.getClass().getSimpleName() + " is not applicable to a multiple property object.");
            }
        }
    }

    @Test
    public void testOperatorsOnNestedObject() {
        // Create an object node with nested objects and arrays
        System.out.println("Testing operators on a nested object:");
        System.out.println("=========================================");
        ObjectNode nestedObject = nodeFactory.objectNode();
        nestedObject.put("simpleProperty", "value");
        
        ObjectNode innerObject = nodeFactory.objectNode();
        innerObject.put("innerKey", "innerValue");
        nestedObject.set("nestedObject", innerObject);
        
        nestedObject.set("nestedArray", nodeFactory.arrayNode().add("arrayElement"));

        for (AbstractOperator operator : objectMutator.getOperators().values()) {
            if (operator.isApplicable(nestedObject)) {
                System.out.println("applying operator: " + operator.getClass().getSimpleName() + " the result is: " + operator.mutate(nestedObject.deepCopy()));
            } else {
                // If not applicable, skip this operator
                System.out.println("Operator " + operator.getClass().getSimpleName() + " is not applicable to a nested object.");
            }
        }
    }
}