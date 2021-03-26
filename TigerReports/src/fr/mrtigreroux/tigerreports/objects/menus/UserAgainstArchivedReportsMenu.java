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

public class UserAgainstArchivedReportsMenu extends UserManagerMenu implements UpdatedMenu {

	public UserAgainstArchivedReportsMenu(OnlineUser u, int page, User tu) {
		super("Menus.User-against-archived-reports", u, page, tu);
	}

	@Override
	public void onUpdate(Inventory inv) {
		ReportUtils.addReports(null, tu.getUniqueId(), true, inv, page, Message.REPORT_RESTORE_ACTION.get()
		        + (Permission.STAFF_DELETE.isOwned(u) ? Message.REPORT_DELETE_ACTION.get() : ""), false, "");
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		onClickArchivedReports(false, item, slot, click);
	}

}
