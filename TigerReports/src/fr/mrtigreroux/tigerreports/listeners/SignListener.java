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
		
		String message = "";
		for(String line : e.getLines()) if(line != null && !line.equals("")) message += message.equals("") ? line : " "+line;
		if(!message.equals("")) {
			Comment c = u.getModifiedComment();
			if(c == null) {
				String date = MessageUtils.getNowDate();
				int commentId = TigerReports.getDb().insert("INSERT INTO comments (report_id,status,date,author,message) VALUES (?,?,?,?,?);", Arrays.asList(r.getId(), "Private", date, p.getDisplayName(), message));
				new Comment(r, commentId, "Private", date, p.getDisplayName(), message).save();
			} else c.addMessage(message);
		}
		
		u.setCommentingReport(null);
		u.setModifiedComment(null);
		u.updateSignBlock(e.getBlock());
		u.openCommentsMenu(1, r);
		e.setCancelled(true);
	}
	
}
