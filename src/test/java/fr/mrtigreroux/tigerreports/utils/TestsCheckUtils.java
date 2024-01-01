package fr.mrtigreroux.tigerreports.utils;

/**
 * @author MrTigreroux
 */
public class TestsCheckUtils {

    private TestsCheckUtils() {}

    public static boolean longRightValue(long value, long minExpected, int excessCoef) {
        return longBetween(value, minExpected, minExpected * excessCoef);
    }

    public static boolean intBetween(int value, int min, int max) {
        return min <= value && value <= max;
    }

    public static boolean longBetween(long value, long min, long max) {
        return min <= value && value <= max;
    }

}
