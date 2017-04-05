package fr.mrtigreroux.tigerreports.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if(!UserUtils.checkPlayer(s) || (ReportUtils.permissionRequired() && !UserUtils.checkPermission(s, Permission.REPORT.get()))) return true;
		Player p = (Player) s;
		OnlineUser u = UserUtils.getOnlineUser(p);
		String uuid = p.getUniqueId().toString();
		
		String cooldown = u.getCooldown();
		if(cooldown != null && !cooldown.equals("None")) {
			MessageUtils.sendErrorMessage(p, Message.COOLDOWN.get().replace("_Time_", cooldown));
			return true;
		}
		
		if(args.length == 0 || (args.length == 1 && !ConfigUtils.exist(ConfigFile.CONFIG.get(), "Config.DefaultReasons.Reason1"))) {
			s.sendMessage(Message.INVALID_SYNTAX.get().replace("_Command_", "/"+label+" "+Message.REPORT_SYNTAX.get()));
			return true;
		}
		
		String reportedName = args[0];
		if(reportedName.equalsIgnoreCase(p.getName())) {
			MessageUtils.sendErrorMessage(p, Message.REPORT_ONESELF.get());
			return true;
		}
		
		Player rp = UserUtils.getPlayer(reportedName);
		String ruuid = UserUtils.getUniqueId(reportedName);
		if(rp == null && !UserUtils.isValid(ruuid)) {
			MessageUtils.sendErrorMessage(p, Message.INVALID_PLAYER.get().replace("_Player_", reportedName));
			return true;
		}
		
		if(ReportUtils.onlinePlayerRequired() && !UserUtils.isOnline(reportedName)) {
			MessageUtils.sendErrorMessage(p, Message.REPORTED_OFFLINE.get().replace("_Player_", reportedName));
			return true;
		}
		
		User ru = UserUtils.getUser(ruuid);
		String reportedImmunity = ru.getImmunity();
		if(reportedImmunity != null) {
			if(reportedImmunity.equals("always")) MessageUtils.sendErrorMessage(p, Message.PERMISSION_REPORT.get().replace("_Player_", reportedName));
			else MessageUtils.sendErrorMessage(p, Message.PLAYER_ALREADY_REPORTED.get().replace("_Player_", reportedName).replace("_Time_", reportedImmunity));
			return true;
		}
		
		if(args.length == 1) {
			u.openReasonMenu(1, ru);
			return true;
		}
		
		StringBuilder sb = new StringBuilder();
		for(int argNumber = 1; argNumber < args.length ; argNumber++) sb.append(args[argNumber]+" ");
		String reason = sb.toString().trim();
		if(reason.length() < ReportUtils.getMinCharacters()) {
			MessageUtils.sendErrorMessage(p, Message.TOO_SHORT_REASON.get().replace("_Reason_", reason));
			return true;
		}
		if(ConfigUtils.getLineBreakSymbol().length() >= 1) reason = reason.replace(ConfigUtils.getLineBreakSymbol(), ConfigUtils.getLineBreakSymbol().substring(0, 1));
		
		 if(!ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.CustomReasons")) {
			for(int reasonNumber = 1; reasonNumber <= 100; reasonNumber++) {
				String defaultReason = ConfigFile.CONFIG.get().getString("Config.DefaultReasons.Reason"+reasonNumber+".Name");
				if(defaultReason == null) {
					u.openReasonMenu(1, ru);
					return true;
				} else if(reason.equals(defaultReason)) break;
			}
		}
		
		int reportId = (reportId = ReportUtils.getTotalReports()+1) <= ReportUtils.getMaxReports() ? reportId : -1;
		
		if(reportId != -1) {
			List<Object> parameters;
			if(rp != null) parameters = Arrays.asList(Status.WAITING.getConfigWord(), "None", MessageUtils.getNowDate(), ruuid, uuid.toString(), reason, rp.getAddress().toString(), MessageUtils.formatConfigLocation(rp.getLocation()), UserUtils.getOnlineUser(rp).getLastMessages(), rp.getGameMode().toString().toLowerCase(), rp.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR, rp.isSneaking(), rp.isSprinting(), MessageUtils.cleanDouble(rp.getHealth())+"/"+MessageUtils.cleanDouble(rp.getMaxHealth()), rp.getFoodLevel(), MessageUtils.formatConfigEffects(rp.getActivePotionEffects()), p.getAddress().toString(), MessageUtils.formatConfigLocation(p.getLocation()), u.getLastMessages());
			else parameters = Arrays.asList(Status.WAITING.getConfigWord(), "None", MessageUtils.getNowDate(), ruuid, uuid.toString(), reason, null, null, null, null, null, null, null, null, null, null, p.getAddress().toString(), MessageUtils.formatConfigLocation(p.getLocation()), u.getLastMessages());
			reportId = TigerReports.getDb().insert("INSERT INTO reports (status,appreciation,date,reported_uuid,signalman_uuid,reason,reported_ip,reported_location,reported_messages,reported_gamemode,reported_on_ground,reported_sneak,reported_sprint,reported_health,reported_food,reported_effects,signalman_ip,signalman_location,signalman_messages) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);", parameters);
		}
		
		Report r = new Report(reportId, Status.WAITING.getConfigWord(), "None", MessageUtils.getNowDate(), ruuid, uuid, reason);
		ReportUtils.sendReport(r);
		s.sendMessage(Message.REPORT_SENT.get().replace("_Player_", r.getPlayerName("Reported", false)).replace("_Reason_", reason));
		TigerReports.getBungeeManager().sendPluginNotification(reportId+" new_report "+MessageUtils.getNowDate().replace(" ", "_")+" "+ruuid+" "+uuid+" "+reason.replace(" ", "_"));
		
		u.startCooldown(ReportUtils.getCooldown(), false);
		ru.startImmunity(false);
		u.changeStatistic("reports", 1, false);
		ru.changeStatistic("reported_times", 1, false);
		return true;
	}

}
