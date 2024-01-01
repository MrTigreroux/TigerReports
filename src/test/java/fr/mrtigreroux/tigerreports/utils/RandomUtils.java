package fr.mrtigreroux.tigerreports.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import fr.mrtigreroux.tigerreports.logs.Logger;

/**
 * @author MrTigreroux
 */
public class RandomUtils {

    private static final Logger LOGGER = Logger.fromClass(RandomUtils.class);
    public static final String ALPHABET = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

    public static double getRandomDouble(Random r) {
        return getRandomDouble(r, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public static double getRandomDouble(Random r, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " > max " + max);
        }
        return min + (max - min) * r.nextDouble();
    }

    public static float getRandomFloat(Random r) {
        return getRandomFloat(r, Float.MIN_VALUE, Float.MAX_VALUE);
    }

    public static float getRandomFloat(Random r, float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " > max " + max);
        }
        return min + (max - min) * r.nextFloat();
    }

    public static long getRandomLong(Random r) {
        return getRandomLong(r, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static long getRandomLong(Random r, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " > max " + max);
        }
        return min + (max - min) * r.nextLong();
    }

    public static int getRandomInt(Random r) {
        return getRandomInt(r, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static int getRandomInt(Random r, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " > max " + max);
        }
        return (int) ((long) min + r.nextDouble() * ((long) max - min + 1));
    }

    public static short getRandomShort(Random r) {
        return (short) (r.nextInt(Short.MAX_VALUE * 2 + 1) - Short.MAX_VALUE);
    }

    public static byte getRandomByte(Random r) {
        byte[] res = new byte[1];
        r.nextBytes(res);
        return res[0];
    }

    public static String getRandomString(Random r, int minLength, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int alphabetLength = ALPHABET.length();
        for (int i = 0; i < getRandomInt(r, minLength, maxLength); i++) {
            sb.append(ALPHABET.charAt(r.nextInt(alphabetLength)));
        }
        return sb.toString();
    }

    public static Object getRandomFieldValue(Random r, Field field) {
        String fieldClassName = field.getType().getName();
        if (Integer.TYPE.getName().equals(fieldClassName)) {
            return getRandomInt(r);
        } else if (Long.TYPE.getName().equals(fieldClassName)) {
            return getRandomLong(r);
        } else if (Float.TYPE.getName().equals(fieldClassName)) {
            return getRandomFloat(r);
        } else if (Double.TYPE.getName().equals(fieldClassName)) {
            return getRandomDouble(r);
        } else if (Boolean.TYPE.getName().equals(fieldClassName)) {
            return r.nextBoolean();
        } else if (Byte.TYPE.getName().equals(fieldClassName)) {
            return getRandomByte(r);
        } else if (Short.TYPE.getName().equals(fieldClassName)) {
            return getRandomShort(r);
        } else if (String.class.getName().equals(fieldClassName)) {
            return getRandomString(r, 0, 20);
        } else if (String[].class.getName().equals(fieldClassName)) {
            int length = getRandomInt(r, 0, 20);
            String[] res = new String[length];
            for (int i = 0; i < res.length; i++) {
                res[i] = getRandomString(r, 0, 20);
            }
            return res;
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + fieldClassName);
        }
    }

    public static <T> int executeActionOnRandomElements(List<T> elements, int minPickedAmount, int maxPickedAmount,
            Consumer<T> pickedElementAction) {
        if (minPickedAmount > maxPickedAmount) {
            throw new IllegalArgumentException(minPickedAmount + " > " + maxPickedAmount);
        }

        int pickedElementsAmount = 0;

        Random rand = new Random();
        for (int i = 0; i < elements.size(); i++) {
            if (pickedElementsAmount >= maxPickedAmount) {
                break;
            }

            if (rand.nextBoolean() || ((elements.size() - i) <= (minPickedAmount - pickedElementsAmount))) { // testsReportsData.size() - i = amount of remaining reports counting the current i
                int fi = i;
                int fpickedReportsAmount = pickedElementsAmount;
                LOGGER.debug(() -> "executeActionOnRandomElements(): picked element at i = " + fi + ", "
                        + (elements.size() - fi) + ", " + (minPickedAmount - fpickedReportsAmount));
                pickedElementAction.accept(elements.get(i));
                pickedElementsAmount++;
            }
        }

        int fpickedElementsAmount = pickedElementsAmount;
        assertTrue(pickedElementsAmount >= minPickedAmount,
                () -> "pickedElementsAmount = " + fpickedElementsAmount + ", maxPickedAmount = " + maxPickedAmount);
        assertTrue(pickedElementsAmount <= maxPickedAmount,
                () -> "pickedElementsAmount = " + fpickedElementsAmount + ", maxPickedAmount = " + maxPickedAmount);
        return pickedElementsAmount;
    }

}
