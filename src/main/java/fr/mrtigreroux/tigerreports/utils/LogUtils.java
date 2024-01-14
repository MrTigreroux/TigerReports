package fr.mrtigreroux.tigerreports.utils;

import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.logs.Logger;

/**
 * @author MrTigreroux
 */
public class LogUtils {
    
    private LogUtils() {}
    
    public static void logUnexpectedOfflineUser(Logger logger, String context, Player p) {
        logger.warn(
                () -> context + ": unexpected offline user, player name = "
                        + (p != null ? p.getName() : "<null>")
        );
    }
    
}
