package fr.mrtigreroux.tigerreports.utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.ConfigSound;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;

/**
 * @author MrTigreroux
 */

public class MessageUtils {

	private static final List<String> units = Arrays.asList("YEAR", "MONTH", "WEEK", "DAY", "HOUR", "MINUTE", "SECOND");
	private static final Set<String> colorCodes = new HashSet<String>(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"));
	
	public static void sendErrorMessage(CommandSender s, String message) {
		s.sendMessage(message);
		if(!(s instanceof Player)) return;
		Player p = (Player) s;
		p.playSound(p.getLocation(), ConfigSound.ERROR.get(), 1, 1);
	}

	public static void sendStaffMessage(Object message, Sound sound) {
		boolean isTextComponent = message instanceof TextComponent;
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(!p.hasPermission(Permission.STAFF.get()) || !UserUtils.getUser(p).acceptsNotifications()) continue;
			if(isTextComponent) p.spigot().sendMessage((TextComponent) message);
			else p.sendMessage((String) message);
			if(sound != null) p.playSound(p.getLocation(), sound, 1, 1);
		}
		if(isTextComponent) sendConsoleMessage(((TextComponent) message).toLegacyText());
		else sendConsoleMessage((String) message);
	}

	public static void sendConsoleMessage(String message) {
		Bukkit.getConsoleSender().sendMessage(message.replace("é", "e").replace("è", "e").replace("à", "a").replace("»", ">").replace("ç", "c").replace("î", "i").replace("ê", "e").replace("û", "u"));
	}

	public static String cleanDouble(Double number) {
		String process = number+" ";
		String result = process.replace(".0 ", "").replace(" ", "");
		if(result.contains("E")) {
			double power = Math.pow(10D, Double.parseDouble(result.substring(result.indexOf("E")+1)));
			double withoutPower = Double.parseDouble(result.substring(0, result.indexOf("E")));
			long total = (long) (withoutPower * power);
			result = ""+total;
		}
		return result;
	}
	
	public static String getNowDate() {
		return new SimpleDateFormat("dd/MM/yyyy HH-mm-ss").format(new Date());
	}
	
	public static String getDateArrangedByHeight(String date) {
		date = date.replace("/", "").replace("-", "").replace(" ", "").replace(":", "");
		return date.substring(4, 8)+date.substring(2, 4)+date.substring(0, 2)+date.substring(8, 10)+date.substring(10, 12)+date.substring(12, 14);
	}

	public static Double getSeconds(String date) {
		date = date.replace("/", "").replace(":", "").replace("-", "").replace(" ", "");
		return Double.parseDouble(date.substring(4, 8))*(365*24*60*60)+Double.parseDouble(date.substring(0, 2))*(24*60*60)+Double.parseDouble(date.substring(2, 4))*(30*24*60*60)+Double.parseDouble(date.substring(8, 10))*(60*60)+Double.parseDouble(date.substring(10, 12))*60+Double.parseDouble(date.substring(12, 14));
	}
	
	public static List<Integer> getValues(double seconds) {
		List<Integer> values = Arrays.asList(0, 0, 0, 0, 0, 0, 0);
		
		while(seconds/(365*24*60*60) >= 1) {
			values.set(0, values.get(0)+1);
			seconds -= (365*24*60*60);
		}
		while(seconds/(30*24*60*60) >= 1) {
			values.set(1, values.get(1)+1);
			seconds -= 30*24*60*60;
		}
		while(seconds/(7*24*60*60) >= 1) {
			values.set(2, values.get(2)+1);
			seconds -= 7*24*60*60;
		}
		while(seconds/(24*60*60) >= 1) {
			values.set(3, values.get(3)+1);
			seconds -= 24*60*60;
		}
		while(seconds/(60*60) >= 1) {
			values.set(4, values.get(4)+1);
			seconds -= 60*60;
		}
		while(seconds/60 >= 1) {
			values.set(5, values.get(5)+1);
			seconds -= 60;
		}
		values.set(6, values.get(6)+Integer.parseInt(cleanDouble(seconds)));
		
		return values;
	}
	
