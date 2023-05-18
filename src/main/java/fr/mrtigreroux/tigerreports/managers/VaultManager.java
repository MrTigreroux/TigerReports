package fr.mrtigreroux.tigerreports.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;
import net.milkbowl.vault.chat.Chat;

/**
 * @author MrTigreroux
 */

public class VaultManager {

	private static final Logger LOGGER = Logger.fromClass(VaultManager.DisplayNameResultCallback.class);

	private final boolean enabled;
	private Chat chat = null;

	private final Map<UUID, String> displayNames = new HashMap<>();

	public VaultManager(boolean isVaultInstalled) {
		enabled = isVaultInstalled && ConfigUtils.isEnabled("VaultChat.Enabled");
		setupChat();
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

	// TODO Unused
	public void getPlayerDisplayNameAsynchronously(OfflinePlayer p, boolean staff, TaskScheduler taskScheduler,
	        DisplayNameResultCallback resultCallback) {
		if (!UserUtils.useDisplayName(staff)) {
			resultCallback.onDisplayNameReceived(p.getName());
		}

		if (!p.isOnline()) {
			UUID uuid = p.getUniqueId();
			String lastDisplayName = displayNames.get(uuid);
			if (lastDisplayName != null) {
				resultCallback.onDisplayNameReceived(lastDisplayName);
			} else {
				// should add String name as param and use it
				getVaultDisplayNameAsynchronously(p, null, taskScheduler, resultCallback);
			}
		} else {
			resultCallback.onDisplayNameReceived(getVaultDisplayName(p, null));
		}
	}

	public void getVaultDisplayNameAsynchronously(OfflinePlayer p, String name, TaskScheduler taskScheduler,
	        DisplayNameResultCallback resultCallback) {
		taskScheduler.runTaskAsynchronously(() -> {
			String displayName = getVaultDisplayName(p, name);
			taskScheduler.runTask(() -> {
				resultCallback.onDisplayNameReceived(displayName);
			});
		});
	}

	/**
	 * Must be accessed asynchronously if player is offline
	 */
	private String getVaultDisplayName(OfflinePlayer p, String name) {
		if (name == null) {
			name = p.getName();
		}
		if (name == null || !setupChat()) {
			final String fname = name;
			LOGGER.info(() -> "getVaultDisplayName(" + p + ", name=" + fname + "): return null");
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

	public String getOnlinePlayerDisplayName(Player p) {
		return setupChat() ? getVaultDisplayName(p, p.getName()) : p.getDisplayName();
	}

}
