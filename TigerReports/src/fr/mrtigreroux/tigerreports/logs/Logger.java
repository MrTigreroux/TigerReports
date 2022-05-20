package fr.mrtigreroux.tigerreports.logs;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.FileUtils;

/**
 * @author MrTigreroux
 */
public abstract class Logger {

	private static final String LOGS_CONFIG_FILE_NAME = "logs.config";

	public static final Logger MAIN;
	public static final Logger SQL;
	public static final Logger BUNGEE;
	public static final Logger EVENTS;
	public static final Logger CONFIG;

	private static Level classBukkitLoggerLevel = Level.SEVERE;
	private static boolean bukkitLoggersShowName = false;
	private static boolean bukkitLoggersUseColors = false;

	public static class GlobalLogger {

		String loggerName;
		int configLine;
		Level defaultBukkitLoggerLevel;

		public GlobalLogger(String loggerName, int configLine, Level defaultBukkitLoggerLevel) {
			this.loggerName = loggerName;
			this.configLine = configLine;
			this.defaultBukkitLoggerLevel = defaultBukkitLoggerLevel;
		}

		public Level getBukkitLoggerLevel(List<String> configLines, String pluginName) {
			if (configLines == null) {
				return defaultBukkitLoggerLevel;
			}

			try {
				return Level.parse(configLines.get(configLine).substring(loggerName.length() + 2));
			} catch (IndexOutOfBoundsException | NullPointerException | IllegalArgumentException ex) {
				Bukkit.getLogger()
				        .log(Level.SEVERE, "[" + pluginName + "] Invalid " + LOGS_CONFIG_FILE_NAME + " file for "
				                + loggerName + ": " + CollectionUtils.toString(configLines), ex);
				return defaultBukkitLoggerLevel;
			}
		}

		public BukkitLogger createBukkitLogger(List<String> configLines, String pluginName, boolean showName,
		        boolean useColors) {
			return new BukkitLogger(loggerName, pluginName, getBukkitLoggerLevel(configLines, pluginName), showName,
			        useColors);
		}

		private Log4JLogger createLog4JLogger(String pluginName) {
			return new Log4JLogger("fr.mrtigreroux." + pluginName + ".logger." + loggerName);
		}

	}

	static {
		Plugin plugin = TigerReports.getInstance();
		String pluginName = plugin.getName();

		final GlobalLogger mainGlobalLogger = new GlobalLogger("main", 2, Level.WARNING);
		final GlobalLogger sqlGlobalLogger = new GlobalLogger("sql", 3, Level.SEVERE);
		final GlobalLogger bungeeGlobalLogger = new GlobalLogger("bungee", 4, Level.SEVERE);
		final GlobalLogger eventsGlobalLogger = new GlobalLogger("events", 5, Level.SEVERE);
		final GlobalLogger configGlobalLogger = new GlobalLogger("config", 6, Level.INFO);

		if (Bukkit.getLogger() != null) {
			List<String> configLines;
			try {
				configLines = FileUtils.getFileLines(FileUtils.getPluginDataFile(plugin, LOGS_CONFIG_FILE_NAME));

				if (!configLines.isEmpty() && configLines.size() >= 2) {
					String[] firstLineParams = configLines.get(0).split(",");
					bukkitLoggersShowName = "1".equals(firstLineParams[0]);
					bukkitLoggersUseColors = "1".equals(firstLineParams[1]);

					try {
						classBukkitLoggerLevel = Level.parse(configLines.get(1).substring(7));
					} catch (IndexOutOfBoundsException | NullPointerException | IllegalArgumentException ex) {
						Bukkit.getLogger()
						        .log(Level.SEVERE, "[" + pluginName + "] Invalid " + LOGS_CONFIG_FILE_NAME
						                + " file for class: " + CollectionUtils.toString(configLines), ex);
					}
				} else {
					Bukkit.getLogger()
					        .log(Level.SEVERE, "[" + pluginName + "] Invalid " + LOGS_CONFIG_FILE_NAME + " file: "
					                + CollectionUtils.toString(configLines));
				}
			} catch (IOException | SecurityException e) {
				configLines = null;
			}

			MAIN = mainGlobalLogger.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			SQL = sqlGlobalLogger.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			BUNGEE = bungeeGlobalLogger.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			EVENTS = eventsGlobalLogger.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
			CONFIG = configGlobalLogger.createBukkitLogger(configLines, pluginName, bukkitLoggersShowName,
			        bukkitLoggersUseColors);

			if (configLines != null) {
				MAIN.info(() -> "Using " + LOGS_CONFIG_FILE_NAME + " file.");
			}
		} else {
			MAIN = mainGlobalLogger.createLog4JLogger(pluginName);
			SQL = sqlGlobalLogger.createLog4JLogger(pluginName);
			BUNGEE = bungeeGlobalLogger.createLog4JLogger(pluginName);
			EVENTS = eventsGlobalLogger.createLog4JLogger(pluginName);
			CONFIG = configGlobalLogger.createLog4JLogger(pluginName);
		}
	}

	public static Logger fromClass(Class<?> clazz) {
		return fromClass(clazz, TigerReports.getInstance().getName());
	}

	public static Logger fromClass(Class<?> clazz, String pluginName) {
		String loggerName = clazz.getSimpleName();
		if (Bukkit.getLogger() != null) {
			return new BukkitLogger(loggerName, pluginName, classBukkitLoggerLevel, bukkitLoggersShowName,
			        bukkitLoggersUseColors);
		} else {
			return new Log4JLogger(clazz);
		}
	}

	public Logger() {}

	public abstract boolean isInfoLoggable();

	public abstract boolean isWarnLoggable();

	public abstract boolean isErrorLoggable();

	public abstract void info(Supplier<?> message);

	public abstract void warn(Supplier<?> message);

	public abstract void error(String message);

	public abstract void error(String message, Throwable thrown);

}