	public static String convertToSentence(double seconds) {
		List<Integer> values = getValues(seconds);
		
		String sentence = "";
		for(int valueNumber = 0; valueNumber <= 6; valueNumber++) {
			switch(values.get(valueNumber)) {
				case 0: break;
				case 1: sentence += 1+" "+Message.valueOf(units.get(valueNumber)).get()+" "; break;
				default: sentence += values.get(valueNumber)+" "+Message.valueOf(units.get(valueNumber)+"S").get()+" "; break;
			}
		}
		if(sentence.endsWith(" ")) sentence = sentence.substring(0, sentence.length()-1);
		return sentence;
	}
	
	public static String convertToDate(double seconds) {
		List<Integer> values = getValues(seconds);
		values = Arrays.asList(values.get(2)*7+values.get(3), values.get(1), values.get(0), values.get(4), values.get(5), values.get(6));
		String date = "";
		for(int valueNumber = 0; valueNumber <= 5; valueNumber++) {
			String value = ""+values.get(valueNumber);
			if(value.length() < 2) value = "0"+value;
			date += (valueNumber == 0 ? "" : valueNumber <= 2 ? "/" : valueNumber == 3 ? " " : "-")+value;
		}
		return date;
	}
	public static ChatColor getLastColor(String text, String lastWord) {
		String color = null;
		int index = lastWord != null ? text.indexOf(lastWord) : text.length();
		if(index == 0) return ChatColor.WHITE;
		for(String code : org.bukkit.ChatColor.getLastColors(text.substring(0, index)).split("§")) if(colorCodes.contains(code)) color = code;
		if(color == null) color = "f";
		return ChatColor.getByChar(color.charAt(0));
	}
	
	public static String getMenuSentence(String text, Message message, String lastWord, boolean wordSeparation) {
		if(text == null || text.isEmpty()) return Message.NOT_FOUND_MALE.get();
		String sentence = "";
		int maxLength = 22;
		String lineBreak = ConfigUtils.getLineBreakSymbol();
		ChatColor lastColor = getLastColor(message.get(), lastWord);
		if(wordSeparation) {
			for(String word : text.split(" ")) {
				if(word.length() >= 25) {
					sentence += word.substring(0, word.length()/2)+lineBreak+lastColor+word.substring(word.length()/2, word.length())+" ";
					maxLength += 35;
				} else if(sentence.replace(lineBreak, "").replace(lastColor.toString(), "").length() >= maxLength) {
					sentence += lineBreak+lastColor+word+" ";
					maxLength += 35;
				} else sentence += word+" ";
			}
			return sentence.substring(0, sentence.length()-1);
		} else {
			for(char c : text.toCharArray()) {
				if(sentence.length() <= maxLength) sentence += c;
				else {
					sentence += lineBreak+lastColor+c;
					maxLength += 35;
				}
			}
			return sentence;
		}
	}
	
	public static Object getAdvancedMessage(String line, String placeHolder, String replacement, String hover, String command) {
		if(!line.contains(placeHolder)) return line;
		else {
			TextComponent advancedLine = new TextComponent("");
			for(String part : line.replace(placeHolder, "¤µ¤"+placeHolder+"¤µ¤").split("¤µ¤")) {
				if(!part.equals(placeHolder)) advancedLine.addExtra(part);
				else {
					TextComponent advancedText = new TextComponent(replacement);
					advancedText.setColor(ChatColor.valueOf(MessageUtils.getLastColor(replacement, null).name()));
					advancedText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover.replace(ConfigUtils.getLineBreakSymbol(), "\n")).create()));
					if(command != null) advancedText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
					advancedLine.addExtra(advancedText);
				} 
			}
			return advancedLine;
		}
	}
	
	public static String getGamemodeWord(String gamemode) {
		try {
			return Message.valueOf(gamemode.toUpperCase()).get();
		} catch(Exception invalidGamemode) {
			return gamemode.substring(0, 1).toUpperCase()+gamemode.substring(1, gamemode.length()).toLowerCase();
		}
	}
	
}
