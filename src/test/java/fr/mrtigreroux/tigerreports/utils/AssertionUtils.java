package fr.mrtigreroux.tigerreports.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.DeeplyCloneable;

/**
 * @author MrTigreroux
 */
public class AssertionUtils {

	private static final Logger LOGGER = Logger.fromClass(AssertionUtils.class);

	private static final Set<Class<?>> PRIMITIVES_CLASS = getPrimitivesClass();

	public static boolean isPrimitive(Class<?> clazz) {
		return PRIMITIVES_CLASS.contains(clazz);
	}

	private static Set<Class<?>> getPrimitivesClass() {
		Set<Class<?>> wrappers = new HashSet<Class<?>>();
		wrappers.add(Boolean.class);
		wrappers.add(Character.class);
		wrappers.add(Byte.class);
		wrappers.add(Short.class);
		wrappers.add(Integer.class);
		wrappers.add(Long.class);
		wrappers.add(Float.class);
		wrappers.add(Double.class);
		wrappers.add(boolean.class);
		wrappers.add(char.class);
		wrappers.add(byte.class);
		wrappers.add(short.class);
		wrappers.add(int.class);
		wrappers.add(long.class);
		wrappers.add(float.class);
		wrappers.add(double.class);
		wrappers.add(String.class);
		return wrappers;
	}

	/**
	 * 
	 * @param <T>
	 * @param object               must have all its non static fields not null.
	 * @param uniqueInstanceFields name of fields which should have the same instance for clones that the one used in {@code object}
	 */
	public static <T extends DeeplyCloneable<T>> void assertDeeplyCloneable(DeeplyCloneable<T> object,
	        String... uniqueInstanceFields) {
		assertNotNull(object);

		T deepClone = object.deepClone();
		assertDeepEquals(object, deepClone);

		// Check that non primitive fields are different instances
		assertNotNullAndNotSameFieldsInstance(object, deepClone, uniqueInstanceFields);

//		assertThat(deepClone).usingRecursiveComparison().withEqualsForFields((a, b) -> {
//			System.out.println("a = " + a + ", b = " + b + ", a!=b = " + (a != b) + ", a class = " + a.getClass()
//			        + " a isPrimitive = " + (a != null && isPrimitiveWrapper(a.getClass())));
//			return a != b || (a != null && b != null && isPrimitiveWrapper(a.getClass())
//			        && a.getClass().equals(b.getClass()));
//		}).isEqualTo(object);
	}

	public static <T> void assertNotNullAndNotSameFieldsInstance(T expected, T actual, String... ignoredFields) {
		if (expected == null || actual == null) {
			fail("expected and actual must not be null");
		}

		List<Field> allFields = getAllFields(actual.getClass());
		List<String> ignored = Arrays.asList(ignoredFields);
		try {
			for (Field field : allFields) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}

				LOGGER.debug(() -> "assertDeepNotSameFieldsReferences(): field " + field.getName() + ", type: "
				        + field.getType());
				if (isPrimitive(field.getType())) {
					continue;
				}

				field.setAccessible(true);

				Object expectedObj = field.get(expected);

				assertNotNull(expectedObj, () -> "field " + field.getName()
				        + " is null for expected, all fields should not be null to be able to check the instance but also check that their value is correctly copied");

				if (ignored.contains(field.getName())) {
					LOGGER.debug(() -> "assertDeepNotSameFieldsReferences(): ignore: " + field.getName());
					continue;
				}
				Object actualObj = field.get(actual);

				assertTrue(expectedObj != actualObj,
				        () -> "same field " + field.getName() + " instance for expected and actual: " + actualObj);

				field.setAccessible(false);
			}
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		fail("error occurred while accessing fields");
	}

	public static List<Field> getAllFields(Class<?> type) {
		List<Field> fields = new ArrayList<Field>();
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			fields.addAll(Arrays.asList(c.getDeclaredFields()));
		}
		return fields;
	}

	public static <T> void assertDeepListEquals(List<T> expectedList, List<T> actualList,
	        String... fieldNamesToIgnore) {
		assertListEquals(expectedList, actualList, null);
		if (expectedList != null) {
			Iterator<T> expectedListIt = expectedList.iterator();
			Iterator<T> actualListIt = actualList.iterator();
			while (expectedListIt.hasNext()) {
				assertThat(actualListIt.next()).usingRecursiveComparison()
				        .ignoringFields(fieldNamesToIgnore)
				        .isEqualTo(expectedListIt.next());
			}
		}
	}

	public static <T> void assertListEquals(List<T> expectedList, List<T> actualList, String context) {
		if (expectedList != null) {
			String contextPrefix = context != null ? context + ": " : "";
			assertEquals(expectedList.size(), actualList.size(), () -> contextPrefix + "Expected: "
			        + CollectionUtils.toString(expectedList) + ", \nActual: " + CollectionUtils.toString(actualList));
			int i = 0;
			Iterator<T> expectedListIt = expectedList.iterator();
			Iterator<T> actualListIt = actualList.iterator();
			while (expectedListIt.hasNext()) {
				assertEquals(expectedListIt.next(), actualListIt.next(),
				        contextPrefix + "Index " + i + " is different");
				i++;
			}
		}
		assertEquals(expectedList, actualList);
	}

	public static <T> void assertDeepEquals(T expected, T actual, String... fieldNamesToIgnore) {
		assertThat(actual).usingRecursiveComparison().ignoringFields(fieldNamesToIgnore).isEqualTo(expected);
	}

	public static <T> void assertFieldEquals(T expected, T actual, String fieldNameToCheck) {
		assertFieldCompare(expected, actual, true, fieldNameToCheck);
	}

	public static <T> void assertFieldNotEquals(T expected, T actual, String fieldNameToCheck) {
		assertFieldCompare(expected, actual, false, fieldNameToCheck);
	}

	private static <T> void assertFieldCompare(T expected, T actual, boolean equals, String fieldNameToCheck) {
		if (expected == null || actual == null) {
			fail("expected and actual must not be null");
		}

		try {
			Field field = expected.getClass().getDeclaredField(fieldNameToCheck);
			field.setAccessible(true);
			if (equals) {
				assertEquals(field.get(expected), field.get(actual));
			} else {
				assertNotEquals(field.get(expected), field.get(actual));
			}
			field.setAccessible(false);
		} catch (NoSuchFieldException e) {
			fail("invalid field name: " + fieldNameToCheck + ": ", e);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
