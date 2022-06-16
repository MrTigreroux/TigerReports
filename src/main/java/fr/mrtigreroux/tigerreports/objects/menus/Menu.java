package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.VersionUtils;

/**
 * @author MrTigreroux
 */

public abstract class Menu {

	private static final Logger LOGGER = Logger.fromClass(Menu.class);

	final User u;
	final Player p;
	final int size;
	int page;
	private final Permission permission;
	private boolean isInvCurrentlyModified = false;
	private boolean updateRequested = false;

	public Menu(User u, int size, int page, Permission permission) {
		this.u = u;
		this.p = u.getPlayer();
		this.size = size;
		this.page = page;
		this.permission = permission;
	}

	boolean check() {
		if (!u.isOnline()) {
			LOGGER.info(() -> this + ": check(): " + u.getName() + " is not online, return false");
			return false;
		}

		String error = permission != null && !u.hasPermission(permission) ? Message.PERMISSION_COMMAND.get()
		        : this instanceof ReportManagerMenu ? ((ReportManagerMenu) this).checkReport() : null;
		if (error != null) {
			MessageUtils.sendErrorMessage(p, error);
			p.closeInventory();
			return false;
		} else {
			return true;
		}
	}

	Inventory getInventory(String title, boolean borders) {
		if (title.length() > 32 && VersionUtils.isVersionLower1_9())
			title = title.substring(0, 29) + "..";
		Inventory inv = Bukkit.createInventory(null, size, title);
		if (borders) {
			ItemStack gui = MenuRawItem.GUI.create();
			int size = inv.getSize();
			for (int position = 9; position < 18; position++)
				inv.setItem(position, gui);
			for (int position = size - 9; position < size; position++)
				inv.setItem(position, gui);
			inv.setItem(size - 5, MenuItem.CLOSE.get());
		}
		return inv;
	}

	public void open(boolean sound) {
		if (!check())
			return;

		u.setOpenedMenu(null); // Close previous menu if any

		LOGGER.info(() -> this + ": open()");
		isInvCurrentlyModified = true;
		Inventory inv = onOpen();
		if (inv == null) {
			p.closeInventory();
			isInvCurrentlyModified = false;
			return;
		}

		if (this instanceof UpdatedMenu) {
			((UpdatedMenu) this).onUpdate(inv);
		}

		p.openInventory(inv);
		u.setOpenedMenu(this);
		isInvCurrentlyModified = false;

		if (sound) {
			ConfigSound.MENU.play(p);
		}

		if (updateRequested) {
			LOGGER.info(() -> this + ": open(): update requested, calls update()");
			updateRequested = false;
			update(false);
		}
	}

	abstract Inventory onOpen();

	public void update(boolean sound) {
		if (!check())
			return;

		if (this != u.getOpenedMenu()) {
			LOGGER.info(() -> this + ": update(): " + u.getName() + "'s opened menu is not this menu, cancel update");
			return;
		}

		LOGGER.info(() -> this + ": update(): isInvCurrentlyModified = " + isInvCurrentlyModified);
		if (isInvCurrentlyModified) {
			updateRequested = true;
			return;
		}

		Inventory inv = getOpenInventory();

		if (inv != null) {
			isInvCurrentlyModified = true;
			if (this instanceof UpdatedMenu) {
				((UpdatedMenu) this).onUpdate(inv);
			} else {
				inv.setContents(this.onOpen().getContents());
			}
			isInvCurrentlyModified = false;

			if (sound) {
				ConfigSound.MENU.play(p);
			}
			if (updateRequested) {
				LOGGER.info(() -> this + ": update(): update requested, calls update()");
				updateRequested = false;
				update(false);
			}
		} else {
			open(sound);
		}
	}

	protected Inventory getOpenInventory() {
		InventoryView invView = p.getOpenInventory();
		if (invView != null) {
			Inventory inv = invView.getTopInventory();
			if (inv != null && inv.getSize() == size) {
				return inv;
			}
		}
		return null;
	}

	public void click(ItemStack item, int slot, ClickType click) {
		if (slot == -1 || item == null || item.getType() == null || item.getType() == Material.AIR
		        || (item.getType().toString().toUpperCase().contains("STAINED_GLASS_PANE")
		                && ((slot >= size - 9 && slot < size) || (slot >= 9 && slot <= 17)))
		        || !check())
			return;

		if (slot == size - 5) {
			TigerReports.getInstance().runTaskDelayedly(10, new Runnable() {

				@Override
				public void run() {
					p.closeInventory();
					ConfigSound.MENU.play(p);
				}

			});
			return;
		}

		if (page != 0) {
			int newPage = page - (slot == size - 7 ? 1 : slot == size - 3 ? -1 : page);
			if (newPage != 0) {
				onPageChange(page, newPage);
				page = newPage;
				update(true);
				return;
			}
		}

		onClick(item, slot, click);
	}

	abstract void onClick(ItemStack item, int slot, ClickType click);

	int getItemGlobalIndex(int slot) {
		return slot - 17 + ((page - 1) * 27);
	}

	public int getPage() {
		return page;
	}

	public void onPageChange(int oldPage, int newPage) {}

	public void onClose() {}

}
