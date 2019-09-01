package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public abstract class UserManagerMenu extends Menu {

	final User tu;
	private String tag;

	public UserManagerMenu(String tag, OnlineUser u, int page, User tu) {
		super(u, 54, page, Permission.STAFF);
		this.tu = tu;
		this.tag = tag;
	}

	@Override
	public Inventory onOpen() {
		String name = tu.getName();
		Inventory inv = getInventory(Message.get(tag+"-title").replace("_Target_", name).replace("_Page_", Integer.toString(page)), true);

		inv.setItem(0, new CustomItem().skullOwner(name)
				.name(Message.USER.get().replace("_Target_", name))
				.lore(Message.get("Menus.User-details").replace("_Target_", name).split(ConfigUtils.getLineBreakSymbol()))
				.create());
		inv.setItem(4, new CustomItem().type(Material.BOOKSHELF).name(Message.get(tag).replace("_Target_", name)).create());

		return inv;
	}
	
	protected Report getReport(boolean archived, int reportIndex) {
		return TigerReports.getInstance().getReportsManager().formatFullReport(TigerReports.getInstance()
				.getDb()
				.query("SELECT * FROM tigerreports_reports WHERE archived = ? AND reporter_uuid LIKE '%"+tu.getUniqueId()+"%' LIMIT 1 OFFSET ?", Arrays.asList(archived ? 1 : 0, reportIndex-1))
				.getResult(0));
	}

}
