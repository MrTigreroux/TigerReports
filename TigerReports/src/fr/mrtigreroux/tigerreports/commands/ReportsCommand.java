package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportsCommand implements TabExecutor {

	private final List<String> ACTIONS = Arrays.asList("reload", "notify", "archiveall", "archives", "deleteall", "user", "stopcooldown", "#1");
	private final List<String> USER_ACTIONS = Arrays.asList("user", "u", "stopcooldown", "sc");

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (Permission.MANAGE.check(s)) {
				MenuUpdater.stop(true);
				TigerReports plugin = TigerReports.getInstance();
				plugin.unload();
				plugin.load();
				plugin.getReportsManager().clearReports();
				plugin.getUsersManager().clearUsers();
				plugin.initializeDatabase();
				BungeeManager bm = plugin.getBungeeManager();
				bm.collectServerName();
				bm.collectOnlinePlayers();

				if (s instanceof Player) {
					s.sendMessage(Message.RELOAD.get());
				} else {
					MessageUtils.sendConsoleMessage(Message.RELOAD.get());
				}
			}
			return true;
		}

		if (!UserUtils.checkPlayer(s) || !Permission.STAFF.check(s))
			return true;
		Player p = (Player) s;
		OnlineUser u = TigerReports.getInstance().getUsersManager().getOnlineUser(p);

		switch (args.length) {
			case 0:
				u.openReportsMenu(1, true);
				return true;
			case 1:
				switch (args[0].toLowerCase()) {
					case "canceledit":
						u.cancelComment();
						return true;
					case "notify":
						boolean newState = !u.acceptsNotifications();
						u.setStaffNotifications(newState);
						p.sendMessage(Message.STAFF_NOTIFICATIONS.get().replace("_State_", (newState ? Message.ACTIVATED : Message.DISABLED).get()));
						return true;
					case "archiveall":
						if (Permission.STAFF_ARCHIVE.check(s)) {
							TigerReports.getInstance()
									.getDb()
									.updateAsynchronously("UPDATE tigerreports_reports SET archived = ? WHERE archived = ? AND status LIKE 'Done%'",
											Arrays.asList(1, 0));
							MessageUtils.sendStaffMessage(Message.STAFF_ARCHIVEALL.get().replace("_Player_", p.getName()), ConfigSound.STAFF.get());
						}
						return true;
					case "archives":
						if (Permission.STAFF_ARCHIVE.check(s))
							u.openArchivedReportsMenu(1, true);
						return true;
					case "deleteall":
						if (Permission.STAFF_DELETE.check(s)) {
							TigerReports.getInstance()
									.getDb()
									.update("DELETE FROM tigerreports_reports WHERE archived = ?;", Collections.singletonList(1));
							MessageUtils.sendStaffMessage(Message.STAFF_DELETEALL.get().replace("_Player_", p.getName()), ConfigSound.STAFF.get());
						}
						return true;
					default:
						try {
							u.openReportMenu(TigerReports.getInstance()
									.getReportsManager()
									.getReportById(Integer.parseInt(args[0].replace("#", "")), true));
						} catch (Exception invalidIndex) {
							MessageUtils.sendErrorMessage(s, Message.INVALID_REPORT_ID.get().replace("_Id_", args[0]));
						}
						return true;
				}
			case 2:
				String tuuid = UserUtils.getUniqueId(args[1]);
				User tu = TigerReports.getInstance().getUsersManager().getUser(tuuid);
				if (tu == null || !UserUtils.isValid(tuuid)) {
					MessageUtils.sendErrorMessage(s, Message.INVALID_PLAYER.get().replace("_Player_", args[1]));
					return true;
				}

				switch (args[0].toLowerCase()) {
					case "user":
					case "u":
						u.openUserMenu(tu);
						return true;
					case "stopcooldown":
					case "sc":
						tu.stopCooldown(p.getName(), false);
						return true;
					default:
						break;
				}
				break;
			default:
				break;
		}
		s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORTS_SYNTAX.get()));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		switch (args.length) {
			case 1:
				return StringUtil.copyPartialMatches(args[0], ACTIONS, new ArrayList<>());
			case 2:
				return USER_ACTIONS.contains(args[0].toLowerCase()) ? StringUtil.copyPartialMatches(args[1], UserUtils.getOnlinePlayers(),
						new ArrayList<>()) : new ArrayList<>();
			default:
				return new ArrayList<>();
		}
	}

}
