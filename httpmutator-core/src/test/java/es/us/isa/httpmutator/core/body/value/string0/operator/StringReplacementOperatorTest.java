package es.us.isa.httpmutator.core.body.value.string0.operator;

import es.us.isa.httpmutator.core.util.PropertyManager;
import es.us.isa.httpmutator.core.util.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class StringReplacementOperatorTest {

    @After
    public void resetProperties() {
        PropertyManager.resetProperties();
    }

    @Test
    public void asciiReplacementLengthCanReachConfiguredMaximum() {
        PropertyManager.setProperty("operator.value.string.includeAscii", "true");
        StringReplacementOperator operator = new StringReplacementOperator();

        Assert.assertTrue("ASCII replacement should be able to generate max length",
                canGenerateLength(operator, 10));
    }

    @Test
    public void nonAsciiReplacementLengthCanReachConfiguredMaximum() {
        PropertyManager.setProperty("operator.value.string.includeAscii", "false");
        StringReplacementOperator operator = new StringReplacementOperator();

        Assert.assertTrue("Non-ASCII replacement should be able to generate max length",
                canGenerateLength(operator, 10));
    }

    @Test
    public void replacementLengthStaysWithinConfiguredBounds() {
        StringReplacementOperator operator = new StringReplacementOperator();

        for (long seed = 0; seed < 1000; seed++) {
            RandomUtils.setSeed(seed);
            String replacement = (String) operator.mutate("original");
            Assert.assertTrue("Length must be at least configured min", replacement.length() >= 1);
            Assert.assertTrue("Length must be at most configured max", replacement.length() <= 10);
        }
    }

    private boolean canGenerateLength(StringReplacementOperator operator, int expectedLength) {
        for (long seed = 0; seed < 5000; seed++) {
            RandomUtils.setSeed(seed);
            String replacement = (String) operator.mutate("original");
            if (replacement.length() == expectedLength) {
                return true;
            }
        }
        return false;
    }
}
