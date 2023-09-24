package fr.mrtigreroux.tigerreports.logs;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Handler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.FileUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */
public abstract class Logger {

	public static final String LOGS_CONFIG_FILE_NAME = "logs.config";
	public static final String LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE = "1";
	public static final String LOGS_CONFIG_FILE_GLOBAL_SETTINGS_SEPARATOR = ",";
	public static final String LOGS_CONFIG_FILE_GLOBAL_LOGGER_DATA_SEPARATOR = ": ";

	private static final boolean USE_BUKKIT_LOGGER;
	private static final String DEFAULT_PLUGIN_NAME;

	public static final Logger MAIN;
	public static final Logger SQL;
	public static final Logger BUNGEE;
	public static final Logger EVENTS;
	public static final Logger CONFIG;

	private static Level defaultClassLoggerLevel = Level.ERROR;
	private static boolean bukkitLoggersShowName = false;
	private static boolean bukkitLoggersUseColors = false;

	static {
		Plugin plugin = TigerReports.getInstance();
		String pluginName = plugin.getName();
		DEFAULT_PLUGIN_NAME = pluginName;
		USE_BUKKIT_LOGGER = Bukkit.getLogger() != null;

		if (USE_BUKKIT_LOGGER) {
			List<String> configLines;
			try {
				configLines = FileUtils.getFileLines(FileUtils.getPluginDataFile(plugin, LOGS_CONFIG_FILE_NAME));

				if (!configLines.isEmpty() && configLines.size() >= 2) {
					String[] firstLineParams = configLines.get(0).split(LOGS_CONFIG_FILE_GLOBAL_SETTINGS_SEPARATOR);
					bukkitLoggersShowName = LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE.equals(firstLineParams[0]);
					bukkitLoggersUseColors = LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE.equals(firstLineParams[1]);

					setDefaultClassLoggerLevel(GlobalLogger.CLASS.getConfiguredLoggerLevel(configLines, pluginName));
				} else {
					Bukkit.getLogger()
					        .log(java.util.logging.Level.SEVERE, "[" + pluginName + "] Invalid " + LOGS_CONFIG_FILE_NAME
					                + " file: " + CollectionUtils.toString(configLines));
				}
			} catch (IOException | SecurityException ex) { // The logs config file is probably missing
				configLines = null;
			}

			MAIN = GlobalLogger.MAIN.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			SQL = GlobalLogger.SQL.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			BUNGEE = GlobalLogger.BUNGEE.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			EVENTS = GlobalLogger.EVENTS.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			CONFIG = GlobalLogger.CONFIG.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);

			if (configLines != null) {
				MAIN.info(() -> "Using " + LOGS_CONFIG_FILE_NAME + " file.");
			}
		} else {
			MAIN = GlobalLogger.MAIN.createConsoleLogger(pluginName);
			SQL = GlobalLogger.SQL.createConsoleLogger(pluginName);
			BUNGEE = GlobalLogger.BUNGEE.createConsoleLogger(pluginName);
			EVENTS = GlobalLogger.EVENTS.createConsoleLogger(pluginName);
			CONFIG = GlobalLogger.CONFIG.createConsoleLogger(pluginName);
		}
	}

	public static Logger fromClass(Class<?> clazz) {
		return fromClass(clazz, DEFAULT_PLUGIN_NAME);
	}

	public static Logger fromClass(Class<?> clazz, String pluginName) {
		String loggerName = clazz.getSimpleName();
		if (USE_BUKKIT_LOGGER) {
			return new BukkitLogger(loggerName, pluginName, defaultClassLoggerLevel, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
		} else {
			return new ConsoleLogger(loggerName, pluginName, defaultClassLoggerLevel);
		}
	}

	public static void setDefaultClassLoggerLevel(Level defaultClassLoggerLevel) {
		Logger.defaultClassLoggerLevel = defaultClassLoggerLevel;
	}

	public Logger() {}

	public abstract void setLevel(Level level);

	public boolean isInfoLoggable() {
		return isLoggable(Level.INFO);
	}

	public boolean isWarnLoggable() {
		return isLoggable(Level.WARN);
	}

	public boolean isErrorLoggable() {
		return isLoggable(Level.ERROR);
	}

	public abstract boolean isLoggable(Level level);

	public void debug(Supplier<?> messageSupplier) {
		log(Level.DEBUG, messageSupplier);
	}

	public void debug(Supplier<?> messageSupplier, Throwable thrown) {
		log(Level.DEBUG, messageSupplier, thrown);
	}

	public void info(Supplier<?> messageSupplier) {
		log(Level.INFO, messageSupplier);
	}

	public void warn(Supplier<?> messageSupplier) {
		log(Level.WARN, messageSupplier);
	}

	public void warn(Supplier<?> messageSupplier, Throwable thrown) {
		log(Level.WARN, messageSupplier, thrown);
	}

	public void error(String message) {
		log(Level.ERROR, MessageUtils.LINE);
		log(Level.ERROR, message);
		log(Level.ERROR, MessageUtils.LINE);
	}

	public void error(String message, Throwable thrown) {
		log(Level.ERROR, message, thrown);
	}

	public void log(Level level, Supplier<?> messageSupplier) {
		log(level, messageSupplier, null);
	}

	public void log(Level level, Supplier<?> messageSupplier, Throwable thrown) {
		if (isLoggable(level)) {
			log(level, messageSupplier.get().toString(), null);
		}
	}

	public void log(Level level, String message) {
		log(level, message, null);
	}

	public abstract void log(Level level, String message, Throwable thrown);

	public static boolean containsHandler(java.util.logging.Logger logger, Handler handler) {
		if (handler == null) {
			return false;
		}

		Handler[] handlers = logger.getHandlers();
		for (Handler h : handlers) {
			if (handler.equals(h)) {
				return true;
			}
		}
		return false;
	}

}
