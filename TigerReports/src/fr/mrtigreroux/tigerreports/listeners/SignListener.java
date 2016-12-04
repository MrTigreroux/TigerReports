package fr.mrtigreroux.tigerreports.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class SignListener implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onSignChange(SignChangeEvent e) {
		User u = UserUtils.getUser(e.getPlayer());
		int reportNumber = u.getCommentingReport();
		if(reportNumber == -1) return;
		
		String message = "";
		for(String line : e.getLines()) if(line != null && !line.equals("")) message = message.equals("") ? line : message+" "+line;
		Player p = e.getPlayer();
		if(!message.equals("")) {
			int modifiedComment = u.getModifiedComment();
			boolean commentModified = modifiedComment != -1;
			String path = ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+(commentModified ? modifiedComment : ReportUtils.getNewCommentNumber(reportNumber));
			ConfigFile.REPORTS.get().set(path+".Status", ConfigFile.REPORTS.get().getString(path+".Status") != null ? ConfigFile.REPORTS.get().getString(path+".Status") : "Private");
			ConfigFile.REPORTS.get().set(path+".Author", p.getDisplayName());
			ConfigFile.REPORTS.get().set(path+".Date", MessageUtils.getNowDate());
			ConfigFile.REPORTS.get().set(path+".Message", commentModified ? ConfigFile.REPORTS.get().getString(path+".Message")+" "+message : message);
			ConfigFile.REPORTS.save();
		}
		u.setCommentingReport(-1);
		u.setModifiedComment(-1);
		u.updateSignBlock(e.getBlock());
		u.openCommentsMenu(1, new Report(reportNumber));
		e.setCancelled(true);
	}
	
}
