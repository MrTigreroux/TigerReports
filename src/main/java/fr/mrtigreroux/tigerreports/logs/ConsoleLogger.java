package fr.mrtigreroux.tigerreports.logs;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * @author MrTigreroux
 */
public class ConsoleLogger extends Logger {

	public static class StdoutConsoleHandler extends ConsoleHandler {

		@Override
		protected void setOutputStream(OutputStream out) throws SecurityException {
			super.setOutputStream(System.out);
		}

	}

	private static final ConsoleHandler CONSOLE_HANDLER;

	private static final Formatter FORMATTER = new SimpleFormatter() {

		private static final String FORMAT = "%1$tH:%1$tM:%1$tS.%1$tL | %2$s | %3$14.14s | %4$s %5$s %n";

		@Override
		public synchronized String format(LogRecord lr) {
			Level level = Level.fromLoggingLevel(lr.getLevel());

			String thrownStackTrace = "";
			if (lr.getThrown() != null) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				pw.println();
				lr.getThrown().printStackTrace(pw);
				pw.close();
				thrownStackTrace = level.getColoredMessage(sw.toString());
			}

			return String.format(FORMAT, new Date(lr.getMillis()), level.getColoredMessage(level.getDisplayName(true)),
			        Thread.currentThread().getName(), lr.getMessage(), thrownStackTrace);
		}

	};

	static {
		CONSOLE_HANDLER = new StdoutConsoleHandler();
		CONSOLE_HANDLER.setFormatter(FORMATTER);
		CONSOLE_HANDLER.setLevel(java.util.logging.Level.ALL);
	}

	private final String name;
	private final java.util.logging.Logger logger;

	public ConsoleLogger(String name, String pluginName, Level minLoggableLevel) {
		this.name = name;
		logger = java.util.logging.Logger.getLogger("fr.mrtigreroux." + pluginName + ".logger." + name);
		if (!containsHandler(logger, CONSOLE_HANDLER)) {
			logger.addHandler(CONSOLE_HANDLER);
		}
		logger.setUseParentHandlers(false);
		setLevel(minLoggableLevel);
	}

	@Override
	public void setLevel(Level level) {
		java.util.logging.Level loggingLevel = level.getLoggingLevel();
		if (level != null && !loggingLevel.equals(logger.getLevel())) {
			logger.setLevel(loggingLevel);
		}
	}

	@Override
	public boolean isLoggable(Level level) {
		return logger.isLoggable(level.getLoggingLevel());
	}

	@Override
	public void log(Level level, String message, Throwable thrown) {
		if (isLoggable(level)) {
			message = getFormattedMessage(level.getColoredMessage(message));
			if (thrown != null) {
				logger.log(level.getLoggingLevel(), message, thrown);
			} else {
				logger.log(level.getLoggingLevel(), message);
			}
		}
	}

	private String getFormattedMessage(String message) {
		return String.format("%20.20s - %s", name, message);
	}

}
