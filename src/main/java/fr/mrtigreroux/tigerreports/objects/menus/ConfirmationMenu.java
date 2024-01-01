package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class ConfirmationMenu extends ReportManagerMenu {

    public enum Action {

        ARCHIVE("ARCHIVE", Permission.STAFF_ARCHIVE),
        DELETE_ARCHIVE("DELETE", Permission.STAFF_DELETE),
        DELETE("DELETE", Permission.STAFF_DELETE);

        String displayedName;
        Permission perm;

        Action(String displayedName, Permission perm) {
            this.displayedName = displayedName;
            this.perm = perm;
        }

        public String getDisplayedName() {
            return displayedName;
        }

        public Permission getRequiredPermission() {
            return perm;
        }

    }

    private Action action;
    private final VaultManager vm;
    private final BungeeManager bm;

    public ConfirmationMenu(User u, int reportId, Action action, ReportsManager rm, Database db,
            TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
        super(u, 27, 0, Permission.STAFF, reportId, action == Action.DELETE_ARCHIVE, rm, db, taskScheduler, um);
        this.action = action;
        this.vm = vm;
        this.bm = bm;
    }

    @Override
    public Inventory onOpen() {
        String report = r.getName();
        String actionDisplayed = action.getDisplayedName();

        Inventory inv = getInventory(
                Message.valueOf("CONFIRM_" + actionDisplayed + "_TITLE").get().replace("_Report_", report), false);

        ItemStack gui = MenuRawItem.GUI.create();
        for (int position : new int[] { 1, 2, 3, 5, 6, 7, 10, 12, 14, 16, 19, 20, 21, 23, 24, 25 }) {
            inv.setItem(position, gui);
        }

        inv.setItem(11,
                MenuRawItem.GREEN_CLAY.clone()
                        .name(Message.valueOf("CONFIRM_" + actionDisplayed).get())
                        .lore(Message.valueOf("CONFIRM_" + actionDisplayed + "_DETAILS")
                                .get()
                                .replace("_Report_", report)
                                .split(ConfigUtils.getLineBreakSymbol()))
                        .create());

        inv.setItem(13, r.getItem(null, vm, bm));
        inv.setItem(15,
                MenuRawItem.RED_CLAY.clone()
                        .name(Message.valueOf("CANCEL_" + actionDisplayed).get())
                        .lore(Message.valueOf("CANCEL_" + actionDisplayed + "_DETAILS")
                                .get()
                                .split(ConfigUtils.getLineBreakSymbol()))
                        .create());

        return inv;
    }

    @Override
    public void onClick(ItemStack item, int slot, ClickType click) {
        if (slot == 11) {
            if (!u.hasPermission(action.getRequiredPermission())) {
                u.openReportMenu(r.getId(), rm, db, taskScheduler, vm, bm, um);
                return;
            }

            u.openReportsMenu(1, false, rm, db, taskScheduler, vm, bm, um);
            switch (action) {
            case DELETE:
                r.delete(u, false, db, taskScheduler, rm, vm, bm);
                break;
            case DELETE_ARCHIVE:
                r.deleteFromArchives(u, false, db, taskScheduler, rm, vm, bm);
                break;
            default:
                r.archive(u, false, db, rm, vm, bm);
                break;
            }
        } else if (slot == 15) {
            if (action == Action.DELETE_ARCHIVE) {
                u.openArchivedReportsMenu(1, true, rm, db, taskScheduler, vm, bm, um);
            } else {
                u.openReportMenu(r.getId(), rm, db, taskScheduler, vm, bm, um);
            }
        }
    }

}
