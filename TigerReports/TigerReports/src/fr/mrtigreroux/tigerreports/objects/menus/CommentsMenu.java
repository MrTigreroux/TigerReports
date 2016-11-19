package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.UserData;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class CommentsMenu extends Menu {
	
	public CommentsMenu(User u, int page, int reportNumber) {
		super(u, 54, page, reportNumber, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		Inventory inv = getInventory(Message.COMMENTS_TITLE.get().replaceAll("_Report_", ReportUtils.getName(reportNumber)), true);
		
		inv.setItem(0, ReportUtils.getItem(reportNumber, Message.REPORT_SHOW_ACTION.get()));
		inv.setItem(4, MenuItem.COMMENTS.get());
		inv.setItem(8, MenuItem.WRITE_COMMENT.get());
		
		int firstComment = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstComment = ((page-1)*27)+1;
		}
		int totalComments = ReportUtils.getTotalComments(reportNumber);
		for(int commentNumber = firstComment; commentNumber <= firstComment+26; commentNumber++) {
			String path = ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+commentNumber;
			if(commentNumber > totalComments || FilesManager.getReports.get(path) == null) break;
			inv.setItem(commentNumber-firstComment+18, new CustomItem().type(Material.PAPER).name(Message.COMMENT.get().replaceAll("_Number_", ""+commentNumber))
					.lore(Message.COMMENT_DETAILS.get().replaceAll("_Status_", getStatus(path)).replaceAll("_Author_", FilesManager.getReports.getString(path+".Author")).replaceAll("_Date_", FilesManager.getReports.getString(path+".Date").replaceAll("-", ":"))
							.replaceAll("_Message_", MessageUtils.getMenuSentence(FilesManager.getReports.getString(path+".Message"), Message.COMMENT_DETAILS, "_Message_", true))
									.replaceAll("_Actions_", Message.COMMENT_ADD_MESSAGE_ACTION.get()+(FilesManager.getReports.getString(path+".Status").equals("Private") ? Message.COMMENT_SEND_ACTION.get() : Message.COMMENT_CANCEL_SEND_ACTION.get())+(u.hasPermission(Permission.REMOVE) ? Message.COMMENT_REMOVE_ACTION.get() : "")).split(ConfigUtils.getLineBreakSymbol())).create());
		}
		
		if(firstComment+26 < totalComments) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 0) u.openReportMenu(reportNumber);
		else if(slot == 8) u.comment(reportNumber);
		else if(slot >= 18 && slot <= size-9) {
			int commentNumber = slot-18+((page-1)*27)+1;
			if(click.toString().contains("LEFT")) {
				UserData.CommentModified.put(p.getUniqueId(), commentNumber);
				u.comment(reportNumber);
			} else if(click.toString().contains("RIGHT")) {
				String commentPath = ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+commentNumber;
					String comment = "Report#"+reportNumber+":Comment#"+commentNumber;
					String signalman = ReportUtils.getPlayerName("Signalman", reportNumber, false);
					String uuid = UserUtils.getUniqueId(signalman);
				if(FilesManager.getReports.getString(commentPath+".Status").equals("Private")) {
					Player s = UserUtils.getPlayer(signalman);
					FilesManager.getReports.set(commentPath+".Status", "Sent");
					FilesManager.saveReports();
					if(s != null) new User(s).sendNotification(comment);
					else {
						List<String> notifications = UserUtils.getNotifications(uuid);
						notifications.add(comment);
						UserUtils.setNotifications(uuid, notifications);
					}
				} else {
					FilesManager.getReports.set(commentPath+".Status", "Private");
					FilesManager.saveReports();
					List<String> notifications = UserUtils.getNotifications(uuid);
					notifications.remove(comment);
					UserUtils.setNotifications(uuid, notifications);
				}
				open(true);
			} else if(click.equals(ClickType.DROP) && u.hasPermission(Permission.REMOVE)) {
				FilesManager.getReports.set(ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+commentNumber, null);
				FilesManager.saveReports();
				open(true);
			}
		}
	}
	
	private String getStatus(String path) {
		String status = FilesManager.getReports.getString(path+".Status");
		try {
			return status != null ? Message.valueOf(status.toUpperCase()).get() : Message.PRIVATE.get();
		} catch (Exception invalidStatus) {
			return Message.PRIVATE.get();
		}
	}
	
}
