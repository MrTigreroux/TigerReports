package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class CommentsMenu extends Menu {
	
	public CommentsMenu(User u, int page, int reportNumber) {
		super(u, 54, page, reportNumber);
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
					.lore(Message.COMMENT_DETAILS.get().replaceAll("_Author_", FilesManager.getReports.getString(path+".Author")).replaceAll("_Date_", FilesManager.getReports.getString(path+".Date").replaceAll("-", ":"))
							.replaceAll("_Message_", MessageUtils.getMenuSentence(FilesManager.getReports.getString(path+".Message"), Message.COMMENT_DETAILS, "_Message_", true))
									.replaceAll("_Action_", u.hasPermission(Permission.REMOVE) ? Message.COMMENT_REMOVE_ACTION.get() : "").split(ConfigUtils.getLineBreakSymbol())).create());
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
			if(click.equals(ClickType.DROP) && u.hasPermission(Permission.REMOVE)) {
				int commentNumber = slot-18+((page-1)*27)+1;
				FilesManager.getReports.set(ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+commentNumber, null);
				FilesManager.saveReports();
				open(true);
			}
		}
	}
	
}
