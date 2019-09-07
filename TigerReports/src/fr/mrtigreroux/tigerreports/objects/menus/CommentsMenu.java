package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class CommentsMenu extends ReportManagerMenu implements UpdatedMenu {

	public CommentsMenu(OnlineUser u, int page, Report report) {
		super(u, 54, page, Permission.STAFF, report);
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.COMMENTS_TITLE.get().replace("_Report_", r.getName()), true);

		inv.setItem(0, r.getItem(Message.REPORT_SHOW_ACTION.get()));
		inv.setItem(4, MenuItem.COMMENTS.get());
		inv.setItem(8, MenuItem.WRITE_COMMENT.get());

		return inv;
	}

	@Override
	public void onUpdate(Inventory inv) {
		int firstComment = 1;
		if (page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstComment += (page-1)*27;
		}

		List<Map<String, Object>> results = TigerReports.getInstance()
				.getDb()
				.query("SELECT * FROM tigerreports_comments WHERE report_id = ? LIMIT 28 OFFSET ?", Arrays.asList(r.getId(), firstComment-1))
				.getResultList();

		boolean delete = Permission.STAFF_DELETE.isOwned(u);
		int index = 0;
		for (int slot = 18; slot < 45; slot++) {
			if (index == -1) {
				inv.setItem(slot, null);
			} else {
				Comment c = index < results.size() ? r.formatComment(results.get(index)) : null;
				if (c == null) {
					inv.setItem(slot, null);
					index = -1;
				} else {
					inv.setItem(slot, c.getItem(delete));
					index++;
				}
			}
		}

		if (results.size() == 28)
			inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if (slot == 0) {
			u.openReportMenu(r);
		} else if (slot == 8) {
			u.createComment(r);
		} else if (slot >= 18 && slot <= size-9) {
			Comment c = r.getComment(getConfigIndex(slot));
			if (c != null) {
				switch (click) {
					case LEFT:
					case SHIFT_LEFT:
						u.editComment(c);
						return;
					case RIGHT:
					case SHIFT_RIGHT:
						int reportId = r.getId();
						int commentId = c.getId();
						String notification = reportId+":"+commentId;
						User ru = TigerReports.getInstance().getUsersManager().getUser(r.getReporterUniqueId());
						boolean isPrivate = c.getStatus(true).equals("Private");
						if (isPrivate && ru instanceof OnlineUser) {
							((OnlineUser) ru).sendCommentNotification(r, c, true);
						} else if (isPrivate && UserUtils.isOnline(ru.getName())) {
							TigerReports.getInstance().getBungeeManager().sendPluginNotification(reportId+" comment "+commentId+" "+ru.getName());
						} else {
							List<String> notifications = ru.getNotifications();
							if (isPrivate) {
								c.setStatus("Sent");
								notifications.add(notification);
							} else {
								c.setStatus("Private");
								notifications.remove(notification);
							}
							ru.setNotifications(notifications);
						}
						break;
					case DROP:
						if (Permission.STAFF_DELETE.isOwned(u)) {
							c.delete();
						} else {
							return;
						}
						break;
					default:
						break;
				}
			}
			Bukkit.getScheduler().runTaskLater(TigerReports.getInstance(), new Runnable() {

				@Override
				public void run() {
					update(true);
				}

			}, 10);
		}
	}

}
