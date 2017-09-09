package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class CommentsMenu extends ReportManagerMenu implements UpdatedMenu {
	
	public CommentsMenu(OnlineUser u, int page, int reportId) {
		super(u, 54, page, Permission.STAFF, reportId);
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
		int index = 0;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			index += (page-1)*27;
		}
		
		List<Comment> comments = new ArrayList<>(r.getComments().values());
		boolean remove = Permission.REMOVE.isOwned(u);
		ItemStack empty = new ItemStack(Material.AIR);
		for(int position = 18; position < 45; position++) {
			if(index == -1) inv.setItem(position, empty);
			else {
				Comment c = index < comments.size() ? comments.get(index) : null;
				if(c == null) {
					inv.setItem(position, empty);
					index = -1;
				} else {
					inv.setItem(position, c.getItem(remove));
					index++;
				}
			}
		}

		if(comments.size() == 28) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 0) u.openReportMenu(r);
		else if(slot == 8) u.comment(r);
		else if(slot >= 18 && slot <= size-9) {
			Comment c = new ArrayList<>(r.getComments().values()).get(getIndex(slot)-1);
			if(c != null) {
				if(click.toString().contains("LEFT")) {
					if(!c.getAuthor().equalsIgnoreCase(p.getDisplayName())) {
						ConfigSound.ERROR.play(p);
						return;
					}
					u.setModifiedComment(c);
					u.comment(r);
					return;
				} else if(click.toString().contains("RIGHT")) {
					String comment = "Report"+r.getId()+":Comment"+c.getId();
					User su = UserUtils.getUser(r.getSignalmanUniqueId());
					boolean isPrivate = c.getStatus(true).equals("Private");
					if(isPrivate && su instanceof OnlineUser) {
						((OnlineUser) su).sendNotification(comment, true);
						update(true);
						return;
					}
					
					List<String> notifications = su.getNotifications();
					if(isPrivate) {
						c.setStatus("Sent");
						notifications.add(comment);
					} else {
						c.setStatus("Private");
						notifications.remove(comment);
					}
					su.setNotifications(notifications);
				} else if(click.equals(ClickType.DROP)) {
					if(Permission.REMOVE.isOwned(u)) c.remove();
					else return;
				}
			}
			update(true);
		}
	}
	
}
