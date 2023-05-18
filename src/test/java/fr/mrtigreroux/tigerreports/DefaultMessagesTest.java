package fr.mrtigreroux.tigerreports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * @author MrTigreroux
 */
public class DefaultMessagesTest extends TestClass {

	private final String DEFAULT_MESSAGES_DIRECTORY_NAME = "default-messages";
	private final int EXPECTED_LINES_AMOUNT = 220;

	@Test
	public void testDefaultMessagesFileLinesAmount() throws IOException {
		URL resourceURL = getClass().getResource("/messages.yml");

		if (resourceURL == null) {
			throw new IllegalArgumentException("The file messages.yml was not found.");
		}

		File messagesFile = new File(resourceURL.getFile());
		assertNotNull(messagesFile);
		testMessagesFileLinesAmount(messagesFile);
	}

	@Test
	public void testDefaultMessagesFilesLinesAmount() throws IOException {
		Path directory = Paths.get(System.getProperty("user.dir"), DEFAULT_MESSAGES_DIRECTORY_NAME);
		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException("The directory " + DEFAULT_MESSAGES_DIRECTORY_NAME + " was not found.");
		}

		Files.list(directory)
		        .filter(Files::isRegularFile)
		        .forEach((file) -> testMessagesFileLinesAmount(file.toFile()));
	}

	private void testMessagesFileLinesAmount(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			int linesAmount = 0;
			while (reader.readLine() != null) {
				linesAmount++;
			}

			assertEquals(EXPECTED_LINES_AMOUNT, linesAmount,
			        "The messages file " + file.getAbsolutePath() + " has an incorrect amount of lines");
		} catch (IOException e) {
			fail(e);
		}
	}
}
