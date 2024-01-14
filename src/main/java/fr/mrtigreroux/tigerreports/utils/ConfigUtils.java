package fr.mrtigreroux.tigerreports.utils;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;

/**
 * @author MrTigreroux
 */

public class ConfigUtils {
    
    private static final List<String> ACTIVATION_WORDS =
            Arrays.asList("true", "t", "on", "enabled", "yes", "y", "activated", "a");
    
    private ConfigUtils() {}
    
    public static boolean isEnabled(ConfigurationSection config, String path) {
        return config.get(path) != null && ACTIVATION_WORDS.contains(config.getString(path));
    }
    
    public static boolean isEnabled(String path) {
        return isEnabled(ConfigFile.CONFIG.get(), path);
    }
    
    public static char getColorCharacter() {
        return ConfigFile.CONFIG.get().getString("Config.ColorCharacter", "&").charAt(0);
    }
    
    public static String getLineBreakSymbol() {
        return ConfigFile.CONFIG.get().getString("Config.LineBreakSymbol", "//");
    }
    
    public static String getInfoLanguage() {
        return ConfigFile.CONFIG.get().getString("Config.InfoLanguage", "French");
    }
    
    public static String getInfoMessage(String english, String french) {
        return getInfoLanguage().equalsIgnoreCase("English") ? english : french;
    }
    
    public static boolean exists(ConfigurationSection config, String path) {
        return config.get(path) != null;
    }
    
    public static Material getMaterial(ConfigurationSection config, String path) {
        String icon = config.getString(path);
        return icon != null && icon.startsWith("Material-")
                ? Material.matchMaterial(icon.split("-")[1].toUpperCase().replace(":" + getDamage(config, path), "")) : null;
    }
    
    public static short getDamage(ConfigurationSection config, String path) {
        try {
            String icon = config.getString(path);
            return icon != null && icon.startsWith("Material-") && icon.contains(":")
                    ? Short.parseShort(icon.split(":")[1])
                    : 0;
        } catch (Exception noDamage) {
            return 0;
        }
    }
    
    public static String getSkull(ConfigurationSection config, String path) {
        String icon = config.getString(path);
        return icon != null && icon.startsWith("Skull-") ? icon.split("-")[1] : null;
    }
    
    public static boolean playersNotifications() {
        return isEnabled(ConfigFile.CONFIG.get(), "Config.Notifications.Players.Enabled");
    }
    
    public static boolean playersNotificationsHoverableReport() {
        return isEnabled(ConfigFile.CONFIG.get(), "Config.Notifications.Players.HoverableReport");
    }
    
    public static ZoneId getZoneId() {
        String time = ConfigFile.CONFIG.get().getString("Config.Time");
        if (!time.equals("default")) {
            try {
                return ZoneId.of(time);
            } catch (Exception ex) {}
        }
        return ZoneId.systemDefault();
    }
    
    public static boolean isPlaceholderUsedInCommands(ConfigurationSection config, String path,
            String placeholder) {
        List<String> commands = config.getStringList(path);
        if (commands.isEmpty()) {
            return false;
        } else {
            for (String command : commands) {
                if (command.contains(placeholder)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public static void processCommands(ConfigurationSection config, String path, Report r,
            Player staff, VaultManager vm, BungeeManager bm) {
        processCommands(config, path, r, staff, null, vm, bm);
    }
    
    public static void processCommands(ConfigurationSection config, String path, Report r,
            Player staff, String staffReason, VaultManager vm, BungeeManager bm) {
        if (staff == null) {
            Logger.MAIN.warn(
                    () -> ConfigUtils.getInfoMessage(
                            "Could not process commands at <" + path + "> for report #" + r.getId()
                                    + " because staff is unknown.",
                            "Les commandes configurees dans <" + path + "> pour le signalement #"
                                    + r.getId()
                                    + " n'ont pas pu etre traitees car le staff est inconnu."
                    )
            );
            return;
        }
        
        List<String> commands = config.getStringList(path);
        if (commands.isEmpty()) {
            return;
        }
        
        String reported = r.getPlayerName(Report.ParticipantType.REPORTED, false, false, vm, bm);
        String reportId = Integer.toString(r.getId());
        boolean isStaffReasonDefined = staffReason != null && !staffReason.isEmpty();
        
        for (String command : commands) {
            command = command.replace("_Reported_", reported)
                    .replace("_Staff_", staff.getName())
                    .replace("_Id_", reportId)
                    .replace("_Reason_", r.getReason(false));
            if (isStaffReasonDefined) {
                command = command.replace("_StaffReason_", staffReason);
            }
            if (command.contains("_Reporter_")) {
                for (User reporter : r.getReporters()) {
                    executeCommand(staff, command.replace("_Reporter_", reporter.getName()));
                }
            } else {
                executeCommand(staff, command);
            }
        }
    }
    
    public static void executeCommand(Player p, String command) {
        if (command.startsWith("-CONSOLE")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(9));
        } else {
            Bukkit.dispatchCommand(p, command);
        }
    }
    
}
