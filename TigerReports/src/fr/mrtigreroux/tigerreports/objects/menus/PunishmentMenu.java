package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class PunishmentMenu extends ReportManagerMenu {

	private String reported = r.getPlayerName("Reported", false, false);
	private Map<Integer, String> configIndexes = new HashMap<>();

	public PunishmentMenu(OnlineUser u, int page, Report report) {
		super(u, 54, page, Permission.STAFF, report);
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.PUNISH_TITLE.get().replace("_Reported_", reported), true);

		inv.setItem(0, MenuItem.CANCEL_PROCESS.get());
		inv.setItem(4, MenuItem.PUNISHMENTS.get());
		inv.setItem(8, new CustomItem().type(Material.STAINED_CLAY)
				.damage((byte) 5)
				.name(Message.NO_PUNISHMENT.get())
				.lore(Message.NO_PUNISHMENT_DETAILS.get().replace("_Reported_", reported).split(ConfigUtils.getLineBreakSymbol()))
				.create());

		int firstPunishment = 1;
		if (page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstPunishment += (page-1)*27;
		}

		int slot = ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.Punishments.DefaultReasons") ? implement(inv,
				"Config.DefaultReasons.Reason", firstPunishment, 18) : 18;
		if (slot < 44)
			slot = implement(inv, "Config.Punishments.Punishment", slot == 18 ? firstPunishment : 1, slot);
		if (slot == 45)
			inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		return inv;
	}

	private int implement(Inventory inv, String prepath, int firstConfigIndex, int firstSlot) {
		int slot = firstSlot;
		for (int configIndex = firstConfigIndex; configIndex <= firstConfigIndex+27; configIndex++) {
			String path = prepath+configIndex;
			if (slot > 44)
				return ConfigUtils.exist(ConfigFile.CONFIG.get(), path+".PunishCommands") ? 45 : 46;
			if (!ConfigUtils.exist(ConfigFile.CONFIG.get(), path))
				break;
			if (!ConfigUtils.exist(ConfigFile.CONFIG.get(), path+".PunishCommands"))
				continue;
			Material material = ConfigUtils.getMaterial(ConfigFile.CONFIG.get(), path+".Item");
			String punishment = ConfigFile.CONFIG.get().getString(path+".Name");
			inv.setItem(slot, new CustomItem().type(material != null ? material : Material.PAPER)
					.damage(ConfigUtils.getDamage(ConfigFile.CONFIG.get(), path+".Item"))
					.skullOwner(ConfigUtils.getSkull(ConfigFile.CONFIG.get(), path+".Item"))
					.name(Message.PUNISHMENT.get().replace("_Punishment_", punishment))
					.lore(Message.PUNISHMENT_DETAILS.get()
							.replace("_Reported_", reported)
							.replace("_Punishment_", punishment)
							.replace("_Lore_", ChatColor.translateAlternateColorCodes(ConfigUtils.getColorCharacter(), ConfigFile.CONFIG.get()
									.getString(path+".Lore")))
							.split(ConfigUtils.getLineBreakSymbol()))
					.hideFlags(true)
					.create());
			int len = prepath.length();
			configIndexes.put(slot, prepath.substring(len-1)+configIndex);
			slot++;
		}
		return slot;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openReportMenu(r);
		} else if (slot == 8) {
			r.process(p.getUniqueId().toString(), p.getName(), "True", false, Permission.STAFF_ARCHIVE_AUTO.isOwned(u));
			u.openDelayedlyReportsMenu();
		} else if (slot >= 18 && slot <= size-9) {
			ConfigSound.MENU.play(p);
			String configIndex = configIndexes.get(slot);
			String path = (configIndex.charAt(0) == 't' ? "Config.Punishments.Punishment" : "Config.DefaultReasons.Reason")+configIndex.substring(1);

			for (String command : ConfigFile.CONFIG.get().getStringList(path+".PunishCommands")) {
				command = command.replace("_Player_", reported).replace("_Staff_", p.getName()).replace("_Id_", Integer.toString(r.getId()));
				if (command.startsWith("-CONSOLE")) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(9));
				} else {
					Bukkit.dispatchCommand(p, command);
				}
			}

			r.processPunishing(p.getUniqueId().toString(), p.getName(), false, Permission.STAFF_ARCHIVE_AUTO.isOwned(u), ConfigFile.CONFIG.get()
					.getString(path+".Name"));
			u.openDelayedlyReportsMenu();
		}
	}

}
