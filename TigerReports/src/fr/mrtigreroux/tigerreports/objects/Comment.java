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

	private final Report r;
	private final Integer commentId;
	private final String date, author;
	private String status, message;

	public Comment(Report r, Integer commentId, String status, String date, String author, String message) {
		this.r = r;
		this.commentId = commentId;
		this.status = status;
		this.date = date;
		this.author = author;
		this.message = message;
	}

	public Report getReport() {
		return r;
	}

	public Integer getId() {
		return commentId;
	}

	public String getStatus(boolean config) {
		if (config)
			return status;

		if (status == null) {
			return Message.PRIVATE.get();
		} else if (status.startsWith("Read")) {
			return Message.READ.get().replace("_Date_", status.replace("Read ", ""));
		} else {
			Message message = Message.valueOf(status.toUpperCase());
			return message != null ? message.get() : Message.PRIVATE.get();
		}
	}

	public void setStatus(String status) {
		this.status = status;
		TigerReports.getInstance()
				.getDb()
				.updateAsynchronously("UPDATE tigerreports_comments SET status = ? WHERE report_id = ? AND comment_id = ?", Arrays.asList(this.status,
						r.getId(), commentId));
	}

	public String getAuthor() {
		return author;
	}

	public String getMessage() {
		return message;
	}

	public void addMessage(String message) {
		this.message += " "+message;
		TigerReports.getInstance()
				.getDb()
				.updateAsynchronously("UPDATE tigerreports_comments SET message = ? WHERE report_id = ? AND comment_id = ?", Arrays.asList(
						this.message, r.getId(), commentId));
	}

	public ItemStack getItem(boolean deletePermission) {
		return new CustomItem().type(Material.PAPER)
				.name(Message.COMMENT.get().replace("_Id_", Integer.toString(commentId)))
				.lore(Message.COMMENT_DETAILS.get()
						.replace("_Status_", getStatus(false))
						.replace("_Author_", author)
						.replace("_Date_", date)
						.replace("_Message_", MessageUtils.getMenuSentence(message, Message.COMMENT_DETAILS, "_Message_", true))
						.replace("_Actions_", Message.COMMENT_ADD_MESSAGE_ACTION.get()+(status.equals("Private")	? Message.COMMENT_SEND_ACTION.get()
																													: Message.COMMENT_CANCEL_SEND_ACTION
																															.get())+(deletePermission
																																						? Message.COMMENT_DELETE_ACTION
																																								.get()
																																						: ""))
						.split(ConfigUtils.getLineBreakSymbol()))
				.create();
	}

	public void delete() {
		TigerReports.getInstance()
				.getDb()
				.updateAsynchronously("DELETE FROM tigerreports_comments WHERE report_id = ? AND comment_id = ?", Arrays.asList(r.getId(),
						commentId));
	}

}
