package fr.mrtigreroux.tigerreports.logs;

import org.bukkit.Bukkit;

/**
 * @author MrTigreroux
 */
public class BukkitLogger extends Logger {

	private final String name;
	private final String pluginName;
	private boolean showName = true;
	private boolean useColors = true;
	private final java.util.logging.Logger logger;
	private java.util.logging.Level loggingLevel;

	public BukkitLogger(String name, String pluginName, Level minLoggableLevel, boolean showName, boolean useColors) {
		this.name = name;
		this.pluginName = pluginName;
		this.showName = showName;
		this.useColors = useColors;
		logger = Bukkit.getLogger();
		setLevel(minLoggableLevel);
	}

	@Override
	public void setLevel(Level level) {
		loggingLevel = level.getLoggingLevel(); // This class uses the main bukkit logger, therefore the levels must be managed here.
		if (!logger.isLoggable(loggingLevel)) { // Reduce the level if necessary, but do not increase it because the same bukkit logger is shared across all the BukkitLogger (this class) instances.
			logger.setLevel(loggingLevel);
		}
	}

	@Override
	public boolean isLoggable(Level level) {
		return logger.isLoggable(level.getLoggingLevel())
		        && level.getLoggingLevel().intValue() >= loggingLevel.intValue(); // This class uses the main bukkit logger, therefore the levels must be managed here.
	}

	@Override
	public void log(Level level, String message, Throwable thrown) {
		if (isLoggable(level)) {
			if (useColors) {
				message = level.getColoredMessage(message);
			}

			if (level.getLoggingLevel().intValue() <= java.util.logging.Level.INFO.intValue()) {
				// fix levels lower than INFO not printed by Bukkit
				level = Level.INFO;
			}

			message = getFormattedMessage(message, level);
			if (thrown != null) {
				logger.log(level.getLoggingLevel(), message, thrown);
			} else {
				logger.log(level.getLoggingLevel(), message);
			}
		}
	}

	private String getFormattedMessage(String message, Level level) {
		String formattedMsg;
		if (showName) {
			String adjustementForLevelLength = level.getDisplayName().length() == 4 ? " " : "";
			formattedMsg = String.format("[%s] " + adjustementForLevelLength + "%20.20s - %s", pluginName, name,
			        message);
		} else {
			formattedMsg = String.format("[%s] %s", pluginName, message);
		}
		return formattedMsg;
	}

}
