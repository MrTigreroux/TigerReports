package fr.mrtigreroux.tigerreports.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import net.milkbowl.vault.chat.Chat;

/**
 * @author MrTigreroux
 */

public class VaultManager {

	private boolean isVaultInstalled;
	private Chat chat = null;

	private boolean displayForStaff = false;
	private boolean displayForPlayers = false;

	private Map<String, String> displayNames = new HashMap<>();

	public VaultManager(boolean isVaultInstalled) {
		this.isVaultInstalled = isVaultInstalled;
		load();
	}

	public void load() {
		if (setupChat()) {
			displayForStaff = ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "VaultChat.DisplayForStaff");
			displayForPlayers = ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "VaultChat.DisplayForPlayers");
		}
	}

	private boolean isEnabled() {
		return isVaultInstalled && ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "VaultChat.Enabled");
	}

	private boolean setupChat() {
		if (!isEnabled())
			return false;
		if (chat != null)
			return true;

		RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		if (chat == null) {
			MessageUtils.logSevere(ConfigUtils.getInfoMessage("The Chat of Vault plugin could not be used.",
			        "Le chat du plugin Vault n'a pas pu etre utilise."));
			return false;
		}

		Bukkit.getLogger()
		        .info(ConfigUtils.getInfoMessage(
		                "The plugin is using the prefixes and suffixes from the chat of Vault plugin to display player names.",
		                "Le plugin utilise les prefixes et suffixes du chat du plugin Vault pour afficher les noms des joueurs."));
		return true;
	}

	/**
	 * Must be accessed asynchronously if player is offline
	 */
	private String getVaultDisplayName(OfflinePlayer p) {
		String vaultDisplayName = MessageUtils
		        .translateColorCodes(chat.getPlayerPrefix(null, p) + p.getName() + chat.getPlayerSuffix(null, p));
		displayNames.put(p.getUniqueId().toString(), vaultDisplayName);
		return vaultDisplayName;
	}

	public String getPlayerDisplayName(OfflinePlayer p, boolean staff) {
		if (!(((staff && displayForStaff) || (!staff && displayForPlayers)) && setupChat())) {
			return p.getName();
		}

		if (!p.isOnline()) {
			String uuid = p.getUniqueId().toString();
			String lastDisplayName = displayNames.get(uuid);
			if (lastDisplayName != null) {
				return lastDisplayName;
			}

			Bukkit.getScheduler().runTaskAsynchronously(TigerReports.getInstance(), new Runnable() {

				@Override
				public void run() {
					getVaultDisplayName(p); // Collected and saved for next time.
				}

			});

			return p.getName();
		}

		return getVaultDisplayName(p);
	}

	public String getOnlinePlayerDisplayName(Player p) {
		return setupChat() ? getVaultDisplayName(p) : p.getDisplayName();
	}

}
