package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ProcessMenu extends ReportManagerMenu {

	public ProcessMenu(OnlineUser u, Report report) {
		super(u, 27, 0, Permission.STAFF, report);
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.PROCESS_TITLE.get().replace("_Report_", r.getName()), false);

		ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name(" ").create();
		for (int position : new Integer[] {1, 2, 3, 4, 5, 6, 7, 10, 16, 19, 20, 21, 22, 23, 24, 25})
			inv.setItem(position, gui);

		inv.setItem(0, r.getItem(null));

		for (String appreciation : Arrays.asList("TRUE", "UNCERTAIN", "FALSE")) {
			String appreciationWord = Message.valueOf(appreciation).get();
			inv.setItem(appreciation.equals("TRUE") ? 11 : appreciation.equals("UNCERTAIN") ? 13 : 15, new CustomItem().type(Material.STAINED_CLAY)
					.damage((byte) (appreciation.equals("TRUE") ? 5 : appreciation.equals("UNCERTAIN") ? 4 : 14))
					.name(Message.PROCESS.get().replace("_Appreciation_", appreciationWord))
					.lore(Message.PROCESS_DETAILS.get().replace("_Appreciation_", appreciationWord).split(ConfigUtils.getLineBreakSymbol()))
					.create());
		}

		inv.setItem(18, MenuItem.CANCEL_PROCESS.get());

		return inv;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		switch (slot) {
			case 11:
				if (ReportUtils.punishmentsEnabled()) {
					u.openPunishmentMenu(1, r);
				} else {
					process("True");
				}
				break;
			case 13:
				process("Uncertain");
				break;
			case 15:
				process("False");
				break;
			case 18:
				u.openReportMenu(r);
				break;
			default:
				break;
		}
	}

	private void process(String appreciation) {
		r.process(p.getUniqueId().toString(), p.getName(), appreciation, false, Permission.STAFF_ARCHIVE_AUTO.isOwned(u));
		u.openDelayedlyReportsMenu();
	}

}
