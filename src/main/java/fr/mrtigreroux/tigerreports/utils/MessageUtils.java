package fr.mrtigreroux.tigerreports.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author MrTigreroux
 */

public class MessageUtils {

    private static final Logger LOGGER = Logger.fromClass(MessageUtils.class);
    private static final Pattern COLOR_CODES_PATTERN = Pattern.compile("^[0-9a-f]$");
    private static final Pattern HEX_CODES_PATTERN = Pattern.compile("§x(§[a-fA-F0-9]){6}");
    private static final Function<String, String> TRANSLATE_COLOR_CODES_METHOD;
    public static final BiConsumer<BaseComponent, String> APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD;

    private static final Pattern CONSOLE_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static final String LINE = "------------------------------------------------------";

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    static {
        if (VersionUtils.isVersionAtLeast1_16()) {
            TRANSLATE_COLOR_CODES_METHOD = string -> net.md_5.bungee.api.ChatColor
                    .translateAlternateColorCodes(ConfigUtils.getColorCharacter(), string);
            APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD = (tc, text) -> tc
                    .addExtra(new TextComponent(TextComponent.fromLegacyText(text)));
        } else {
            TRANSLATE_COLOR_CODES_METHOD = string -> org.bukkit.ChatColor
                    .translateAlternateColorCodes(ConfigUtils.getColorCharacter(), string);
            APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD =
                    (bc, text) -> bc.addExtra(text);
        }
    }

    private MessageUtils() {
    }

    public static void sendErrorMessage(CommandSender s, String message) {
        s.sendMessage(message);
        if (s instanceof Player) {
            ConfigSound.ERROR.play((Player) s);
        }
    }

    public static void sendStaffMessage(Object message, Sound sound) {
        boolean isTextComponent = message instanceof TextComponent;
        UsersManager um = TigerReports.getInstance().getUsersManager();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission(Permission.STAFF.get())) {
                continue;
            }
            User u = um.getOnlineUser(p);
            if (u == null) {
                LogUtils.logUnexpectedOfflineUser(LOGGER, "sendStaffMessage()", p);
                continue;
            }
            if (!u.acceptsNotifications()) {
                continue;
            }

            u.sendMessage(message);

            if (sound != null) {
                p.playSound(p.getLocation(), sound, 1, 1);
            }
        }
        sendConsoleMessage(
                isTextComponent ? ((TextComponent) message).toLegacyText() : (String) message
        );
    }

    public static void sendConsoleMessage(String message) {
        String temp = Normalizer.normalize(message, Normalizer.Form.NFD);

        message = CONSOLE_PATTERN.matcher(temp).replaceAll("");
        Bukkit.getConsoleSender().sendMessage(message.replace("�", ">"));
    }

    public static ChatColor getLastColor(String text, String lastWord) {
        ChatColor color = ChatColor.WHITE;
        int index = lastWord != null ? text.indexOf(lastWord) : text.length();
        if (index == -1) {
            return ChatColor.WHITE;
        }

        String codes = deserializeHex(org.bukkit.ChatColor.getLastColors(text.substring(0, index)));

        for (String code : codes.replace("#", "§").split("§")) {
            if (code.length() == 6) {
                color = ChatColor.of("#" + code);
            } else if (COLOR_CODES_PATTERN.matcher(code).matches()) {
                color = ChatColor.getByChar(code.charAt(0));
            }
        }

        return color;
    }

    private static String deserializeHex(String message) {
        Matcher matcher = HEX_CODES_PATTERN.matcher(message);

        while (matcher.find()) {
            String serializedHex = message.substring(matcher.start(), matcher.end());
            String deserializedHex = "#" + serializedHex.substring(2, 14).replace("§", "");
            message = message.replace(serializedHex, deserializedHex);
            matcher = HEX_CODES_PATTERN.matcher(message);
        }

        return message;
    }

    public static String getMenuSentence(String text, Message message, String lastWord,
                                         boolean wordSeparation) {
        if (text == null || text.isEmpty()) {
            return Message.NOT_FOUND_MALE.get();
        }

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
                } else if (
                        sentence.toString()
                                .replace(lineBreak, "")
                                .replace(lastColor.toString(), "")
                                .length() >= maxLength
                ) {
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

    public static Object getAdvancedMessage(String line, String placeHolder, String replacement,
                                            String hover, String command) {
        if (!line.contains(placeHolder)) {
            return line;
        }

        String[] parts = line.split(placeHolder);
        BaseComponent advancedText = getAdvancedText(replacement, hover, command);
        if (parts.length == 0) {
            return advancedText;
        } else if (parts.length == 1) {
            parts = new String[]{
                    parts[0], ""
            };
        }

        BaseComponent advancedLine = new TextComponent("");
        APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD
                .accept(advancedLine, parts[0]);
        for (int i = 1; i < parts.length; i++) {
            advancedLine.addExtra(advancedText);
            APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD
                    .accept(advancedLine, parts[i]);
        }
        return new TextComponent(advancedLine);
    }

    @SuppressWarnings("deprecation")
    public static BaseComponent getAdvancedText(String text, String hover, String command) {
        BaseComponent advancedText = new TextComponent("");
        advancedText.setColor(ChatColor.valueOf(MessageUtils.getLastColor(text, null).name()));
        APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD
                .accept(advancedText, text);

        BaseComponent hoverTC = new TextComponent("");
        APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD
                .accept(hoverTC, hover.replace(ConfigUtils.getLineBreakSymbol(), "\n"));
        advancedText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                hoverTC
        }));
        if (command != null) {
            advancedText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        }
        return new TextComponent(advancedText);
    }

    public static String getServerName(String server) {
        String name = ConfigFile.CONFIG.get().getString("BungeeCord.Servers." + server);
        return name != null ? name : server;
    }

    public static String translateColorCodes(String message) {
        if (VersionUtils.isVersionAtLeast1_16()) {
            Matcher matcher = HEX_PATTERN.matcher(message);

            while (matcher.find()) {
                String color = message.substring(matcher.start(), matcher.end());
                message = message.replace(color, ChatColor.of(color) + "");
                matcher = HEX_PATTERN.matcher(message);
            }
        }

        return TRANSLATE_COLOR_CODES_METHOD.apply(message);
    }

    public static <T> String joinElements(String separator, T[] elements,
                                          boolean keepNullElements) {
        return joinElements(
                separator,
                elements != null ? Arrays.asList(elements) : null,
                keepNullElements
        );
    }

    public static <T> String joinElements(String separator, Collection<T> elements,
                                          boolean keepNullElements) {
        if (elements == null || elements.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean firstElementAdded = false;
        for (T ele : elements) {
            if (!keepNullElements && ele == null) {
                continue;
            }

            if (firstElementAdded) {
                sb.append(separator);
            } else {
                firstElementAdded = true;
            }
            sb.append(ele);
        }
        return sb.toString();
    }

}
