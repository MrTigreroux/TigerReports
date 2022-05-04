package fr.mrtigreroux.tigerreports.logs;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;

public class Log4JLogger extends Logger {

	private final org.apache.logging.log4j.Logger logger;

	public Log4JLogger(String name) {
		logger = LogManager.getLogger(name);
	}

	public Log4JLogger(Class<?> clazz) {
		logger = LogManager.getLogger(clazz);
	}

	@Override
	public void info(Supplier<?> message) {
		logger.info("{}", message);
	}

	@Override
	public void warn(Supplier<?> message) {
		logger.warn("{}", message);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public void error(String message, Throwable thrown) {
		logger.error(message, thrown);
	}

	@Override
	public boolean isInfoLoggable() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnLoggable() {
		return logger.isWarnEnabled();
	}

	@Override
	public boolean isErrorLoggable() {
		return logger.isErrorEnabled();
	}

}
