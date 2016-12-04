package fr.mrtigreroux.tigerreports.commands;

import java.util.Collection;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
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
		if(!UserUtils.checkPlayer(s) || (ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.PermissionRequired") && !UserUtils.checkPermission(s, Permission.REPORT.get()))) return true;
		Player p = (Player) s;
		User u = UserUtils.getUser(p);
		
		if(ReportUtils.permissionRequired() && !u.hasPermission(Permission.REPORT)) {
			MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
			return true;
		}
		
		String cooldown = u.getCooldown();
		if(cooldown != null) {
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
		if(rp == null) {
			if(!UserUtils.isValid(ruuid)) {
				MessageUtils.sendErrorMessage(p, Message.INVALID_PLAYER.get().replace("_Player_", reportedName));
				return true;
			} else if(ReportUtils.onlinePlayerRequired()) {
				MessageUtils.sendErrorMessage(p, Message.REPORTED_OFFLINE.get().replace("_Player_", reportedName));
				return true;
			}
		} else if(rp.hasPermission(Permission.EXEMPT.get())) {
			MessageUtils.sendErrorMessage(p, Message.PERMISSION_REPORT.get().replace("_Player_", reportedName));
			return true;
		}
		
		if(UserUtils.LastTimeReported.containsKey(ruuid) && System.currentTimeMillis()-UserUtils.LastTimeReported.get(ruuid) < ConfigUtils.getReportedImmunity()) {
			long current = System.currentTimeMillis()/1000;
			long immunityEnd = (UserUtils.LastTimeReported.get(ruuid)+ConfigUtils.getReportedImmunity())/1000;
			if(current < immunityEnd) {
				MessageUtils.sendErrorMessage(p, Message.PLAYER_ALREADY_REPORTED.get().replace("_Player_", reportedName).replace("_Time_", MessageUtils.convertToSentence(immunityEnd-current)));
				return true;
			}
		}
		
		if(args.length == 1) {
			u.openReasonMenu(1, reportedName);
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
		
		int reportNumber = ReportUtils.getNewReportNumber();
		Report r = new Report(reportNumber);
		
		if(reportNumber == -1) MessageUtils.sendStaffMessage(Message.STAFF_MAX_REPORTS_REACHED.get().replace("_Number_", ""+ReportUtils.getMaxReports()), ConfigUtils.getStaffSound());
		else {
			String reportPath = r.getConfigPath();
			r.setStatus(Status.WAITING);
			ConfigFile.REPORTS.get().set(reportPath+".Appreciation", "None");
			ConfigFile.REPORTS.get().set(reportPath+".Date", MessageUtils.getNowDate());
			ConfigFile.REPORTS.get().set(reportPath+".Reason", reason);
			ConfigFile.REPORTS.get().set(reportPath+".Reported.UUID", ruuid);
			if(rp != null) {
				ConfigFile.REPORTS.get().set(reportPath+".Reported.IP", rp.getAddress().toString());
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Gamemode", rp.getGameMode().toString().toLowerCase());
				Location loc = rp.getLocation();
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Location", loc.getWorld().getName()+":"+loc.getX()+"/"+loc.getY()+"/"+loc.getZ()+"/"+loc.getYaw()+"/"+loc.getPitch());
				ConfigFile.REPORTS.get().set(reportPath+".Reported.OnGround", rp.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR);
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Sneak", rp.isSneaking());
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Sprint", rp.isSprinting());
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Health", MessageUtils.cleanDouble(rp.getHealth())+"/"+MessageUtils.cleanDouble(rp.getMaxHealth()));
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Food", rp.getFoodLevel());
				
				String configEffects = "";
				Collection<PotionEffect> effects = rp.getActivePotionEffects();
				for(PotionEffect effect : effects) configEffects += effect.getType().getName()+":"+effect.getAmplifier()+"/"+effect.getDuration()+",";
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Effects", configEffects.length() > 0 ? configEffects.substring(0, configEffects.length()-1): "None");
				ConfigFile.REPORTS.get().set(reportPath+".Reported.Messages", UserUtils.getUser(rp).getLastMessages());
			}
			ConfigFile.REPORTS.get().set(reportPath+".Signalman.UUID", p.getUniqueId().toString());
			ConfigFile.REPORTS.get().set(reportPath+".Signalman.IP", p.getAddress().toString());
			ConfigFile.REPORTS.get().set(reportPath+".Signalman.Gamemode", p.getGameMode().toString().toLowerCase());
			Location loc = p.getLocation();
			ConfigFile.REPORTS.get().set(reportPath+".Signalman.Location", loc.getWorld().getName()+":"+loc.getX()+"/"+loc.getY()+"/"+loc.getZ()+"/"+loc.getYaw()+"/"+loc.getPitch());
			ConfigFile.REPORTS.get().set(reportPath+".Signalman.Messages", u.getLastMessages());
			ConfigFile.REPORTS.save();
		}
		UserUtils.LastTimeReported.put(ruuid, System.currentTimeMillis());
		UserUtils.changeStat(p.getUniqueId().toString(), "Reports", 1);
		UserUtils.changeStat(ruuid.toString(), "ReportedTime", 1);
		
		TextComponent alert = new TextComponent(Message.ALERT.get().replace("_Signalman_", r.getPlayerName("Signalman", false)).replace("_Reported_", r.getPlayerName("Reported", !ReportUtils.onlinePlayerRequired())).replace("_Reason_", reason));
		alert.setColor(ChatColor.valueOf(MessageUtils.getLastColor(Message.ALERT.get(), "_Reason_").name()));
		if(reportNumber != -1) {
			alert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports #"+reportNumber));
			alert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Message.ALERT_DETAILS.get().replace("_Report_", r.getName())).create()));
		}
		MessageUtils.sendStaffMessage(alert, ConfigUtils.getReportSound());
		
		s.sendMessage(Message.REPORT_SENT.get().replace("_Player_", r.getPlayerName("Reported", false)).replace("_Reason_", reason));
		u.startCooldown(ReportUtils.getCooldown());
		return true;
	}

}
