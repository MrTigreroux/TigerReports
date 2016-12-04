package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class CommentsMenu extends Menu {
	
	public CommentsMenu(User u, int page, Report r) {
		super(u, 54, page, r, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		Inventory inv = getInventory(Message.COMMENTS_TITLE.get().replace("_Report_", r.getName()), true);
		
		inv.setItem(0, r.getItem(Message.REPORT_SHOW_ACTION.get()));
		inv.setItem(4, MenuItem.COMMENTS.get());
		inv.setItem(8, MenuItem.WRITE_COMMENT.get());
		
		int firstComment = 1;
		if(page >= 2) {
			inv.setItem(size-7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
			firstComment = ((page-1)*27)+1;
		}
		int totalComments = r.getTotalComments();
		for(int commentNumber = firstComment; commentNumber <= firstComment+26; commentNumber++) {
			String path = r.getConfigPath()+".Comments.Comment"+commentNumber;
			if(commentNumber > totalComments || ConfigFile.REPORTS.get().get(path) == null) break;
			inv.setItem(commentNumber-firstComment+18, new CustomItem().type(Material.PAPER).name(Message.COMMENT.get().replace("_Number_", ""+commentNumber))
					.lore(Message.COMMENT_DETAILS.get().replace("_Status_", getStatus(path)).replace("_Author_", ConfigFile.REPORTS.get().getString(path+".Author")).replace("_Date_", ConfigFile.REPORTS.get().getString(path+".Date").replace("-", ":"))
							.replace("_Message_", MessageUtils.getMenuSentence(ConfigFile.REPORTS.get().getString(path+".Message"), Message.COMMENT_DETAILS, "_Message_", true))
									.replace("_Actions_", Message.COMMENT_ADD_MESSAGE_ACTION.get()+(ConfigFile.REPORTS.get().getString(path+".Status").equals("Private") ? Message.COMMENT_SEND_ACTION.get() : Message.COMMENT_CANCEL_SEND_ACTION.get())+(u.hasPermission(Permission.REMOVE) ? Message.COMMENT_REMOVE_ACTION.get() : "")).split(ConfigUtils.getLineBreakSymbol())).create());
		}
		
		if(firstComment+26 < totalComments) inv.setItem(size-3, MenuItem.PAGE_SWITCH_NEXT.get());
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(slot == 0) u.openReportMenu(r);
		else if(slot == 8) u.comment(r.getNumber());
		else if(slot >= 18 && slot <= size-9) {
			int commentNumber = slot-18+((page-1)*27)+1;
			if(click.toString().contains("LEFT")) {
				u.setModifiedComment(commentNumber);
				u.comment(r.getNumber());
			} else if(click.toString().contains("RIGHT")) {
				String commentPath = r.getConfigPath()+".Comments.Comment"+commentNumber;
					String comment = "Report#"+r.getNumber()+":Comment#"+commentNumber;
					String signalman = r.getPlayerName("Signalman", false);
					String uuid = UserUtils.getUniqueId(signalman);
				if(ConfigFile.REPORTS.get().getString(commentPath+".Status").equals("Private")) {
					Player s = UserUtils.getPlayer(signalman);
					ConfigFile.REPORTS.get().set(commentPath+".Status", "Sent");
					ConfigFile.REPORTS.save();
					if(s != null) new User(s).sendNotification(comment);
					else {
						List<String> notifications = UserUtils.getNotifications(uuid);
						notifications.add(comment);
						UserUtils.setNotifications(uuid, notifications);
					}
				} else {
					ConfigFile.REPORTS.get().set(commentPath+".Status", "Private");
					ConfigFile.REPORTS.save();
					List<String> notifications = UserUtils.getNotifications(uuid);
					notifications.remove(comment);
					UserUtils.setNotifications(uuid, notifications);
				}
				open(true);
			} else if(click.equals(ClickType.DROP) && u.hasPermission(Permission.REMOVE)) {
				ConfigFile.REPORTS.get().set(r.getConfigPath()+".Comments.Comment"+commentNumber, null);
				ConfigFile.REPORTS.save();
				open(true);
			}
		}
	}
	
	private String getStatus(String path) {
		String status = ConfigFile.REPORTS.get().getString(path+".Status");
		try {
			return status != null ? Message.valueOf(status.toUpperCase()).get() : Message.PRIVATE.get();
		} catch (Exception invalidStatus) {
			return Message.PRIVATE.get();
		}
	}
	
}
