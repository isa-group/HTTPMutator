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

    /**
     * Returns a random int in [origin, bound) (exclusive upper bound).
     * Uses SplittableRandom semantics.
     */
    public static int nextInt(int origin, int bound) {
        return RNG.nextInt(origin, bound);
    }

    /**
     * Returns a random int in [lower, upper] (inclusive upper bound).
     */
    public static int nextIntInclusive(int lower, int upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("lower must be <= upper");
        }
        if (lower == upper) {
            return lower;
        }
        long exclusiveUpper = (long) upper + 1L;
        if (exclusiveUpper <= Integer.MAX_VALUE) {
            return RNG.nextInt(lower, upper + 1);
        }
        return (int) RNG.nextLong(lower, exclusiveUpper);
    }

    public static long nextLong() {
        return RNG.nextLong();
    }

    /**
     * Returns a random long in [lower, upper] (inclusive upper bound).
     */
    public static long nextLongInclusive(long lower, long upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("lower must be <= upper");
        }
        if (lower == upper) {
            return lower;
        }
        if (upper < Long.MAX_VALUE) {
            return RNG.nextLong(lower, upper + 1L);
        }
        if (lower == Long.MIN_VALUE) {
            return RNG.nextLong();
        }
        if (lower == 0L) {
            return RNG.nextLong() & Long.MAX_VALUE;
        }
        if (lower > 0L) {
            long bound = Long.MAX_VALUE - lower + 1L;
            return lower + RNG.nextLong(bound);
        }

        long value;
        do {
            value = RNG.nextLong();
        } while (value < lower);
        return value;
    }

    public static double nextDouble() {
        return RNG.nextDouble();
    }

    /**
     * Returns a random double in [lower, upper) (approximately).
     * Replaces Apache Commons RandomDataGenerator.nextUniform(lower, upper).
     */
    public static double nextUniform(double lower, double upper) {
        return lower + (upper - lower) * RNG.nextDouble();
    }

    public static float nextFloat() {
        return (float) RNG.nextDouble();
    }

    public static boolean nextBoolean() {
        return RNG.nextBoolean();
    }

    public static Random getRandom() {
        return RANDOM;
    }
}
