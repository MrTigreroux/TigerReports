package fr.mrtigreroux.tigerreports.utils;

import java.util.Collection;
import java.util.Objects;

/**
 * @author MrTigreroux
 */
public class CheckUtils {
    
    private CheckUtils() {}
    
    public static <T> T notNull(T o) {
        return Objects.requireNonNull(o);
    }
    
    public static String notEmpty(String str) {
        if (str.isEmpty()) {
            throw new IllegalArgumentException("String is empty");
        }
        return str;
    }
    
    public static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
    
    public static <T, C extends Collection<T>> C notEmpty(C c) {
        if (c.isEmpty()) {
            throw new IllegalArgumentException("Collection is empty");
        }
        return c;
    }
    
    public static <T> T[] notEmpty(T[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }
        return array;
    }
    
    public static <T, C extends Collection<T>> boolean isNotEmpty(C c) {
        return c != null && !c.isEmpty();
    }
    
    public static long strictlyPositive(long l) {
        if (l <= 0L) {
            throw new IllegalArgumentException("Long is not strictly positive");
        }
        return l;
    }
    
}
