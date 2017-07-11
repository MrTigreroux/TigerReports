package fr.mrtigreroux.tigerreports.objects;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class Comment {

	final Report r;
	final int commentId;
	final String date, author;
	String status, message;
	
	public Comment(Report r, int commentId, String status, String date, String author, String message) {
		this.r = r;
		this.commentId = commentId;
		this.status = status;
		this.date = date;
		this.author = author;
		this.message = message;
	}
	
	public int getId() {
		return commentId;
	}
	
	public String getStatus(boolean config) {
		if(config) return status;
		try {
			return status != null ? Message.valueOf(status.toUpperCase()).get() : Message.PRIVATE.get();
		} catch (Exception invalidStatus) {
			return Message.PRIVATE.get();
		}
	}
	
	public void setStatus(String status) {
		this.status = status;
		save();
		TigerReports.getDb().updateAsynchronously("UPDATE report"+r.getId()+"_comments SET status = ? WHERE comment_id = ?", Arrays.asList(this.status, commentId));
	}
	
	public String getAuthor() {
		return author;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void addMessage(String message) {
		this.message += " "+message;
		save();
		TigerReports.getDb().updateAsynchronously("UPDATE report"+r.getId()+"_comments SET message = ? WHERE comment_id = ?", Arrays.asList(this.message, commentId));
	}
	
	public ItemStack getItem(boolean removePermission) {
		return new CustomItem().type(Material.PAPER).name(Message.COMMENT.get().replace("_Id_", ""+commentId)).lore(Message.COMMENT_DETAILS.get().replace("_Status_", getStatus(false))
				.replace("_Author_", author).replace("_Date_", date).replace("_Message_", MessageUtils.getMenuSentence(message, Message.COMMENT_DETAILS, "_Message_", true))
				.replace("_Actions_", Message.COMMENT_ADD_MESSAGE_ACTION.get()+(status.equals("Private") ? Message.COMMENT_SEND_ACTION.get() : Message.COMMENT_CANCEL_SEND_ACTION.get())+(removePermission ? Message.COMMENT_REMOVE_ACTION.get() : "")).split(ConfigUtils.getLineBreakSymbol())).create();
	}
	
	public void remove() {
		r.comments.remove(commentId);
		TigerReports.getDb().updateAsynchronously("DELETE FROM report"+r.getId()+"_comments WHERE comment_id = ?", Arrays.asList(commentId));
	}
	
	public void save() {
		r.comments.put(commentId, this);
	}
	
}
