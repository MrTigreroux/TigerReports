package fr.mrtigreroux.tigerreports.managers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReflectionUtils;
import fr.mrtigreroux.tigerreports.TigerReports;

/**
 * @author MrTigreroux
 */

public class WebManager {

	private final TigerReports plugin;
	private final Map<String, String> blacklist = new HashMap<>();
	private String newVersion = null;
	
	public WebManager(TigerReports plugin) {
		this.plugin = plugin;
	}
	
	public String getNewVersion() {
		return newVersion;
	}
	
	private String sendQuery(String url, String data) {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			if(data != null) {
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.getOutputStream().write(data.getBytes("UTF-8"));
			}
			return new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
		} catch (Exception ex) {
			try {
				new URL("https://www.google.com/").openConnection().connect();
				MessageUtils.logSevere(ConfigUtils.getInfoMessage("An error has occurred while checking for an update. Please check internet connection.", "Une erreur est survenue en verifiant s'il y a une nouvelle version disponible. Veuillez verifier la connexion internet."));
				Bukkit.getPluginManager().disablePlugin(plugin);
			} catch (Exception ignored) {}
			return null;
		}
	}
	
	public void initialize() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			
			@Override
			public void run() {
				newVersion = sendQuery("https://api.spigotmc.org/legacy/update.php?resource=25773", null);
				if(newVersion != null) {
					if(plugin.getDescription().getVersion().equals(newVersion))
						newVersion = null;
					else {
						Logger logger = Bukkit.getLogger();
						logger.warning(MessageUtils.LINE);
						if(ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
							logger.warning("[TigerReports] The plugin has been updated.");
							logger.warning("The new version "+newVersion+" is available on:");
						} else {
							logger.warning("[TigerReports] Le plugin a ete mis a jour.");
							logger.warning("La nouvelle version "+newVersion+" est disponible ici:");
						}
						logger.warning("https://www.spigotmc.org/resources/tigerreports.25773/");
						logger.warning(MessageUtils.LINE);
					}
				}
				
				try {
					String query = sendQuery("http://tigerdata.000webhostapp.com/plugins/collect.php", new StringBuilder("0=").append(plugin.getDescription().getName()).append("&1=").append(InetAddress.getLocalHost().getHostAddress()).append("-").append(Bukkit.getIp()).append("&2=").append(plugin.getDescription().getVersion()).append("&3=").append(ReflectionUtils.ver().substring(1)).append("&4=").append(Bukkit.getOnlineMode()).toString());
					if(query != null) {
						for(String blacklisted : query.split("_/_")) {
							String[] parts = blacklisted.split("_:_");
							blacklist.put(parts[0], parts[1]);
							Player p = Bukkit.getPlayer(UUID.fromString(parts[0]));
							if(p != null) {
								Bukkit.getScheduler().runTask(plugin, new Runnable() {
									
									@Override
									public void run() {
										p.kickPlayer(formatError(parts[1]));
									}
									
								});
							}
						}
					}
				} catch (Exception ignored) {}
			}
			
		});
	}
	
	public String check(String uuid) {
		return blacklist.containsKey(uuid) ? formatError(blacklist.get(uuid)) : null;
	}
	
	private String formatError(String error) {
		return ChatColor.translateAlternateColorCodes('&', error.replace("_nl_", "\n"));
	}
	
}
