package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class PunishmentMenu extends ReportManagerMenu {

	private Map<Integer, String> configIndexes = new HashMap<>();

	public PunishmentMenu(OnlineUser u, int page, int reportId) {
		super(u, 54, page, Permission.STAFF, reportId);
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(
		        Message.PUNISH_TITLE.get().replace("_Reported_", r.getPlayerName("Reported", false, false)), true);

		inv.setItem(0, MenuItem.CANCEL_PROCESS.get());
		inv.setItem(4, MenuItem.PUNISHMENTS.create());
		inv.setItem(8,
		        MenuRawItem.GREEN_CLAY.clone()
		                .name(Message.NO_PUNISHMENT.get())
		                .lore(Message.NO_PUNISHMENT_DETAILS.get()
		                        .replace("_Reported_", r.getPlayerName("Reported", false, true))
		                        .split(ConfigUtils.getLineBreakSymbol()))
		                .create());

		int firstPunishment = 1;
		if (page >= 2) {
			inv.setItem(size - 7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstPunishment += (page - 1) * 27;
		}

		int slot = ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.Punishments.DefaultReasons")
		        ? implement(inv, "Config.DefaultReasons.Reason", firstPunishment, 18)
		        : 18;
		if (slot < 44)
			slot = implement(inv, "Config.Punishments.Punishment", slot == 18 ? firstPunishment : 1, slot);
		if (slot == 45)
			inv.setItem(size - 3, MenuItem.PAGE_SWITCH_NEXT.get());
		return inv;
	}

	/**
	 * @return 45 if it remains punishments that could not be displayed in current
	 *         page, 46 else.
	 */
	private int implement(Inventory inv, String prepath, int firstConfigIndex, int firstSlot) {
		int slot = firstSlot;

		FileConfiguration configFile = ConfigFile.CONFIG.get();
		final String PUNISHMENT_MESSAGE = Message.PUNISHMENT.get();
		final String PUNISHMENT_DETAILS_MESSAGE = Message.PUNISHMENT_DETAILS.get();
		final String LINE_BREAK_SYMBOL = ConfigUtils.getLineBreakSymbol();

		for (int configIndex = firstConfigIndex; configIndex <= firstConfigIndex + 27; configIndex++) {
			String path = prepath + configIndex;
			boolean punishCommandsExists = ConfigUtils.exist(configFile, path + ".PunishCommands");
			if (slot > 44)
				return punishCommandsExists ? 45 : 46;
			if (!ConfigUtils.exist(configFile, path))
				break;
			if (!punishCommandsExists)
				continue;
			String permission = configFile.getString(path + ".PunishCommandsPermission");
			if (permission != null && !p.hasPermission(permission))
				continue;

			String punishment = configFile.getString(path + ".Name");
			String lore = configFile.getString(path + ".Lore");
			inv.setItem(slot, new CustomItem().fromConfig(configFile, path + ".Item")
			        .name(PUNISHMENT_MESSAGE.replace("_Punishment_", punishment))
			        .lore(PUNISHMENT_DETAILS_MESSAGE.replace("_Reported_", r.getPlayerName("Reported", false, true))
			                .replace("_Punishment_", punishment)
			                .replace("_Lore_", MessageUtils.translateColorCodes(lore != null ? lore : ""))
			                .split(LINE_BREAK_SYMBOL))
			        .hideFlags(true)
			        .create());
			int len = prepath.length();
			configIndexes.put(slot, prepath.substring(len - 1) + configIndex);
			slot++;
		}
		return slot;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openReportMenu(r.getId());
		} else if (slot == 8) {
			r.process(p.getUniqueId().toString(), "True", false, Permission.STAFF_ARCHIVE_AUTO.isOwned(u), true);
			u.openDelayedlyReportsMenu();
		} else if (slot >= 18 && slot <= size - 9) {
			ConfigSound.MENU.play(p);
			String configIndex = configIndexes.get(slot);
			String path = (configIndex.charAt(0) == 't' ? "Config.Punishments.Punishment"
			        : "Config.DefaultReasons.Reason") + configIndex.substring(1);

			FileConfiguration configFile = ConfigFile.CONFIG.get();
			String reported = r.getPlayerName("Reported", false, false);
			String reportId = Integer.toString(r.getId());
			String[] reportersUniqueIds = r.getReportersUniqueIds();
			for (String command : configFile.getStringList(path + ".PunishCommands")) {
				command = command.replace("_Reported_", reported)
				        .replace("_Staff_", p.getName())
				        .replace("_Id_", reportId)
				        .replace("_Reason_", r.getReason(false));
				if (command.contains("_Reporter_")) {
					for (String uuid : reportersUniqueIds)
						executePunishCommand(p, command.replace("_Reporter_", UserUtils.getName(uuid)));
				} else {
					executePunishCommand(p, command);
				}
			}

			r.processPunishing(p.getUniqueId().toString(), false, Permission.STAFF_ARCHIVE_AUTO.isOwned(u),
			        configFile.getString(path + ".Name"), true);
			u.openDelayedlyReportsMenu();
		}
	}

	private void executePunishCommand(Player p, String command) {
		if (command.startsWith("-CONSOLE")) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(9));
		} else {
			Bukkit.dispatchCommand(p, command);
		}
	}

}
