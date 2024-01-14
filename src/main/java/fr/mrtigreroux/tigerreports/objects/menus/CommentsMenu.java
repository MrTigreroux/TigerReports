package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.List;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */

public class CommentsMenu extends ReportManagerMenu
        implements UpdatedMenu, ReportsManager.ReportCommentsPageListener {
    
    private static final Logger LOGGER = Logger.fromClass(CommentsMenu.class);
    
    private final VaultManager vm;
    private final BungeeManager bm;
    
    public CommentsMenu(User u, int page, int reportId, ReportsManager rm, Database db,
            TaskScheduler taskScheduler, UsersManager um, BungeeManager bm, VaultManager vm) {
        super(u, 54, page, Permission.STAFF, reportId, rm, db, taskScheduler, um);
        this.vm = vm;
        this.bm = bm;
    }
    
    @Override
    public Inventory onOpen() {
        rm.addReportCommentsPagesListener(reportId, page, this, db, taskScheduler, um);
        
        Inventory inv =
                getInventory(Message.COMMENTS_TITLE.get().replace("_Report_", r.getName()), true);
        
        inv.setItem(0, r.getItem(Message.REPORT_SHOW_ACTION.get(), vm, bm));
        inv.setItem(8, MenuItem.WRITE_COMMENT.create());
        
        return inv;
    }
    
    @Override
    public void onUpdate(Inventory inv) {
        inv.setItem(
                4,
                MenuItem.COMMENTS.getCustomItem()
                        .details(Message.PAGE_INFO.get().replace("_Page_", Integer.toString(page)))
                        .amount(page)
                        .create()
        );
        List<Comment> pageComments = rm.getReportCommentsCachedPageComments(reportId, page);
        int index = 0;
        boolean deletePerm = u.hasPermission(Permission.STAFF_DELETE);
        
        for (int slot = 18; slot < 45; slot++) {
            if (index >= pageComments.size()) {
                inv.setItem(slot, null);
            } else {
                Comment c = pageComments.get(index);
                if (c == null) {
                    inv.setItem(slot, null);
                } else {
                    inv.setItem(slot, c.getItem(deletePerm, vm));
                    index++;
                }
            }
        }
        
        inv.setItem(
                size - 7,
                page >= 2 ? MenuItem.PAGE_SWITCH_PREVIOUS.get() : MenuRawItem.GUI.create()
        );
        
        inv.setItem(
                size - 3,
                rm.isCachedReportCommentsPageNotEmpty(reportId, page + 1) ? MenuItem.PAGE_SWITCH_NEXT.get() : MenuRawItem.GUI.create()
        );
    }
    
    @Override
    public void onClick(ItemStack item, int slot, ClickType click) {
        if (slot == 0) {
            u.openReportMenu(r, rm, db, taskScheduler, vm, bm, um);
        } else if (slot == 8) {
            u.startCreatingComment(r);
            ConfigSound.MENU.play(p);
        } else if (slot >= 18 && slot <= size - 10) {
            Comment c = rm.getCommentAtIndexInPage(reportId, page, slot - 18);
            
            if (c == null) {
                update(false);
            } else {
                switch (click) {
                    case LEFT:
                    case SHIFT_LEFT:
                        u.startEditingComment(c);
                        ConfigSound.MENU.play(p);
                        return;
                    case RIGHT:
                    case SHIFT_RIGHT:
                        r.getReporter().toggleCommentSentState(c, db, taskScheduler, vm, bm);
                        ConfigSound.MENU.play(p);
                        break;
                    case DROP:
                        if (u.hasPermission(Permission.STAFF_DELETE)) {
                            c.delete(db, rm);
                            ConfigSound.MENU.play(p);
                        } else {
                            update(false);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        
    }
    
    @Override
    public void onReportCommentsPageChanged(int page) {
        if (page == this.page) { // Could be an event fired before page change.
            LOGGER.info(() -> this + ": onReportCommentsPageChanged(" + page + ")");
            update(false);
        }
    }
    
    @Override
    public void onPageChange(int oldPage, int newPage) {
        LOGGER.info(() -> this + ": onPageChange(" + oldPage + ", " + newPage + ")");
        rm.removeReportCommentsPagesListener(reportId, oldPage, this);
        rm.addReportCommentsPagesListener(reportId, newPage, this, db, taskScheduler, um);
        super.onPageChange(oldPage, newPage);
    }
    
    @Override
    public void onClose() {
        LOGGER.info(() -> this + ": onClose()");
        rm.removeReportCommentsPagesListener(reportId, page, this);
        super.onClose();
    }
    
}
