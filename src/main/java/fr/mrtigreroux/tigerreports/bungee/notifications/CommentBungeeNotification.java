package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class CommentBungeeNotification extends BungeeNotification {
    
    public final int reportId;
    public final int commentId;
    public final String receiverUniqueId;
    
    public CommentBungeeNotification(long creationTime, int reportId, int commentId,
            UUID receiverUUID) {
        this(creationTime, reportId, commentId, receiverUUID.toString());
    }
    
    public CommentBungeeNotification(long creationTime, int reportId, int commentId,
            String receiverUniqueId) {
        super(creationTime);
        this.reportId = reportId;
        this.commentId = commentId;
        this.receiverUniqueId = CheckUtils.notEmpty(receiverUniqueId);
    }
    
    @Override
    public boolean isEphemeral() {
        return true;
    }
    
    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        User ru = um.getOnlineUser(receiverUniqueId);
        if (ru == null) {
            return;
        }
        rm.getReportByIdAsynchronously(reportId, false, isNotifiable(bm), db, ts, um, (r) -> {
            if (r != null) {
                r.getCommentByIdAsynchronously(commentId, db, ts, um, (c) -> {
                    ru.sendCommentNotification(r, c, true, db, vm, bm);
                });
            }
        });
    }
    
}
