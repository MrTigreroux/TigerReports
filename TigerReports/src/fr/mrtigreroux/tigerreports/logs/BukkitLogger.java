package fr.mrtigreroux.tigerreports.logs;

import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */
public class BukkitLogger extends Logger {

	private final String name;
	private final String pluginName;
	private boolean showName = true;
	private boolean useColors = true;
	private final java.util.logging.Logger logger;
	private Level level;

	public BukkitLogger(String name, String pluginName, Level minLoggableLevel, boolean showName, boolean useColors) {
		this.name = name;
		this.pluginName = pluginName;
		this.showName = showName;
		this.useColors = useColors;
		logger = Bukkit.getLogger();
		setLevel(minLoggableLevel);
	}

	public void setLevel(Level level) {
		this.level = level; // This class uses the main bukkit logger, therefore the levels must be managed here.
		if (!logger.isLoggable(level)) { // Reduce the level if necessary, but do not increase it because the same bukkit logger is shared across all the BukkitLogger (this class) instances.
			logger.setLevel(level);
		}
	}

	@Override
	public void info(Supplier<?> message) {
		log(Level.INFO, message);
	}

	@Override
	public void warn(Supplier<?> message) {
		log(Level.WARNING, message);
	}

	@Override
	public void error(String message) {
		log(Level.SEVERE, MessageUtils.LINE);
		log(Level.SEVERE, message);
		log(Level.SEVERE, MessageUtils.LINE);
	}

	@Override
	public void error(String message, Throwable thrown) {
		log(Level.SEVERE, message, thrown);
	}

	public void log(Level level, Supplier<?> messageSupplier) {
		if (isLoggable(level)) {
			log(level, messageSupplier.get().toString(), null);
		}
	}

	public void log(Level level, String message) {
		log(level, message, null);
	}

	public void log(Level level, String message, Throwable thrown) {
		if (isLoggable(level)) {
			message = getFormattedMessage(getColoredMessage(message, level), level);
			if (thrown != null) {
				logger.log(level, message, thrown);
			} else {
				logger.log(level, message);
			}
		}
	}

	private String getFormattedMessage(String message, Level level) {
		String formattedMsg;
		if (showName) {
			String adjustementForLevelLength = Level.SEVERE.equals(level) ? "" : " ";
			formattedMsg = String.format("[%s] " + adjustementForLevelLength + "%20.20s - %s", pluginName, name,
			        message);
		} else {
			formattedMsg = String.format("[%s] %s", pluginName, message);
		}
		return formattedMsg;
	}

	private String getColoredMessage(String message, Level level) {
		String color = getLevelColor(level);
		if (color != null && !color.isEmpty()) {
			return color + message + "\033[0m";
		} else {
			return message;
		}
	}

	private String getLevelColor(Level level) {
		if (!useColors) {
			return "";
		}

		if (Level.WARNING.equals(level)) {
			return "\033[33m";
		} else if (Level.SEVERE.equals(level)) {
			return "\033[31m";
		} else {
			return "";
		}
	}

	@Override
	public boolean isInfoLoggable() {
		return isLoggable(Level.INFO);
	}

	@Override
	public boolean isWarnLoggable() {
		return isLoggable(Level.WARNING);
	}

	@Override
	public boolean isErrorLoggable() {
		return isLoggable(Level.SEVERE);
	}

	public boolean isLoggable(Level level) {
		return logger.isLoggable(level) && level.intValue() >= this.level.intValue(); // This class uses the main bukkit logger, therefore the levels must be managed here.
	}

}
