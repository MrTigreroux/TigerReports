package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public abstract class Menu {

	final OnlineUser u;
	final Player p;
	final int size;
	int page;
	private final Permission permission;

	public Menu(OnlineUser u, int size, int page, Permission permission) {
		this.u = u;
		this.p = u.getPlayer();
		this.size = size;
		this.page = page;
		this.permission = permission;
	}

	boolean check() {
		String error = permission != null && !permission.isOwned(u)	? Message.PERMISSION_COMMAND.get()
																	: this instanceof ReportManagerMenu	? ((ReportManagerMenu) this).checkReport()
																										: null;
		if (error != null) {
			MessageUtils.sendErrorMessage(p, error);
			p.closeInventory();
			return false;
		} else {
			return true;
		}
	}

	Inventory getInventory(String title, boolean borders) {
		if (title.length() > 32)
			title = title.substring(0, 29)+"..";
		Inventory inv = Bukkit.createInventory(null, size, title);
		if (borders) {
			ItemStack gui = new CustomItem().type(Material.STAINED_GLASS_PANE).damage((byte) 7).name(" ").create();
			int size = inv.getSize();
			for (int position = 9; position < 18; position++)
				inv.setItem(position, gui);
			for (int position = size-9; position < size; position++)
				inv.setItem(position, gui);
			inv.setItem(size-5, MenuItem.CLOSE.get());
		}
		return inv;
	}

	public void open(boolean sound) {
		if (this instanceof ReportManagerMenu && !((ReportManagerMenu) this).collectReport())
			return;

		if (!check())
			return;

		Inventory inv = onOpen();
		if (inv == null) {
			p.closeInventory();
			return;
		}

		boolean updated = this instanceof UpdatedMenu;
		if (updated)
			((UpdatedMenu) this).onUpdate(inv);

		p.openInventory(inv);
		u.setOpenedMenu(this);
		if (updated)
			MenuUpdater.addUser(u);
		if (sound)
			ConfigSound.MENU.play(p);
	}

	abstract Inventory onOpen();

	public void update(boolean sound) {
		if (this instanceof UpdatedMenu) {
			if (!check())
				return;
			InventoryView invView = p.getOpenInventory();
			if (invView != null) {
				Inventory inv = invView.getTopInventory();
				if (inv != null && inv.getSize() == size) {
					((UpdatedMenu) this).onUpdate(inv);
					if (sound)
						ConfigSound.MENU.play(p);
					return;
				}
			}
		}
		open(sound);
	}

	public void click(ItemStack item, int slot, ClickType click) {
		if (slot == -1 || item == null || item.getType() == Material.AIR || (item.getType() == Material.STAINED_GLASS_PANE && ((slot >= size-9
				&& slot < size) || (slot >= 9 && slot <= 17))) || !check())
			return;

		if (slot == size-5) {
			p.closeInventory();
			ConfigSound.MENU.play(p);
			return;
		}

		if (page != 0) {
			int newPage = page-(slot == size-7 ? 1 : slot == size-3 ? -1 : page);
			if (newPage != 0) {
				page = newPage;
				open(true);
				return;
			}
		}

		onClick(item, slot, click);
	}

	abstract void onClick(ItemStack item, int slot, ClickType click);

	int getConfigIndex(int slot) {
		return slot-17+((page-1)*27);
	}

}
