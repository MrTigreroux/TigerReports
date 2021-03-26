package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
		String displayName = tu.getDisplayName(false);
		Inventory inv = getInventory(
		        Message.get(tag + "-title").replace("_Target_", name).replace("_Page_", Integer.toString(page)), true);

		inv.setItem(0,
		        new CustomItem().skullOwner(name).name(Message.USER.get().replace("_Target_", displayName))
		                .lore(Message.get("Menus.User-details").replace("_Target_", displayName)
		                        .split(ConfigUtils.getLineBreakSymbol()))
		                .create());
		inv.setItem(4, new CustomItem().type(Material.BOOKSHELF).name(Message.get(tag).replace("_Target_", displayName))
		        .create());

		return inv;
	}

	protected Report getReportWhereReporter(boolean archived, int reportIndex) {
		TigerReports tr = TigerReports.getInstance();
		return tr.getReportsManager().formatFullReport(tr.getDb()
		        .query("SELECT * FROM tigerreports_reports WHERE archived = ? AND reporter_uuid LIKE '%"
		                + tu.getUniqueId() + "%'" + (archived ? " ORDER BY report_id DESC" : "") + " LIMIT 1 OFFSET ?",
		                Arrays.asList(archived ? 1 : 0, reportIndex - 1))
		        .getResult(0));
	}

	private Report getReportWhereReported(boolean archived, int reportIndex) {
		TigerReports tr = TigerReports.getInstance();
		return tr.getReportsManager()
		        .formatFullReport(tr.getDb()
		                .query("SELECT * FROM tigerreports_reports WHERE archived = ? AND reported_uuid = ?"
		                        + (archived ? " ORDER BY report_id DESC" : "") + " LIMIT 1 OFFSET ?",
		                        Arrays.asList(archived ? 1 : 0, tu.getUniqueId(), reportIndex - 1))
		                .getResult(0));
	}

	protected void onClickCurrentReports(boolean whereReporter, ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openUserMenu(tu);
		} else if (slot >= 18 && slot <= size - 9) {
			TigerReports tr = TigerReports.getInstance();
			Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

				@Override
				public void run() {
					Report r = whereReporter ? getReportWhereReporter(false, getConfigIndex(slot))
					        : getReportWhereReported(false, getConfigIndex(slot));
					Bukkit.getScheduler().runTask(tr, new Runnable() {

						@Override
						public void run() {
							if (r == null) {
								update(false);
							} else {
								if (click.equals(ClickType.MIDDLE) && u.canArchive(r)) {
									u.openConfirmationMenu(r, "ARCHIVE");
								} else if (click.equals(ClickType.DROP) && Permission.STAFF_DELETE.isOwned(u)) {
									u.openConfirmationMenu(r, "DELETE");
								} else {
									u.openReportMenu(r);
								}
							}
						}

					});
				}

			});
		}
	}

	protected void onClickArchivedReports(boolean whereReporter, ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openUserMenu(tu);
		} else if (slot >= 18 && slot <= size - 9) {
			TigerReports tr = TigerReports.getInstance();
			Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

				@Override
				public void run() {
					Report r = whereReporter ? getReportWhereReporter(true, getConfigIndex(slot))
					        : getReportWhereReported(true, getConfigIndex(slot));
					Bukkit.getScheduler().runTask(tr, new Runnable() {

						@Override
						public void run() {
							if (r == null) {
								update(true);
							} else if (click.equals(ClickType.DROP) && Permission.STAFF_DELETE.isOwned(u)) {
								u.openConfirmationMenu(r, "DELETE_ARCHIVE");
							} else {
								r.unarchive(p.getUniqueId().toString(), false);
								u.openDelayedlyReportsMenu();
							}
						}

					});
				}

			});
		}
	}

}
