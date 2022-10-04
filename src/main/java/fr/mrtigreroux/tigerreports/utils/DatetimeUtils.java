package fr.mrtigreroux.tigerreports.utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.logs.Logger;

/**
 * @author MrTigreroux
 */
public class DatetimeUtils {

	private static final Logger LOGGER = Logger.fromClass(DatetimeUtils.class);
	public static final String[] TIME_UNITS = new String[] { "YEAR", "MONTH", "WEEK", "DAY", "HOUR", "MINUTE",
	        "SECOND" };
	public static final int[] SECONDS_IN_UNIT = new int[] { 365 * 24 * 60 * 60, 30 * 24 * 60 * 60, 7 * 24 * 60 * 60,
	        24 * 60 * 60, 60 * 60, 60 };
	public static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
	public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT)
	        .withZone(ConfigUtils.getZoneId());

	private DatetimeUtils() {}

	public static byte getCurrentDayOfMonth() {
		return (byte) Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
	}

	public static String getRelativeDatetime(long secondsToAdd) {
		try {
			return ZonedDateTime.now().plusSeconds(secondsToAdd).format(DatetimeUtils.DATETIME_FORMATTER);
		} catch (Exception ex) {
			return Message.NOT_FOUND_FEMALE.get();
		}
	}

	public static String getNowDatetime() {
		try {
			return ZonedDateTime.now().format(DatetimeUtils.DATETIME_FORMATTER);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static long getSeconds(String datetime) {
		try {
			return ZonedDateTime.parse(datetime, DatetimeUtils.DATETIME_FORMATTER).toEpochSecond();
		} catch (Exception ex) {
			return -1;
		}
	}

	public static int[] getTimeValues(double seconds) {
		int[] values = new int[] { 0, 0, 0, 0, 0, 0, 0 };

		for (int unitIndex = 0; unitIndex <= 5; unitIndex++) {
			int amountForUnit = (int) seconds / DatetimeUtils.SECONDS_IN_UNIT[unitIndex];
			values[unitIndex] += amountForUnit;
			seconds -= amountForUnit * DatetimeUtils.SECONDS_IN_UNIT[unitIndex];
		}

		values[6] += (int) Math.round(seconds);

		return values;
	}

	public static String convertToSentence(double seconds) {
		int[] values = getTimeValues(seconds);

		StringBuilder sentenceBuilder = new StringBuilder();
		for (int valueIndex = 0; valueIndex <= 6; valueIndex++) {
			int value = values[valueIndex];
			if (value <= 0) {
				continue;
			}

			String valueMessage = DatetimeUtils.TIME_UNITS[valueIndex];
			if (value > 1) {
				valueMessage += "S";
			}
			sentenceBuilder.append(value).append(" ").append(Message.valueOf(valueMessage).get()).append(" ");
		}

		int length = sentenceBuilder.length();

		return length > 1 ? sentenceBuilder.deleteCharAt(length - 1).toString() : "0 " + Message.SECOND.get();
	}

	public static String getTimeAgo(String datetime) {
		return convertToSentence(-DatetimeUtils.getSecondsBetweenNowAndDatetime(datetime));
	}

	public static long getSecondsBetweenNowAndDatetime(String datetime) {
		try {
			return Duration
			        .between(ZonedDateTime.now(ConfigUtils.getZoneId()),
			                ZonedDateTime.parse(datetime, DatetimeUtils.DATETIME_FORMATTER))
			        .getSeconds();
		} catch (Exception invalidDate) {
			return -1;
		}
	}

	public static ZonedDateTime getZonedDateTime(String datetime) {
		if (datetime == null) {
			return null;
		}
		try {
			return ZonedDateTime.parse(datetime, DatetimeUtils.DATETIME_FORMATTER);
		} catch (DateTimeParseException e) {
			LOGGER.warn(() -> "getZonedDateTime(): invalid datetime: " + datetime);
			return null;
		}
	}

}
