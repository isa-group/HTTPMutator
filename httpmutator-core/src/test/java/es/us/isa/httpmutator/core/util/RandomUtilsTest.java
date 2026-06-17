package es.us.isa.httpmutator.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class RandomUtilsTest {

    @Test
    public void defaultSeedIs42BeforeExplicitSeeding() throws Exception {
        URL[] classpath = toUrls(System.getProperty("java.class.path").split(File.pathSeparator));

        try (URLClassLoader loader = new URLClassLoader(classpath, null)) {
            Class<?> randomUtilsClass = Class.forName(RandomUtils.class.getName(), true, loader);
            Object seed = randomUtilsClass.getMethod("getSeed").invoke(null);

            Assert.assertEquals(42L, seed);
        }
    }

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

    private static URL[] toUrls(String[] classpathEntries) throws Exception {
        URL[] urls = new URL[classpathEntries.length];
        for (int i = 0; i < classpathEntries.length; i++) {
            urls[i] = new File(classpathEntries[i]).toURI().toURL();
        }
        return urls;
    }
}
