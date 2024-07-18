package fr.mrtigreroux.tigerreports.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.GlobalLogger;
import fr.mrtigreroux.tigerreports.logs.Level;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UpdatesManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.FileUtils;
import fr.mrtigreroux.tigerreports.utils.LogUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportsCommand implements TabExecutor {
    
    private static final Logger LOGGER = Logger.fromClass(ReportsCommand.class);
    private static final List<String> ACTIONS = Arrays.asList(
            "reload",
            "notify",
            "archive",
            "delete",
            "comment",
            "archives",
            "archiveall",
            "deleteall",
            "user",
            "stopcooldown",
            "punish",
            "#1"
    );
    private static final List<String> USER_ACTIONS =
            Arrays.asList("user", "u", "stopcooldown", "sc", "punish");
    private static final List<String> DELETEALL_ARGS = Arrays.asList("archived", "unarchived");
    
    private final ReportsManager rm;
    private final Database db;
    private final TigerReports tr;
    private final BungeeManager bm;
    private final VaultManager vm;
    private final UsersManager um;
    
    public ReportsCommand(ReportsManager rm, Database db, TigerReports tr, BungeeManager bm,
            VaultManager vm, UsersManager um) {
        this.rm = rm;
        this.db = db;
        this.tr = tr;
        this.bm = bm;
        this.vm = vm;
        this.um = um;
    }
    
    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            User u = checkStaffAction(0, s, args);
            if (u == null) {
                return true;
            }
            u.openReportsMenu(1, true, rm, db, tr, vm, bm, um);
            return true;
        }
        
        if (args.length == 1) {
            try {
                int reportId = Integer.parseInt(args[0].replace("#", ""));
                User u = checkStaffAction(1, s, args);
                if (u == null) {
                    return true;
                }
                if (reportId >= 0) {
                    u.openReportMenu(reportId, rm, db, tr, vm, bm, um);
                } else {
                    MessageUtils.sendErrorMessage(
                            s,
                            Message.INVALID_REPORT_ID.get().replace("_Id_", args[0])
                    );
                }
                return true;
            } catch (NumberFormatException ex) {
                // The user probably wanted to type a special action.
            }
        }
        
        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                case "user":
                case "u":
                    processUser(s, args);
                    return true;
                case "stopcooldown":
                case "sc":
                    processStopCooldown(s, args);
                    return true;
                case "canceledit":
                    processCancelEdit(s, args);
                    return true;
                case "notify":
                    processNotify(s, args);
                    return true;
                case "archiveall":
                    processArchiveAll(s, args);
                    return true;
                case "deleteall":
                    processDeleteAll(s, args);
                    return true;
                case "archives":
                    processArchives(s, args);
                    return true;
                case "reload":
                    processReload(s, args);
                    return true;
                case "comment":
                    processComment(s, args);
                    return true;
                case "punish":
                    processPunish(s, args);
                    return true;
                case "archive":
                    processArchive(s, args);
                    return true;
                case "delete":
                    processDelete(s, args);
                    return true;
                case "logs":
                    processLogs(s, args);
                    return true;
                case "update_data":
                    processUpdateData(s, args);
                    return true;
                default:
                    break;
            }
        }
        
        sendInvalidSyntax(s);
        return true;
    }
    
    private void processReload(CommandSender s, String[] args) {
        if (!checkAction(Permission.MANAGE, 1, s, args)) {
            return;
        }
        
        tr.unload();
        tr.load();
        
        if (s instanceof Player) {
            s.sendMessage(Message.RELOAD.get());
        } else {
            MessageUtils.sendConsoleMessage(Message.RELOAD.get());
        }
    }
    
    private void processComment(CommandSender s, String[] args) {
        if (!Permission.STAFF.check(s)) {
            return;
        }
        if (args.length < 3) {
            sendInvalidSyntax(s);
            return;
        }
        
        String reportIdStr = args[1];
        int reportId = getReportIdOrSendError(reportIdStr, s);
        if (reportId < 0) {
            return;
        }
        rm.getReportByIdAsynchronously(reportId, false, true, db, tr, um, (r) -> {
            if (r == null) {
                MessageUtils.sendErrorMessage(
                        s,
                        Message.INVALID_REPORT_ID.get().replace("_Id_", reportIdStr)
                );
                return;
            }
            
            Player p = s instanceof Player ? (Player) s : null;
            String author = p != null ? p.getUniqueId().toString() : "";
            StringBuilder sb = new StringBuilder();
            for (int argIndex = 2; argIndex < args.length; argIndex++) {
                sb.append(args[argIndex]).append(" ");
            }
            String message = sb.toString().trim();
            
            r.addComment(author, message, db, tr, (id) -> {});
            if (p != null) {
                User u = um.getOnlineUser(p);
                if (u == null) {
                    LogUtils.logUnexpectedOfflineUser(LOGGER, "onCommand()", p);
                    return;
                }
                u.openCommentsMenu(1, r, rm, db, tr, um, bm, vm);
            }
        });
    }
    
    private void processLogs(CommandSender s, String[] args) {
        if (!Permission.MANAGE.check(s)) {
            return;
        }
        if (args.length < 2) {
            sendInvalidSyntax(s);
            return;
        }
        
        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("default")) {
                try {
                    File logsConfigFile =
                            FileUtils.getPluginDataFile(tr, Logger.LOGS_CONFIG_FILE_NAME);
                    logsConfigFile.delete();
                    s.sendMessage(
                            "\u00A77[\u00A76TigerReports\u00A77] " + ConfigUtils.getInfoMessage(
                                    "\u00A7eThe logs configuration has been set to default. Restart your server to use it.",
                                    "\u00A7eLa configuration des logs a \u00E9t\u00E9 remise par d\u00E9faut. Red\u00E9marrez votre serveur pour l'utiliser."
                            )
                    );
                } catch (SecurityException e) {
                    LOGGER.error("Could not edit the logs config file", e);
                    MessageUtils.sendErrorMessage(
                            s,
                            "\u00A77[\u00A76TigerReports\u00A77] " + ConfigUtils.getInfoMessage(
                                    "\u00A7cCould not set the logs configuration file to default. Read the error in the logs/console.",
                                    "\u00A7cLa configuration des logs n'a pas pu \u00EAtre remise par d\u00E9faut. Lisez l'erreur dans les logs/console."
                            )
                    );
                }
            } else {
                sendInvalidSyntax(s);
                return;
            }
        } else {
            // /reports logs 1 1 class:D main:I sql:I bungee:I events:I config:I
            
            List<String> newConfigLines = new ArrayList<>();
            String bukkitLoggersShowName =
                    Logger.LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE.equals(args[1])
                            ? Logger.LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE
                            : "0";
            String bukkitLoggersUseColors =
                    Logger.LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE.equals(args[2])
                            ? Logger.LOGS_CONFIG_FILE_GLOBAL_SETTING_TRUE_VALUE
                            : "0";
            newConfigLines.add(
                    bukkitLoggersShowName
                            + Logger.LOGS_CONFIG_FILE_GLOBAL_SETTINGS_SEPARATOR
                            + bukkitLoggersUseColors
            );
            
            Map<String, Level> providedGlobalLoggersLevel = new HashMap<>();
            for (int i = 3; i < args.length; i++) {
                String[] argParts = args[i].split(":");
                if (argParts.length != 2) {
                    continue;
                }
                String levelId = argParts[1];
                if (levelId == null || levelId.length() != 1) {
                    continue;
                }
                Level level = Level.fromId(levelId.charAt(0));
                providedGlobalLoggersLevel.put(argParts[0], level);
            }
            
            for (GlobalLogger gl : GlobalLogger.values()) {
                String loggerName = gl.getLoggerName();
                Level loggerLevel = providedGlobalLoggersLevel.get(loggerName);
                if (loggerLevel == null) {
                    loggerLevel = gl.getDefaultLevel();
                }
                newConfigLines.add(
                        loggerName
                                + Logger.LOGS_CONFIG_FILE_GLOBAL_LOGGER_DATA_SEPARATOR
                                + loggerLevel
                );
            }
            
            try {
                File logsConfigFile = FileUtils.getPluginDataFile(tr, Logger.LOGS_CONFIG_FILE_NAME);
                FileUtils.setFileLines(logsConfigFile, newConfigLines);
                s.sendMessage(
                        "\u00A77[\u00A76TigerReports\u00A77] " + ConfigUtils.getInfoMessage(
                                "\u00A7eThe logs configuration has been changed. Restart your server to use it.",
                                "\u00A7eLa configuration des logs a \u00E9t\u00E9 modifi\u00E9e. Red\u00E9marrez votre serveur pour l'utiliser."
                        )
                );
            } catch (SecurityException | IOException e) {
                LOGGER.error("Could not edit the logs config file", e);
                MessageUtils.sendErrorMessage(
                        s,
                        "\u00A77[\u00A76TigerReports\u00A77] " + ConfigUtils.getInfoMessage(
                                "\u00A7cCould not edit the logs configuration file. Read the error in the logs/console.",
                                "\u00A7cLe fichier de configuration des logs n'a pas pu \u00EAtre modifi\u00E9. Lisez l'erreur dans les logs/console."
                        )
                );
            }
        }
    }
    
    private void processUpdateData(CommandSender s, String[] args) {
        if (!checkAction(Permission.MANAGE, 2, s, args)) {
            return;
        }
        
        UpdatesManager.runUpdatesInstructions(args[1], tr, tr, db);
        UpdatesManager.updateLastVersionUsed(tr);
        tr.updateNeedUpdatesInstructions(false);
        if (s instanceof Player) {
            ConfigSound.STAFF.play((Player) s);
        }
        s.sendMessage(
                "\u00A77[\u00A76TigerReports\u00A77] " + ConfigUtils.getInfoMessage(
                        "\u00A7eData should have been \u00A7aupdated\u00A7e. Check if there is an error in the logs/console.",
                        "\u00A7eLes donn\u00E9es devraient avoir \u00E9t\u00E9 \u00A7amises \u00E0 jour\u00A7e. V\u00E9rifiez qu'il n'y ait pas d'erreur dans les logs/console."
                )
        );
    }
    
    private void processArchiveAll(CommandSender s, String[] args) {
        if (!checkAction(Permission.STAFF_ARCHIVE, 1, s, args)) {
            return;
        }
        
        db.updateAsynchronously(
                "UPDATE tigerreports_reports SET archived = ? WHERE archived = ? AND status LIKE 'Done%'",
                Arrays.asList(1, 0)
        );
        
        MessageUtils.sendStaffMessage(
                Message.STAFF_ARCHIVEALL.get().replace("_Player_", getOperatorName(s)),
                ConfigSound.STAFF.get()
        );
    }
    
    private void processDeleteAll(CommandSender s, String[] args) {
        if (!checkAction(Permission.STAFF_DELETE, 2, s, args)) {
            return;
        }
        
        String reportsType = args[1];
        boolean unarchived = reportsType.equalsIgnoreCase("unarchived");
        if (!unarchived && !reportsType.equalsIgnoreCase("archived")) {
            sendInvalidSyntax(s);
            return;
        }
        
        db.updateAsynchronously(
                "DELETE FROM tigerreports_reports WHERE archived = ?",
                Collections.singletonList(unarchived ? 0 : 1)
        );
        
        MessageUtils.sendStaffMessage(
                Message.get("Messages.Staff-deleteall-" + (unarchived ? "un" : "") + "archived")
                        .replace("_Player_", getOperatorName(s)),
                ConfigSound.STAFF.get()
        );
    }
    
    private void processCancelEdit(CommandSender s, String[] args) {
        User u = checkStaffAction(1, s, args);
        if (u == null) {
            return;
        }
        
        u.cancelEditingComment();
        u.cancelProcessPunishingWithStaffReason();
    }
    
    private void processNotify(CommandSender s, String[] args) {
        User u = checkStaffAction(1, s, args);
        if (u == null) {
            return;
        }
        
        boolean newState = !u.acceptsNotifications();
        u.setStaffNotifications(newState);
        u.sendMessage(
                Message.STAFF_NOTIFICATIONS.get()
                        .replace("_State_", (newState ? Message.ACTIVATED : Message.DISABLED).get())
        );
    }
    
    private void processDelete(CommandSender s, String[] args) {
        User u = checkUserAction(Permission.STAFF_DELETE, 2, s, args);
        if (u == null) {
            return;
        }
        
        getReportByIdAsync(args[1], s, db, (r) -> {
            r.delete(u, false, db, tr, rm, vm, bm);
        });
    }
    
    private void processArchive(CommandSender s, String[] args) {
        User u = checkUserAction(Permission.STAFF_ARCHIVE, 2, s, args);
        if (u == null) {
            return;
        }
        
        getReportByIdAsync(args[1], s, db, (r) -> {
            if (!r.isArchived()) {
                r.archive(u, false, db, rm, vm, bm);
            }
        });
    }
    
    private void processArchives(CommandSender s, String[] args) {
        User u = checkUserAction(Permission.STAFF_ARCHIVE, 1, s, args);
        if (u == null) {
            return;
        }
        
        u.openArchivedReportsMenu(1, true, rm, db, tr, vm, bm, um);
    }
    
    private void processUser(CommandSender s, String[] args) {
        User u = checkStaffAction(2, s, args);
        if (u == null) {
            return;
        }
        
        getTargetByNameAsync(u, args[1], (tu) -> {
            u.openUserMenu(tu, rm, db, tr, vm, um);
        });
    }
    
    private void processStopCooldown(CommandSender s, String[] args) {
        User u = checkStaffAction(2, s, args);
        if (u == null) {
            return;
        }
        
        getTargetByNameAsync(u, args[1], (tu) -> {
            tu.stopCooldown(u, false, db, bm);
        });
    }
    
    private void processPunish(CommandSender s, String[] args) {
        User u = checkStaffAction(3, s, args);
        if (u == null) {
            return;
        }
        
        try {
            long punishSeconds = Long.parseLong(args[2]);
            if (punishSeconds > 0) {
                getTargetByNameAsync(u, args[1], (tu) -> {
                    tu.punish(punishSeconds, u, false, db, bm, vm);
                });
                return;
            }
        } catch (NumberFormatException ex) {}
        MessageUtils.sendErrorMessage(s, Message.INVALID_TIME.get().replace("_Time_", args[2]));
    }
    
    private User checkStaffAction(int argsLen, CommandSender s, String[] args) {
        return checkUserAction(Permission.STAFF, argsLen, s, args);
    }
    
    private User checkUserAction(Permission perm, int argsLen, CommandSender s, String[] args) {
        if (checkAction(perm, argsLen, s, args)) {
            return getOnlineUser(s);
        } else {
            return null;
        }
    }
    
    private static boolean checkAction(Permission perm, int argsLen, CommandSender s,
            String[] args) {
        if (!perm.check(s)) {
            return false;
        }
        if (args.length != argsLen) {
            sendInvalidSyntax(s);
            return false;
        }
        return true;
    }
    
    private User getOnlineUser(CommandSender s) {
        if (!UserUtils.checkPlayer(s)) {
            return null;
        }
        Player p = (Player) s;
        User u = um.getOnlineUser(p);
        if (u == null) {
            LogUtils.logUnexpectedOfflineUser(LOGGER, "onCommand()", p);
            return null;
        }
        return u;
    }
    
    private static int getReportIdOrSendError(String reportId, CommandSender s) {
        int id = -1;
        try {
            id = Integer.parseInt(reportId.replace("#", ""));
        } catch (NumberFormatException ex) {}
        if (id < 0) {
            MessageUtils
                    .sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", reportId));
        }
        
        return id;
    }
    
    private void getReportByIdAsync(String reportId, CommandSender s, Database db,
            ResultCallback<Report> resultCallback) {
        int id = getReportIdOrSendError(reportId, s);
        if (id < 0) {
            return;
        }
        rm.getReportByIdAsynchronously(id, false, false, db, tr, um, (r) -> {
            if (r == null) {
                MessageUtils.sendErrorMessage(
                        s,
                        Message.INVALID_REPORT_ID.get().replace("_Id_", reportId)
                );
                return;
            }
            resultCallback.onResultReceived(r);
        });
    }
    
    private void getTargetByNameAsync(User u, String target, ResultCallback<User> resultCallback) {
        um.getUserByNameAsynchronously(target, db, tr, (tu) -> {
            if (tu == null) {
                u.sendErrorMessage(Message.INVALID_PLAYER.get().replace("_Player_", target));
                return;
            }
            resultCallback.onResultReceived(tu);
        });
    }
    
    private String getOperatorName(CommandSender s) {
        if (s instanceof Player) {
            User u = getOnlineUser(s);
            if (u == null) {
                LogUtils.logUnexpectedOfflineUser(LOGGER, "getOperatorName()", (Player) s);
                return "null";
            }
            return u.getDisplayName(vm, true);
        } else {
            return s.getName();
        }
    }
    
    private static void sendInvalidSyntax(CommandSender s) {
        for (
            String line : Message.INVALID_SYNTAX_REPORTS.get()
                    .split(ConfigUtils.getLineBreakSymbol())
        ) {
            s.sendMessage(line);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        switch (args.length) {
            case 1:
                return StringUtil
                        .copyPartialMatches(args[0].toLowerCase(), ACTIONS, new ArrayList<>());
            case 2:
                switch (args[0].toLowerCase()) {
                    case "deleteall":
                        return StringUtil.copyPartialMatches(
                                args[1].toLowerCase(),
                                DELETEALL_ARGS,
                                new ArrayList<>()
                        );
                    case "archive":
                    case "delete":
                        return StringUtil.copyPartialMatches(
                                args[1].toLowerCase(),
                                Collections.singletonList("#1"),
                                new ArrayList<>()
                        );
                    default:
                        return USER_ACTIONS.contains(args[0].toLowerCase()) && s instanceof Player
                                ? StringUtil.copyPartialMatches(
                                        args[1],
                                        UserUtils.getOnlinePlayersForPlayer(
                                                (Player) s,
                                                false,
                                                um,
                                                bm
                                        ),
                                        new ArrayList<>()
                                )
                                : new ArrayList<>();
                }
            default:
                return new ArrayList<>();
        }
    }
    
}
