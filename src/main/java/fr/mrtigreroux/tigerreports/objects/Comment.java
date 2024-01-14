package fr.mrtigreroux.tigerreports.objects;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class Comment {
    
    private final Report r;
    private final Integer commentId;
    private final String date;
    private final User author;
    private String status, message;
    
    public Comment(Report r, Integer commentId, String status, String date, User author,
            String message) {
        this.r = Objects.requireNonNull(r);
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
        if (config) {
            return status;
        }
        
        if (status == null) {
            return Message.PRIVATE.get();
        } else if (status.startsWith("Read")) {
            return Message.READ.get().replace("_Date_", status.replace("Read ", ""));
        } else {
            Message message = Message.valueOf(status.toUpperCase());
            return message != null ? message.get() : Message.PRIVATE.get();
        }
    }
    
    public void setStatus(String status, Database db, ReportsManager rm) {
        updateStatusWithBroadcast(status, rm);
        db.updateAsynchronously(
                "UPDATE tigerreports_comments SET status = ? WHERE report_id = ? AND comment_id = ?",
                Arrays.asList(this.status, r.getId(), commentId)
        );
    }
    
    public UUID getAuthorUniqueId() {
        return author != null ? author.getUniqueId() : null;
    }
    
    public String getAuthorDisplayName(VaultManager vm) {
        return author != null ? author.getDisplayName(vm, true) : null;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void addMessage(String message, Database db, ReportsManager rm) {
        this.message += " " + message;
        rm.broadcastCommentDataChanged(this);
        
        db.updateAsynchronously(
                "UPDATE tigerreports_comments SET message = ? WHERE report_id = ? AND comment_id = ?",
                Arrays.asList(this.message, r.getId(), commentId)
        );
    }
    
    public ItemStack getItem(boolean deletePermission, VaultManager vm) {
        return new CustomItem().type(Material.PAPER)
                .name(Message.COMMENT.get().replace("_Id_", Integer.toString(commentId)))
                .lore(Message.COMMENT_DETAILS.get().replace("_Status_", getStatus(false)).replace("_Author_", getAuthorDisplayName(vm)).replace("_Date_", date).replace("_Message_", MessageUtils.getMenuSentence(message, Message.COMMENT_DETAILS, "_Message_", true)).replace("_Actions_", Message.COMMENT_ADD_MESSAGE_ACTION.get() + (status.equals("Private") ? Message.COMMENT_SEND_ACTION.get() : Message.COMMENT_CANCEL_SEND_ACTION.get()) + (deletePermission ? Message.COMMENT_DELETE_ACTION.get() : "")).split(ConfigUtils.getLineBreakSymbol())).create();
    }
    
    public void delete(Database db, ReportsManager rm) {
        db.updateAsynchronously(
                "DELETE FROM tigerreports_comments WHERE report_id = ? AND comment_id = ?",
                Arrays.asList(r.getId(), commentId)
        );
        rm.commentIsDeleted(this);
    }
    
    public boolean update(Map<String, Object> result) {
        boolean changed = false;
        
        changed |= updateStatus((String) result.get("status"));
        changed |= updateMessage((String) result.get("message"));
        
        return changed;
    }
    
    private boolean updateStatus(String newStatus) {
        if (!Objects.equals(status, newStatus)) {
            status = newStatus;
            return true;
        } else {
            return false;
        }
    }
    
    private boolean updateMessage(String newMessage) {
        if (!Objects.equals(message, newMessage)) {
            message = newMessage;
            return true;
        } else {
            return false;
        }
    }
    
    private boolean updateStatusWithBroadcast(String newStatus, ReportsManager rm) {
        boolean changed = updateStatus(newStatus);
        if (changed) {
            rm.broadcastCommentDataChanged(this);
        }
        return changed;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(commentId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Comment)) {
            return false;
        }
        Comment other = (Comment) obj;
        return Objects.equals(commentId, other.commentId);
    }
    
}
