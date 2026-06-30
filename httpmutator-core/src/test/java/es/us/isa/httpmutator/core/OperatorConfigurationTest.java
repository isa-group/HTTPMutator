package es.us.isa.httpmutator.core;

import es.us.isa.httpmutator.core.util.PropertyManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class OperatorConfigurationTest {

    @After
    public void resetProperties() {
        PropertyManager.resetProperties();
    }

    @Test
    public void missingOperatorEnabledPropertyDefaultsToEnabled() {
        TestMutator mutator = new TestMutator();

        mutator.register("operator.test.missing.enabled", new AtomicBoolean());

        Assert.assertTrue(mutator.getOperators().containsKey("test"));
    }

    @Test
    public void disabledOperatorIsNotInstantiatedOrRegistered() {
        PropertyManager.setProperty("operator.test.disabled.enabled", "false");
        AtomicBoolean instantiated = new AtomicBoolean();
        TestMutator mutator = new TestMutator();

        mutator.register("operator.test.disabled.enabled", instantiated);

        Assert.assertFalse(instantiated.get());
        Assert.assertFalse(mutator.getOperators().containsKey("test"));
    }

    @Test
    public void invalidOperatorEnabledPropertyIsRejected() {
        PropertyManager.setProperty("operator.test.invalid.enabled", "sometimes");
        TestMutator mutator = new TestMutator();

        try {
            mutator.register("operator.test.invalid.enabled", new AtomicBoolean());
            Assert.fail("Expected invalid boolean property to be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("operator.test.invalid.enabled"));
        }
    }

    private static final class TestMutator extends AbstractMutator {
        void register(String propertyName, AtomicBoolean instantiated) {
            addOperatorIfEnabled(propertyName, "test", () -> {
                instantiated.set(true);
                return new TestOperator();
            });
        }
    }

    private static final class TestOperator extends AbstractOperator {
        @Override
        protected Object doMutate(Object element) {
            return element;
        }
    }
}
