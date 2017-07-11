package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public abstract class Menu {

	protected final OnlineUser u;
	protected final Player p;
	int size, page;
	private final Permission permission;
	protected Report r;
	String action;
	User tu;
	
	public Menu(OnlineUser u, int size, int page, Permission permission, int reportId, String action, User tu) {
		this.u = u;
		this.p = u.getPlayer();
		this.size = size;
		this.page = page;
		this.permission = permission;
		this.r = ReportUtils.getReportById(reportId);
		this.action = action;
		this.tu = tu;
	}
	
	private boolean checkPermission() {
		if(permission != null && !permission.check(u)) {
			p.closeInventory();
			MessageUtils.sendErrorMessage(p, Message.PERMISSION_COMMAND.get());
			return false;
		}
		return true;
	}
	
	protected Inventory getInventory(String title, boolean borders) {
		if(title.length() > 32) title = title.substring(0, 29)+"..";
		Inventory inv = Bukkit.createInventory(null, size, title);
		if(borders) {
			ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name("").create();
			int size = inv.getSize();
			for(int position = 9; position < 18; position++) inv.setItem(position, gui);
			for(int position = size-9; position < size; position++) inv.setItem(position, gui);
			inv.setItem(size-5, MenuItem.CLOSE.get());
		}
		return inv;
	}
	
	private boolean checkReport() {
		if(this instanceof ReportManagement && (r == null || !TigerReports.Reports.containsKey(r.getId()))) {
			p.closeInventory();
			MessageUtils.sendErrorMessage(p, Message.INVALID_REPORT.get());
			return false;
		}
		return true;
	}
	
	public void open(boolean sound) {
		checkPermission();
		checkReport();
		onOpen();
		if(sound) u.playSound(ConfigSound.MENU);
		u.setOpenedMenu(this);
	}
	
	public void click(ItemStack item, int slot, ClickType click) {
		checkPermission();
		checkReport();
		if(slot == -1 || item == null || item.getType() == Material.AIR || (item.getType() == Material.STAINED_GLASS_PANE && ((slot >= size-9 && slot < size) || (slot >= 9 && slot <= 17)))) return;
		if(slot == size-5) {
			p.closeInventory();
			u.playSound(ConfigSound.MENU);
			return;
		}
		if(page != 0) {
			int newPage = page-(slot == size-7 ? 1 : slot == size-3 ? -1 : page);
			if(newPage != 0) {
				page = newPage;
				open(true);
				return;
			}
		}
		onClick(item, slot, click);
	}
	
	int getIndex(int slot) {
		return slot-17+((page-1)*27);
	}
	
	public abstract void onOpen();
	public abstract void onClick(ItemStack item, int slot, ClickType click);
	
}
