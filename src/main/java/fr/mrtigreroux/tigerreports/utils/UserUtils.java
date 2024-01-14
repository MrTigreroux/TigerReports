package fr.mrtigreroux.tigerreports.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.logs.Logger;
import com.google.common.collect.Iterables;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;

/**
 * @author MrTigreroux
 */

public class UserUtils {
    
    private static final Logger LOGGER = Logger.fromClass(UserUtils.class);
    
    private UserUtils() {}
    
    public static UUID getOnlinePlayerUniqueId(String name) {
        try {
            return Bukkit.getPlayer(name).getUniqueId();
        } catch (Exception offlinePlayer) {
            return null;
        }
    }
    
    public static boolean useDisplayName(boolean staff) {
        FileConfiguration configFile = ConfigFile.CONFIG.get();
        boolean displayForStaff = ConfigUtils.isEnabled(configFile, "Config.DisplayNameForStaff");
        boolean displayForPlayers =
                ConfigUtils.isEnabled(configFile, "Config.DisplayNameForPlayers");
        LOGGER.debug(
                () -> "useDisplayName(" + staff + "): displayForStaff = " + displayForStaff
                        + ", displayForPlayers = " + displayForPlayers
        );
        return (staff && displayForStaff) || (!staff && displayForPlayers);
    }
    
    public static boolean checkPlayer(CommandSender s) {
        if (!(s instanceof Player)) {
            s.sendMessage(Message.PLAYER_ONLY.get());
            return false;
        } else {
            return true;
        }
    }
    
    public static Player getPlayerFromUniqueId(UUID uuid) {
        try {
            return Bukkit.getPlayer(uuid);
        } catch (Exception offlinePlayer) {
            return null;
        }
    }
    
    /**
     * Returns the players that player p can see (not vanished). Doesn't take in consideration
     * vanished players on a different server, who are therefore considered as online for the player
     * p, because no official check can be used.
     * 
     * @param p            - Viewer of players
     * @param hideExempted - Hide players owning tigerreports.report.exempt permission
     */
    public static List<String> getOnlinePlayersForPlayer(Player p, boolean hideExempted,
            UsersManager um, BungeeManager bm) {
        List<String> players = bm.getOnlinePlayers();
        
        if (players == null) {
            players = new ArrayList<>();
            for (Player plr : Bukkit.getOnlinePlayers()) {
                if (p.canSee(plr)) {
                    players.add(plr.getName());
                }
            }
        }
        
        if (hideExempted) {
            players.removeAll(um.getExemptedPlayers());
        }
        
        return players;
    }
    
    public static String getStaffDisplayName(User u, VaultManager vm) {
        String name;
        if (u == null) {
            name = Message.NOT_FOUND_MALE.get();
        } else {
            name = u.getDisplayName(vm, true);
        }
        return name;
    }
    
    public static Player getRandomPlayer() {
        return Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
    }
    
}
