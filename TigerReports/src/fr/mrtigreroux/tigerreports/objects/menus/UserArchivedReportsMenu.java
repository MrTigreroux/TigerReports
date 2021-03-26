package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class UserArchivedReportsMenu extends UserManagerMenu implements UpdatedMenu {

	public UserArchivedReportsMenu(OnlineUser u, int page, User tu) {
		super("Menus.User-archived-reports", u, page, tu);
	}

	@Override
	public void onUpdate(Inventory inv) {
		ReportUtils.addReports(tu.getUniqueId(), null, true, inv, page, Message.REPORT_RESTORE_ACTION.get()
		        + (Permission.STAFF_DELETE.isOwned(u) ? Message.REPORT_DELETE_ACTION.get() : ""), false, "");
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		onClickArchivedReports(true, item, slot, click);
	}

}
