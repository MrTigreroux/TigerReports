package fr.mrtigreroux.tigerreports.listeners;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class SignListener implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onSignChange(SignChangeEvent e) {
		Player p = e.getPlayer();
		OnlineUser u = UserUtils.getOnlineUser(p);
		Report r = u.getCommentingReport();
		if(r == null) return;
		
		StringBuilder message = new StringBuilder();
		for(String line : e.getLines()) if(line != null && !line.equals("")) message.append(message.toString().equals("") ? line : " " + line);
		if(!message.toString().equals("")) {
			Comment c = u.getModifiedComment();
			r.checkComments();
			if(c == null) {
				String date = MessageUtils.getNowDate();
				int commentId = TigerReports.getDb().insert("INSERT INTO report"+r.getId()+"_comments (status,date,author,message) VALUES (?,?,?,?);", Arrays.asList("Private", date, p.getDisplayName(), message.toString()));
				new Comment(r, commentId, "Private", date, p.getDisplayName(), message.toString()).save();
			} else c.addMessage(message.toString());
		}
		
		u.setCommentingReport(null);
		u.setModifiedComment(null);
		u.updateSignBlock(e.getBlock());
		u.openCommentsMenu(1, r);
		e.setCancelled(true);
	}
	
}
