package es.us.isa.httpmutator.core.util;


import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple global random utility.
 * Provides reproducible randomness by controlling a single global seed
 * TODO: RandomManager is not suitable for this purpose as it is designed to be extended by multiple classes
 *
 * @author lixin
 */
public class RandomUtils {
    private static final AtomicLong GLOBAL_SEED = new AtomicLong(-1L);
    private static volatile SplittableRandom RNG = new SplittableRandom();
    private static volatile Random RANDOM = new Random();


    private RandomUtils() {
    }

    /**
     * global seed
     */
    public static void setSeed(long seed) {
        GLOBAL_SEED.set(seed);
        RNG = new SplittableRandom(seed);
        RANDOM = new Random(seed);
    }

    /**
     * back to non-deterministic mode
     */
    public static void clearSeed() {
        GLOBAL_SEED.set(-1L);
        RNG = new SplittableRandom();
        RANDOM = new Random();
    }

    public static long getSeed() {
        return GLOBAL_SEED.get();
    }

    // ========== Random number generation methods ==========
    public static int nextInt(int bound) {
        return RNG.nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        return RNG.nextInt(origin, bound);
    }

    public static long nextLong() {
        return RNG.nextLong();
    }

    public static double nextDouble() {
        return RNG.nextDouble();
    }

    public static boolean nextBoolean() {
        return RNG.nextBoolean();
    }

    public static Random getRandom() {
        return RANDOM;
    }
}
