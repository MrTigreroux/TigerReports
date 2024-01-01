package fr.mrtigreroux.tigerreports.logs;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.utils.CollectionUtils;

/**
 * @author MrTigreroux
 */
public enum GlobalLogger {

    CLASS("class", Level.WARN),
    MAIN("main", Level.WARN),
    SQL("sql", Level.ERROR),
    BUNGEE("bungee", Level.ERROR),
    EVENTS("events", Level.ERROR),
    CONFIG("config", Level.INFO);

    private final String loggerName;
    private final Level defaultLevel;

    GlobalLogger(String loggerName, Level defaultLevel) {
        this.loggerName = loggerName;
        this.defaultLevel = defaultLevel;
    }

    public int getConfigLine() {
        return 1 + ordinal();
    }

    public Level getConfiguredLoggerLevel(List<String> configLines, String pluginName) {
        if (configLines == null) {
            return getDefaultLevel();
        }

        try {
            return Objects.requireNonNull(Level.fromDisplayName(configLines.get(getConfigLine())
                    .substring(
                            getLoggerName().length() + Logger.LOGS_CONFIG_FILE_GLOBAL_LOGGER_DATA_SEPARATOR.length())));
        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            Bukkit.getLogger()
                    .log(java.util.logging.Level.SEVERE, "[" + pluginName + "] Invalid " + Logger.LOGS_CONFIG_FILE_NAME
                            + " file for " + getLoggerName() + ": " + CollectionUtils.toString(configLines), ex);
            return getDefaultLevel();
        }
    }

    public BukkitLogger createBukkitLogger(List<String> configLines, String pluginName, boolean showName,
            boolean useColors) {
        return new BukkitLogger(getLoggerName(), pluginName, getConfiguredLoggerLevel(configLines, pluginName),
                showName, useColors);
    }

    public ConsoleLogger createConsoleLogger(String pluginName) {
        return new ConsoleLogger(getLoggerName(), pluginName, getDefaultLevel());
    }

    public String getLoggerName() {
        return loggerName;
    }

    public Level getDefaultLevel() {
        return defaultLevel;
    }

}
