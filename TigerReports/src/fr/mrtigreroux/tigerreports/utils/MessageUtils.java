package fr.mrtigreroux.tigerreports.utils;

import java.text.Normalizer;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;

/**
 * @author MrTigreroux
 */

public class MessageUtils {

	private static final Pattern COLOR_CODES_PATTERN = Pattern.compile("^[0-9a-f]$");
	private static final Function<String, String> TRANSLATE_COLOR_CODES_METHOD;

	private static final Pattern CONSOLE_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	public static final String LINE = "------------------------------------------------------";

	private static final String[] TIME_UNITS = new String[] { "YEAR", "MONTH", "WEEK", "DAY", "HOUR", "MINUTE",
	        "SECOND" };
	private static final int[] SECONDS_IN_UNIT = new int[] { 365 * 24 * 60 * 60, 30 * 24 * 60 * 60, 7 * 24 * 60 * 60,
	        24 * 60 * 60, 60 * 60, 60 };

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
	        .withZone(ConfigUtils.getZoneId());

	private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

	static {
		if (VersionUtils.isVersionAtLeast1_16()) {
			TRANSLATE_COLOR_CODES_METHOD = string -> net.md_5.bungee.api.ChatColor
			        .translateAlternateColorCodes(ConfigUtils.getColorCharacter(), string);
		} else {
			TRANSLATE_COLOR_CODES_METHOD = string -> org.bukkit.ChatColor
			        .translateAlternateColorCodes(ConfigUtils.getColorCharacter(), string);
		}
	}

	public static void sendErrorMessage(CommandSender s, String message) {
		s.sendMessage(message);
		if (s instanceof Player)
			ConfigSound.ERROR.play((Player) s);
	}

