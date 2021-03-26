package fr.mrtigreroux.tigerreports.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

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

	public String getPlayerDisplayName(OfflinePlayer p, boolean staff) {
		return ((staff && displayForStaff) || (!staff && displayForPlayers)) && setupChat() ? MessageUtils
		        .translateColorCodes(chat.getPlayerPrefix(null, p) + p.getName() + chat.getPlayerSuffix(null, p))
		        : p.getName();
	}

	public String getPlayerDisplayName(Player p) {
		return setupChat()
		        ? MessageUtils.translateColorCodes(
		                chat.getPlayerPrefix(null, p) + p.getName() + chat.getPlayerSuffix(null, p))
		        : p.getDisplayName();
	}

}
