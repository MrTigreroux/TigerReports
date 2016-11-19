package fr.mrtigreroux.tigerreports.listeners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import fr.mrtigreroux.tigerreports.data.UserData;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class SignListener implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onSignChange(SignChangeEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		if(!UserData.ReportCommenting.containsKey(uuid)) return;
		int reportNumber = UserData.ReportCommenting.get(uuid);
		
		String message = "";
		for(String line : e.getLines()) if(line != null && !line.equals("")) message = message.equals("") ? line : message+" "+line;
		Player p = e.getPlayer();
		User u = new User(p);
		if(!message.equals("")) {
			boolean commentModified = UserData.CommentModified.containsKey(uuid);
			String path = ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+(commentModified ? UserData.CommentModified.get(uuid) : ReportUtils.getNewCommentNumber(reportNumber));
			FilesManager.getReports.set(path+".Status", FilesManager.getReports.getString(path+".Status") != null ? FilesManager.getReports.getString(path+".Status") : "Private");
			FilesManager.getReports.set(path+".Author", p.getDisplayName());
			FilesManager.getReports.set(path+".Date", MessageUtils.getNowDate());
			FilesManager.getReports.set(path+".Message", commentModified ? FilesManager.getReports.getString(path+".Message")+" "+message : message);
			FilesManager.saveReports();
		}
		UserData.ReportCommenting.remove(uuid);
		UserData.CommentModified.remove(uuid);
		u.updateSignBlock(e.getBlock());
		u.openCommentsMenu(1, reportNumber);
		e.setCancelled(true);
	}
	
}