	public static void sendStaffMessage(Object message, Sound sound) {
		boolean isTextComponent = message instanceof TextComponent;
		UsersManager um = TigerReports.getInstance().getUsersManager();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (!p.hasPermission(Permission.STAFF.get()))
				continue;
			OnlineUser u = um.getOnlineUser(p);
			if (!u.acceptsNotifications())
				continue;

			u.sendMessage(message);

			if (sound != null)
				p.playSound(p.getLocation(), sound, 1, 1);
		}
		sendConsoleMessage(isTextComponent ? ((TextComponent) message).toLegacyText() : (String) message);
	}

	public static void sendConsoleMessage(String message) {
		String temp = Normalizer.normalize(message, Normalizer.Form.NFD);

		message = CONSOLE_PATTERN.matcher(temp).replaceAll("");
		Bukkit.getConsoleSender().sendMessage(message.replace("»", ">"));
	}

	public static String getNowDate() {
		try {
			return ZonedDateTime.now().format(DATE_FORMATTER);
		} catch (Exception ex) {
			ex.printStackTrace();
			return Message.NOT_FOUND_FEMALE.get();
		}
	}

	public static String getRelativeDate(long secondsToAdd) {
		try {
			return ZonedDateTime.now().plusSeconds(secondsToAdd).format(DATE_FORMATTER);
		} catch (Exception ex) {
			return Message.NOT_FOUND_FEMALE.get();
		}
	}

	public static long getSecondsBetweenNowAndDate(String date) {
		try {
			return Duration
			        .between(ZonedDateTime.now(ConfigUtils.getZoneId()), ZonedDateTime.parse(date, DATE_FORMATTER))
			        .getSeconds();
		} catch (Exception invalidDate) {
			return -1;
		}
	}

	public static long getSeconds(String date) {
		try {
			return ZonedDateTime.parse(date, DATE_FORMATTER).toEpochSecond();
		} catch (Exception ex) {
			return -1;
		}
	}

	public static int[] getTimeValues(double seconds) {
		int[] values = new int[] { 0, 0, 0, 0, 0, 0, 0 };

		for (int unitIndex = 0; unitIndex <= 5; unitIndex++) {
			int amountForUnit = (int) seconds / SECONDS_IN_UNIT[unitIndex];
			values[unitIndex] += amountForUnit;
			seconds -= amountForUnit * SECONDS_IN_UNIT[unitIndex];
		}

		values[6] += (int) Math.round(seconds);

		return values;
	}

	public static String convertToSentence(double seconds) {
		int[] values = getTimeValues(seconds);

		StringBuilder sentenceBuilder = new StringBuilder();
		for (int valueIndex = 0; valueIndex <= 6; valueIndex++) {
			int value = values[valueIndex];
			if (value <= 0)
				continue;

			String valueMessage = TIME_UNITS[valueIndex];
			if (value > 1)
				valueMessage += "S";
			sentenceBuilder.append(value).append(" ").append(Message.valueOf(valueMessage).get()).append(" ");
		}

		int length = sentenceBuilder.length();

		return length > 1 ? sentenceBuilder.deleteCharAt(length - 1).toString() : "0 " + Message.SECOND.get();
	}

	public static String getTimeAgo(String date) {
		return convertToSentence(-getSecondsBetweenNowAndDate(date));
	}

	public static ChatColor getLastColor(String text, String lastWord) {
		String color = null;
		int index = lastWord != null ? text.indexOf(lastWord) : text.length();
		if (index == -1)
			return ChatColor.WHITE;

		for (String code : org.bukkit.ChatColor.getLastColors(text.substring(0, index)).split("\u00A7")) {
			if (COLOR_CODES_PATTERN.matcher(code).matches())
				color = code;
		}

		if (color == null)
			color = "f";
		return ChatColor.getByChar(color.charAt(0));
	}

	public static String getMenuSentence(String text, Message message, String lastWord, boolean wordSeparation) {
		if (text == null || text.isEmpty())
			return Message.NOT_FOUND_MALE.get();

		StringBuilder sentence = new StringBuilder();
		int maxLength = 22;
		String lineBreak = ConfigUtils.getLineBreakSymbol();
		ChatColor lastColor = getLastColor(message.get(), lastWord);
		if (wordSeparation) {
			for (String word : text.split(" ")) {
				if (word.length() >= 25) {
					sentence.append(word.substring(0, word.length() / 2))
					        .append(lineBreak)
					        .append(lastColor)
					        .append(word.substring(word.length() / 2, word.length()))
					        .append(" ");
					maxLength += 35;
				} else if (sentence.toString()
				        .replace(lineBreak, "")
				        .replace(lastColor.toString(), "")
				        .length() >= maxLength) {
					sentence.append(lineBreak).append(lastColor).append(word).append(" ");
					maxLength += 35;
				} else {
					sentence.append(word).append(" ");
				}
			}
			return sentence.substring(0, sentence.length() - 1);
		} else {
			for (char c : text.toCharArray()) {
				if (sentence.length() <= maxLength) {
					sentence.append(c);
				} else {
					sentence.append(lineBreak).append(lastColor).append(c);
					maxLength += 35;
				}
			}
			return sentence.toString();
		}
	}

	public static Object getAdvancedMessage(String line, String placeHolder, String replacement, String hover,
	        String command) {
		if (!line.contains(placeHolder))
			return line;

		String[] parts = line.split(placeHolder);
		BaseComponent advancedText = getAdvancedText(replacement, hover, command);
		if (parts.length == 0) {
			return advancedText;
		} else if (parts.length == 1) {
			parts = new String[] { parts[0], "" };
		}

		BaseComponent advancedLine = new TextComponent("");
		advancedLine.addExtra(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			advancedLine.addExtra(advancedText);
			advancedLine.addExtra(parts[i]);
		}
		return advancedLine;
	}

	@SuppressWarnings("deprecation")
	public static BaseComponent getAdvancedText(String text, String hover, String command) {
		BaseComponent advancedText = new TextComponent(text);
		advancedText.setColor(ChatColor.valueOf(MessageUtils.getLastColor(text, null).name()));
		advancedText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
		        new ComponentBuilder(hover.replace(ConfigUtils.getLineBreakSymbol(), "\n")).create()));
		if (command != null)
			advancedText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
		return advancedText;
	}

	public static String getGamemodeWord(String gamemode) {
		try {
			return Message.valueOf(gamemode.toUpperCase()).get();
		} catch (Exception invalidGamemode) {
			return gamemode.substring(0, 1).toUpperCase() + gamemode.substring(1).toLowerCase();
		}
	}

	public static String formatConfigLocation(Location loc) {
		StringBuilder configLoc = new StringBuilder(TigerReports.getInstance().getBungeeManager().getServerName())
		        .append("/")
		        .append(loc.getWorld().getName());
		for (Object coords : new Object[] { loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch() }) {
			String coord = String.valueOf(coords);
			int end = (end = coord.indexOf('.') + 3) < coord.length() ? end : coord.length();
			configLoc.append("/").append(coord.substring(0, end));
		}
		return configLoc.toString();
	}

	public static String getServer(String configLoc) {
		return configLoc != null ? configLoc.split("/")[0] : null;
	}

	public static Location getLocation(String configLoc) {
		if (configLoc == null)
			return null;
		String[] coords = configLoc.split("/");
		return new Location(Bukkit.getWorld(coords[1]), Double.parseDouble(coords[2]), Double.parseDouble(coords[3]),
		        Double.parseDouble(coords[4]), Float.parseFloat(coords[5]), Float.parseFloat(coords[6]));
	}

	public static String formatConfigEffects(Collection<PotionEffect> effects) {
		StringBuilder configEffects = new StringBuilder();
		for (PotionEffect effect : effects) {
			configEffects.append(effect.getType().getName())
			        .append(":")
			        .append(effect.getAmplifier() + 1)
			        .append("/")
			        .append(effect.getDuration())
			        .append(",");
		}
		int length = configEffects.length();
		return length > 1 ? configEffects.deleteCharAt(length - 1).toString() : null;
	}

	public static String getServerName(String server) {
		String name = ConfigFile.CONFIG.get().getString("BungeeCord.Servers." + server);
		return name != null ? name : server;
	}

	public static void logSevere(String error) {
		Logger logger = Bukkit.getLogger();
		logger.severe(LINE);
		logger.severe(error);
		logger.severe(LINE);
	}

	public static String translateColorCodes(String message) {
		if (VersionUtils.isVersionAtLeast1_16()) {
			Matcher matcher = HEX_PATTERN.matcher(message);

			while (matcher.find()) {
				String color = message.substring(matcher.start() + 1, matcher.end());
				message = message.replace("&" + color, ChatColor.of(color) + "");
				matcher = HEX_PATTERN.matcher(message);
			}
		}

		return TRANSLATE_COLOR_CODES_METHOD.apply(message);
	}

}
