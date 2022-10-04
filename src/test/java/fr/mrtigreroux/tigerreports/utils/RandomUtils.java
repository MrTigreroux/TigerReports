package fr.mrtigreroux.tigerreports.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import fr.mrtigreroux.tigerreports.logs.Logger;

/**
 * @author MrTigreroux
 */
public class RandomUtils {

	private static final Logger LOGGER = Logger.fromClass(RandomUtils.class);

	public static double getRandomDouble(Random r, double min, double max) {
		return min + (max - min) * r.nextDouble();
	}

	public static float getRandomFloat(Random r, float min, float max) {
		return min + (max - min) * r.nextFloat();
	}

	public static int getRandomInt(Random r, int min, int max) {
		return min + r.nextInt((max - min) + 1);
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
