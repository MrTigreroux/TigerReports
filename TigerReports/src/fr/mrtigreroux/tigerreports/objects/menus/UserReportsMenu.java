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

public class UserReportsMenu extends UserManagerMenu implements UpdatedMenu {

	public UserReportsMenu(OnlineUser u, int page, User tu) {
		super("Menus.User-reports", u, page, tu);
	}

	@Override
	public void onUpdate(Inventory inv) {
		ReportUtils.addReports(tu.getUniqueId(), null, false, inv, page, Message.REPORT_SHOW_ACTION.get(), Permission.STAFF_ARCHIVE.isOwned(u),
				Permission.STAFF_DELETE.isOwned(u) ? Message.REPORT_DELETE_ACTION.get() : "");
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		onClickCurrentReports(true, item, slot, click);
	}

}
