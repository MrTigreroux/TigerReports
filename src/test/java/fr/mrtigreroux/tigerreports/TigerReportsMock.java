package fr.mrtigreroux.tigerreports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.bukkit.Bukkit;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.logs.Level;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */
public class TigerReportsMock {

	private static final TigerReports MAIN_MOCK;
	public static final File TESTS_PLUGIN_DATA_FOLDER = new File("tests/plugin-data-folder");

	static {
		System.out.println("TigerReportsMock - static block init");
		Thread.currentThread().setName("tests");

		clearFolder(TESTS_PLUGIN_DATA_FOLDER);
		TESTS_PLUGIN_DATA_FOLDER.mkdir();
		assertTrue(TESTS_PLUGIN_DATA_FOLDER.exists());

		MAIN_MOCK = new TigerReportsMock().mockDataFolder(TESTS_PLUGIN_DATA_FOLDER).get();
		try (MockedStatic<TigerReports> tr = mockStatic(TigerReports.class);
		        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			tr.when(TigerReports::getInstance).thenReturn(MAIN_MOCK);
			bukkit.when(Bukkit::getLogger).thenReturn(null);
			assertNotNull(Logger.MAIN); // init static Logger block

			bukkit.when(Bukkit::getVersion).thenReturn("Spigot (MC: 1.8.3)");
			assertNotNull(MessageUtils.APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD); // init static MessageUtils block
			MenuRawItem.init();
		}

		loadConfigFiles(MAIN_MOCK);

		Logger.CONFIG.setLevel(Level.INFO);
		Logger.BUNGEE.setLevel(Level.INFO);
		Logger.EVENTS.setLevel(Level.INFO);
		Logger.MAIN.setLevel(Level.INFO);
		Logger.SQL.setLevel(Level.INFO);
		Logger.setDefaultClassLoggerLevel(Level.DEBUG);
	}

	@Deprecated
	public static <T> Class<T> forceInit(Class<T> clazz) {
		try {
			Class.forName(clazz.getName(), true, clazz.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e); // Can't happen
		}
		return clazz;
	}

	public static void clearFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					clearFolder(f);
				} else {
					f.delete();
				}
			}
		}
	}

	private TigerReports mock;

	public TigerReportsMock() {
		mock = mock(TigerReports.class);
	}

	public TigerReportsMock mockDataFolder(File folder) {
		when(mock.getDataFolder()).thenReturn(folder);
		return this;
	}

	public TigerReports get() {
		when(mock.getName()).thenReturn("TigerReports");
		when(mock.getResource(Mockito.anyString())).then((invocation) -> {
			String filename = invocation.getArgument(0);
			if (filename == null) {
				throw new IllegalArgumentException("Filename cannot be null");
			}

			try {
				URL url = new File("src/test/resources/" + filename).toURI().toURL();

				if (url == null) {
					return null;
				}

				URLConnection connection = url.openConnection();
				connection.setUseCaches(false);
				return connection.getInputStream();
			} catch (IOException ex) {
				return null;
			}
		});

		// mock.saveResource doesn't do anything because config files are already known and accessible

		return mock;
	}

	public static Logger getLoggerFromClass(Class<?> clazz) {
		return getLoggerFromClass(clazz, Level.INFO);
	}

	public static Logger getLoggerFromClass(Class<?> clazz, Level level) {
		Logger logger = Logger.fromClass(clazz);
		logger.setLevel(level);
		return logger;
	}

	public static void loadConfigFiles(TigerReports tr) {
		for (ConfigFile configFiles : ConfigFile.values()) {
			configFiles.load(tr);
		}
	}

	public static TigerReports getMainMock() {
		return MAIN_MOCK;
	}

}
