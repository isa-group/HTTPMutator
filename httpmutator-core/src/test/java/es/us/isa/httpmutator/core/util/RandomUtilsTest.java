package es.us.isa.httpmutator.core.util;

import org.junit.Assert;
import org.junit.Test;

public class RandomUtilsTest {

    @Test
    public void nextIntInclusiveReturnsOnlyValueWhenBoundsMatch() {
        RandomUtils.setSeed(42L);

        Assert.assertEquals(1, RandomUtils.nextIntInclusive(1, 1));
    }

    @Test
    public void nextIntInclusiveIncludesBothBounds() {
        boolean sawLower = false;
        boolean sawUpper = false;

        for (long seed = 0; seed < 1000 && !(sawLower && sawUpper); seed++) {
            RandomUtils.setSeed(seed);
            int value = RandomUtils.nextIntInclusive(1, 2);
            Assert.assertTrue("Value must stay within inclusive range", value == 1 || value == 2);
            sawLower |= value == 1;
            sawUpper |= value == 2;
        }

        Assert.assertTrue("Lower bound should be reachable", sawLower);
        Assert.assertTrue("Upper bound should be reachable", sawUpper);
    }

    @Test
    public void nextLongInclusiveReturnsOnlyValueWhenBoundsMatch() {
        RandomUtils.setSeed(42L);

        Assert.assertEquals(5L, RandomUtils.nextLongInclusive(5L, 5L));
    }

    @Test
    public void nextLongInclusiveSupportsLongMaxValueSingleton() {
        RandomUtils.setSeed(42L);

        Assert.assertEquals(Long.MAX_VALUE, RandomUtils.nextLongInclusive(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void nextLongInclusiveSupportsRangeAdjacentToLongMaxValue() {
        RandomUtils.setSeed(42L);

        long value = RandomUtils.nextLongInclusive(Long.MAX_VALUE - 1L, Long.MAX_VALUE);

        Assert.assertTrue(value == Long.MAX_VALUE - 1L || value == Long.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nextIntInclusiveRejectsInvertedBounds() {
        RandomUtils.nextIntInclusive(2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nextLongInclusiveRejectsInvertedBounds() {
        RandomUtils.nextLongInclusive(2L, 1L);
    }
}
