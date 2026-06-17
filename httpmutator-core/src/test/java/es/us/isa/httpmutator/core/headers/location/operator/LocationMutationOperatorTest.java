package es.us.isa.httpmutator.core.headers.location.operator;

import es.us.isa.httpmutator.core.util.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

public class LocationMutationOperatorTest {

    @Test
    public void mutatedLocationSuffixIsNeverNegative() {
        LocationMutationOperator operator = new LocationMutationOperator();

        for (long seed = 0; seed < 1000; seed++) {
            RandomUtils.setSeed(seed);
            String mutated = (String) operator.mutate("https://example.com/api");
            String suffix = mutated.substring(mutated.lastIndexOf('/') + 1);

            Assert.assertFalse("Location suffix should not be negative", suffix.startsWith("-"));
        }
    }
}
