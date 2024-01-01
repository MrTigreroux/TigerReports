package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */

public class ArchivedReportsMenu extends ReportsPageMenu {

    private final VaultManager vm;
    private final BungeeManager bm;

    public ArchivedReportsMenu(User u, int page, ReportsManager rm, Database db, TaskScheduler taskScheduler,
            VaultManager vm, BungeeManager bm, UsersManager um) {
        super(u, 54, page, Permission.STAFF_ARCHIVE, ReportsCharacteristics.ARCHIVED_REPORTS, rm, db, taskScheduler,
                um);
        this.vm = vm;
        this.bm = bm;
    }

    @Override
    public CustomItem getPageDisplayerItem() {
        return MenuItem.ARCHIVED_REPORTS.getCustomItem();
    }

    @Override
    protected Inventory onOpen() {
        Inventory inv = getInventory(Message.ARCHIVED_REPORTS_TITLE.get(), true);

        inv.setItem(0, MenuItem.REPORTS.getWithDetails(Message.REPORTS_DETAILS.get()));

        return inv;
    }

    @Override
    public void onUpdate(Inventory inv) {
        super.onUpdate(inv);
        rm.fillInventoryWithReportsPage(inv, reportsPage,
                Message.REPORT_RESTORE_ACTION.get()
                        + (u.hasPermission(Permission.STAFF_DELETE) ? Message.REPORT_DELETE_ACTION.get() : ""),
                false, "", vm, bm);
    }

    @Override
    public void onClick(ItemStack item, int slot, ClickType click) {
        if (slot == 0) {
            u.openReportsMenu(1, true, rm, db, taskScheduler, vm, bm, um);
        } else if (slot >= 18 && slot <= size - 10) {
            int reportId = reportsPage.getReportIdAtIndex(slot - 18);

            if (reportId == -1) {
                update(false);
            } else {
                rm.getReportByIdAsynchronously(reportId, false, true, db, taskScheduler, um,
                        new ResultCallback<Report>() {

                            @Override
                            public void onResultReceived(Report r) {
                                if (r == null) {
                                    update(true);
                                } else if (click == ClickType.DROP && u.hasPermission(Permission.STAFF_DELETE)) {
                                    u.openConfirmationMenu(r, ConfirmationMenu.Action.DELETE_ARCHIVE, rm, db,
                                            taskScheduler, vm, bm, um);
                                } else {
                                    r.unarchive(u, false, db, rm, vm, bm);
                                    u.openReportsMenu(1, false, rm, db, taskScheduler, vm, bm, um);
                                }
                            }

                        });
            }
        }
    }

}
