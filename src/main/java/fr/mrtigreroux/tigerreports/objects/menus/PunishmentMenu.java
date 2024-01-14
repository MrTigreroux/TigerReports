package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public class PunishmentMenu extends ReportManagerMenu {
    
    private Map<Integer, String> configIndexes = new HashMap<>();
    private final VaultManager vm;
    private final BungeeManager bm;
    
    public PunishmentMenu(User u, int page, int reportId, ReportsManager rm, Database db,
            TaskScheduler taskScheduler, VaultManager vm, BungeeManager bm, UsersManager um) {
        super(u, 54, page, Permission.STAFF, reportId, rm, db, taskScheduler, um);
        this.vm = vm;
        this.bm = bm;
    }
    
    @Override
    public Inventory onOpen() {
        Inventory inv = getInventory(
                Message.PUNISH_TITLE.get()
                        .replace(
                                "_Reported_",
                                r.getPlayerName(
                                        Report.ParticipantType.REPORTED,
                                        false,
                                        false,
                                        vm,
                                        bm
                                )
                        ),
                true
        );
        
        inv.setItem(0, MenuItem.CANCEL_PROCESS.get());
        inv.setItem(4, MenuItem.PUNISHMENTS.create());
        inv.setItem(
                8,
                MenuRawItem.GREEN_CLAY.clone().name(Message.NO_PUNISHMENT.get()).lore(Message.NO_PUNISHMENT_DETAILS.get().replace("_Reported_", r.getPlayerName(Report.ParticipantType.REPORTED, false, true, vm, bm)).split(ConfigUtils.getLineBreakSymbol())).create()
        );
        
        int firstPunishment = 1;
        if (page >= 2) {
            inv.setItem(size - 7, MenuItem.PAGE_SWITCH_PREVIOUS.get());
            firstPunishment += (page - 1) * 27;
        }
        
        int slot = ConfigUtils.isEnabled("Config.Punishments.DefaultReasons")
                ? implement(inv, "Config.DefaultReasons.Reason", firstPunishment, 18)
                : 18;
        if (slot < 44) {
            slot = implement(
                    inv,
                    "Config.Punishments.Punishment",
                    slot == 18 ? firstPunishment : 1,
                    slot
            );
        }
        if (slot == 45) {
            inv.setItem(size - 3, MenuItem.PAGE_SWITCH_NEXT.get());
        }
        return inv;
    }
    
    /**
     * @return 45 if it remains punishments that could not be displayed in current page, 46 else.
     */
    private int implement(Inventory inv, String prepath, int firstConfigIndex, int firstSlot) {
        int slot = firstSlot;
        
        FileConfiguration configFile = ConfigFile.CONFIG.get();
        final String PUNISHMENT_MESSAGE = Message.PUNISHMENT.get();
        final String PUNISHMENT_DETAILS_MESSAGE = Message.PUNISHMENT_DETAILS.get();
        final String LINE_BREAK_SYMBOL = ConfigUtils.getLineBreakSymbol();
        
        for (
                int configIndex = firstConfigIndex;
                configIndex <= firstConfigIndex + 27;
                configIndex++
        ) {
            String path = prepath + configIndex;
            boolean punishCommandsExists = ConfigUtils.exists(configFile, path + ".PunishCommands");
            if (slot > 44) {
                return punishCommandsExists ? 45 : 46;
            }
            if (!ConfigUtils.exists(configFile, path)) {
                break;
            }
            if (!punishCommandsExists) {
                continue;
            }
            String permission = configFile.getString(path + ".PunishCommandsPermission");
            if (permission != null && !p.hasPermission(permission)) {
                continue;
            }
            
            String punishment = configFile.getString(path + ".Name");
            if (punishment != null) {
                punishment = punishment.replace("_StaffReason_", Message.STAFF_REASON.get());
            }
            String lore = configFile.getString(path + ".Lore");
            inv.setItem(
                    slot,
                    new CustomItem().fromConfig(configFile, path + ".Item").name(PUNISHMENT_MESSAGE.replace("_Punishment_", punishment)).lore(PUNISHMENT_DETAILS_MESSAGE.replace("_Reported_", r.getPlayerName(Report.ParticipantType.REPORTED, false, true, vm, bm)).replace("_Punishment_", punishment).replace("_Lore_", MessageUtils.translateColorCodes(lore != null ? lore : "")).split(LINE_BREAK_SYMBOL)).hideFlags(true).create()
            );
            int len = prepath.length();
            configIndexes.put(slot, prepath.substring(len - 1) + configIndex);
            slot++;
        }
        return slot;
    }
    
    @Override
    public void onClick(ItemStack item, int slot, ClickType click) {
        if (slot == 0) {
            u.openReportMenu(r.getId(), rm, db, taskScheduler, vm, bm, um);
        } else if (slot == 8) {
            u.afterProcessingAReport(true, rm, db, taskScheduler, vm, bm, um);
            r.process(
                    u,
                    Appreciation.TRUE,
                    false,
                    u.hasPermission(Permission.STAFF_ARCHIVE_AUTO),
                    true,
                    db,
                    rm,
                    vm,
                    bm,
                    taskScheduler
            );
        } else if (slot >= 18 && slot <= size - 10) {
            ConfigSound.MENU.play(p);
            String configIndex = configIndexes.get(slot);
            String punishmentConfigPath = (configIndex.charAt(0) == 't'
                    ? "Config.Punishments.Punishment"
                    : "Config.DefaultReasons.Reason") + configIndex.substring(1);
            
            FileConfiguration configFile = ConfigFile.CONFIG.get();
            
            if (
                ConfigUtils.isPlaceholderUsedInCommands(
                        configFile,
                        punishmentConfigPath + ".PunishCommands",
                        "_StaffReason_"
                )
            ) {
                u.startProcessPunishingWithStaffReason(r, punishmentConfigPath);
            } else {
                u.processPunishing(
                        r,
                        punishmentConfigPath,
                        configFile,
                        rm,
                        db,
                        taskScheduler,
                        vm,
                        bm
                );
            }
        }
    }
    
}
