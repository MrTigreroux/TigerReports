package fr.mrtigreroux.tigerreports.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import net.milkbowl.vault.chat.Chat;

/**
 * @author MrTigreroux
 */

public class VaultManager {

	private final boolean enabled;
	private Chat chat = null;

	private boolean displayForStaff = false;
	private boolean displayForPlayers = false;

	private final Map<UUID, String> displayNames = new HashMap<>();

	public VaultManager(boolean isVaultInstalled) {
		enabled = isVaultInstalled && ConfigUtils.isEnabled("VaultChat.Enabled");
		if (enabled) {
			initialize();
		}
	}

	private void initialize() {
		setupChat();
		FileConfiguration configFile = ConfigFile.CONFIG.get();
		displayForStaff = ConfigUtils.isEnabled(configFile, "VaultChat.DisplayForStaff");
		displayForPlayers = ConfigUtils.isEnabled(configFile, "VaultChat.DisplayForPlayers");
	}

	private boolean setupChat() {
		if (!enabled) {
			return false;
		}
		if (chat != null) {
			return true;
		}

		RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);

		if (rsp != null) {
			chat = rsp.getProvider();
			if (chat != null) {
				Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage(
				        "The plugin is using the prefixes and suffixes from the chat of Vault plugin to display player names.",
				        "Le plugin utilise les prefixes et suffixes du chat du plugin Vault pour afficher les noms des joueurs."));
				return true;
			}
		}

		Logger.CONFIG.error(ConfigUtils.getInfoMessage("The Chat of Vault plugin could not be used.",
		        "Le chat du plugin Vault n'a pas pu etre utilise."));
		return false;
	}

	public interface DisplayNameResultCallback {
		void onDisplayNameReceived(String displayName);
	}

	public void getPlayerDisplayNameAsynchronously(OfflinePlayer p, boolean staff, TaskScheduler taskScheduler,
	        DisplayNameResultCallback resultCallback) {
		if (!useVaultDisplayName(staff)) {
			resultCallback.onDisplayNameReceived(p.getName());
		}

		if (!p.isOnline()) {
			UUID uuid = p.getUniqueId();
			String lastDisplayName = displayNames.get(uuid);
			if (lastDisplayName != null) {
				resultCallback.onDisplayNameReceived(lastDisplayName);
			} else {
				getVaultDisplayNameAsynchronously(p, taskScheduler, resultCallback);
			}
		} else {
			resultCallback.onDisplayNameReceived(getVaultDisplayName(p));
		}
	}

	public void getVaultDisplayNameAsynchronously(OfflinePlayer p, TaskScheduler taskScheduler,
	        DisplayNameResultCallback resultCallback) {
		taskScheduler.runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				String displayName = getVaultDisplayName(p);
				taskScheduler.runTask(new Runnable() {

					@Override
					public void run() {
						resultCallback.onDisplayNameReceived(displayName);
					}

				});
			}

		});
	}

	/**
	 * Must be accessed asynchronously if player is offline
	 */
	private String getVaultDisplayName(OfflinePlayer p) {
		String name = p.getName();
		if (name == null || !setupChat()) {
			return null;
		}

		String playerPrefix = (playerPrefix = chat.getPlayerPrefix(null, p)) != null ? playerPrefix : "";
		String playerSuffix = (playerSuffix = chat.getPlayerSuffix(null, p)) != null ? playerSuffix : "";
		String vaultDisplayName = MessageUtils.translateColorCodes(ConfigFile.CONFIG.get()
		        .getString("VaultChat.Format")
		        .replace("_Prefix_", playerPrefix)
		        .replace("_Name_", name)
		        .replace("_Suffix_", playerSuffix));

		displayNames.put(p.getUniqueId(), vaultDisplayName);
		return vaultDisplayName;
	}

	public boolean useVaultDisplayName(boolean staff) {
		return ((staff && displayForStaff) || (!staff && displayForPlayers)) && setupChat();
	}

	public String getOnlinePlayerDisplayName(Player p) {
		return setupChat() ? getVaultDisplayName(p) : p.getDisplayName();
	}

	public String getOnlinePlayerDisplayName(Player p, boolean staff) {
		return useVaultDisplayName(staff) ? getVaultDisplayName(p) : p.getDisplayName();
	}

}
