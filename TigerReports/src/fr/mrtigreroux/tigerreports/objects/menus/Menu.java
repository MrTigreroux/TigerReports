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

	protected OnlineUser u;
	protected Player p;
	protected int size, page;
	protected Report r;
	protected String action;
	protected User tu;
	
	public Menu(OnlineUser u, int size, int page, int reportId, String action, User tu) {
		this.u = u;
		this.p = u.getPlayer();
		this.size = size;
		this.page = page;
		this.r = reportId != -1 ? ReportUtils.getReportById(reportId) : null;
		this.action = action;
		this.tu = tu;
	}
	
	protected Inventory getInventory(String title, boolean borders) {
		if(title.length() > 32) title = title.substring(0, 29)+"..";
		Inventory inv = Bukkit.createInventory(null, size, title);
		if(borders) {
			ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name("").create();
			int size = inv.getSize();
			for(int position = 9; position <= 17; position++) inv.setItem(position, gui);
			for(int position = size-9; position <= size-1; position++) inv.setItem(position, gui);
			inv.setItem(size-5, MenuItem.CLOSE.get());
		}
		return inv;
	}
	
	public void click(ItemStack item, int slot, ClickType click) {
		if(slot == -1 || item == null || item.getType() == Material.AIR || (item.getType() == Material.STAINED_GLASS_PANE && ((slot >= size-9 && slot <= size-1) || (slot >= 9 && slot <= 17)))) return;
		if(slot == size-5 && item.isSimilar(MenuItem.CLOSE.get())) {
			p.closeInventory();
			u.playSound(ConfigSound.MENU.get());
			return;
		}
		if(page != 0) {
			int newPage = slot == size-7 && item.isSimilar(MenuItem.PAGE_SWITCH_PREVIOUS.get()) ? page-1 : slot == size-3 && item.isSimilar(MenuItem.PAGE_SWITCH_NEXT.get()) ? page+1 : 0;
			if(newPage != 0) {
				page = newPage;
				open(true);
				return;
			}
		}
		onClick(item, slot, click);
	}
	
	protected int getIndex(int slot) {
		return slot-17+((page-1)*27);
	}
	
	protected boolean checkReport() {
		if(r == null || !TigerReports.Reports.containsKey(r.getId())) {
			p.closeInventory();
			MessageUtils.sendErrorMessage(p, Message.INVALID_REPORT.get());
			return false;
		}
		return true;
	}
	
	public abstract void open(boolean sound);
	public abstract void onClick(ItemStack item, int slot, ClickType click);
	
}
