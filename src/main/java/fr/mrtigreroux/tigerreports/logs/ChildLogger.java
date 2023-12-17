package fr.mrtigreroux.tigerreports.logs;

/**
 * @author MrTigreroux
 */
public class ChildLogger extends Logger {

	private final Logger parentLogger;
	private final String name;

	public ChildLogger(Logger parentLogger, String name) {
		super();
		this.parentLogger = parentLogger;
		this.name = name;
	}

	@Override
	public void setLevel(Level level) {
		parentLogger.setLevel(level);
	}

	@Override
	public boolean isLoggable(Level level) {
		return parentLogger.isLoggable(level);
	}

	@Override
	public void log(Level level, String message, Throwable thrown) {
		parentLogger.log(level, "[" + name + "] " + message, thrown);
	}

}
