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

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

public class ReportCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if(!UserUtils.isPlayer(s) || (ConfigUtils.isEnabled(FilesManager.getConfig, "Config.PermissionRequired") && !UserUtils.hasPermission(s, Permission.REPORT.get()))) return true;
		Player p = (Player) s;
		User u = new User(p);
		
		if(ReportUtils.permissionRequired() && !u.hasPermission(Permission.REPORT)) {
			MessageUtils.sendErrorMessage(s, Message.PERMISSION_COMMAND.get());
			return true;
		}
		
		String cooldown = u.getCooldown();
		if(cooldown != null) {
			MessageUtils.sendErrorMessage(p, Message.COOLDOWN.get().replaceAll("_Time_", cooldown));
			return true;
		}
		
		if(args.length <= 1) {
			s.sendMessage(Message.INVALID_SYNTAX.get().replaceAll("_Command_", "/"+label+" <joueur> <raison>"));
			return true;
		}
		
		String reported = args[0];
		if(reported.equalsIgnoreCase(p.getName())) {
			MessageUtils.sendErrorMessage(p, Message.REPORT_ONESELF.get());
			return true;
		}
		
		Player r = UserUtils.getPlayer(reported);
		if(ReportUtils.onlinePlayerRequired() && r == null) {
			MessageUtils.sendErrorMessage(p, Message.REPORTED_OFFLINE.get().replaceAll("_Player_", reported));
			return true;
		}
		
		StringBuilder sb = new StringBuilder();
		for(int argNumber = 1; argNumber < args.length ; argNumber++) sb.append(args[argNumber]+" ");
		String reason = sb.toString().trim();
		if(reason.length() < ReportUtils.getMinCharacters()) {
			MessageUtils.sendErrorMessage(p, Message.TOO_SHORT_REASON.get().replaceAll("_Reason_", reason));
			return true;
		}
		if(ConfigUtils.getLineBreakSymbol().length() >= 1) reason = reason.replaceAll(ConfigUtils.getLineBreakSymbol(), ConfigUtils.getLineBreakSymbol().substring(0, 1));
		
		int reportNumber = ReportUtils.getNewReportNumber();
		if(reportNumber != -1) {
			String reportPath = ReportUtils.getConfigPath(reportNumber);
			ReportUtils.setStatus(reportNumber, Status.WAITING);
			FilesManager.getReports.set(reportPath+".Appreciation", "None");
			FilesManager.getReports.set(reportPath+".Date", MessageUtils.getNowDate());
			FilesManager.getReports.set(reportPath+".Reason", reason);
			FilesManager.getReports.set(reportPath+".Reported.UUID", UserUtils.getUniqueId(reported));
			if(r != null) {
				FilesManager.getReports.set(reportPath+".Reported.IP", r.getAddress().toString());
				FilesManager.getReports.set(reportPath+".Reported.Gamemode", r.getGameMode().toString().toLowerCase());
				Location loc = r.getLocation();
				FilesManager.getReports.set(reportPath+".Reported.Location", loc.getWorld().getName()+":"+loc.getX()+"/"+loc.getY()+"/"+loc.getZ()+"/"+loc.getYaw()+"/"+loc.getPitch());
				FilesManager.getReports.set(reportPath+".Reported.OnGround", r.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR);
				FilesManager.getReports.set(reportPath+".Reported.Sneak", r.isSneaking());
				FilesManager.getReports.set(reportPath+".Reported.Sprint", r.isSprinting());
				FilesManager.getReports.set(reportPath+".Reported.Health", MessageUtils.cleanDouble(r.getHealth())+"/"+MessageUtils.cleanDouble(r.getMaxHealth()));
				FilesManager.getReports.set(reportPath+".Reported.Food", r.getFoodLevel());
				
				String configEffects = "";
				Collection<PotionEffect> effects = r.getActivePotionEffects();
				for(PotionEffect effect : effects) configEffects += effect.getType().getName()+":"+effect.getAmplifier()+"/"+effect.getDuration()+",";
				FilesManager.getReports.set(reportPath+".Reported.Effects", configEffects.length() > 0 ? configEffects.substring(0, configEffects.length()-1): "None");
			}
			FilesManager.getReports.set(reportPath+".Signalman.UUID", p.getUniqueId().toString());
			FilesManager.getReports.set(reportPath+".Signalman.IP", p.getAddress().toString());
			FilesManager.getReports.set(reportPath+".Signalman.Gamemode", p.getGameMode().toString().toLowerCase());
			Location loc = p.getLocation();
			FilesManager.getReports.set(reportPath+".Signalman.Location", loc.getWorld().getName()+":"+loc.getX()+"/"+loc.getY()+"/"+loc.getZ()+"/"+loc.getYaw()+"/"+loc.getPitch());
			FilesManager.saveReports();
		} else MessageUtils.sendStaffMessage(Message.STAFF_MAX_REPORTS_REACHED.get().replaceAll("_Number_", ""+ReportUtils.getMaxReports()), ConfigUtils.getStaffSound());
		
		TextComponent alert = new TextComponent(Message.ALERT.get().replaceAll("_Signalman_", ReportUtils.getPlayerName("Signalman", reportNumber, false)).replaceAll("_Reported_", ReportUtils.getPlayerName("Reported", reportNumber, !ReportUtils.onlinePlayerRequired())).replaceAll("_Reason_", reason));
		alert.setColor(ChatColor.valueOf(MessageUtils.getLastColor(Message.ALERT.get(), "_Reason_").name()));
		if(reportNumber != -1) {
			alert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports #"+reportNumber));
			alert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Message.ALERT_DETAILS.get().replaceAll("_Report_", ReportUtils.getName(reportNumber))).create()));
		}
		MessageUtils.sendStaffMessage(alert, ConfigUtils.getReportSound());
		
		s.sendMessage(Message.REPORT_SENT.get().replaceAll("_Player_", ReportUtils.getPlayerName("Reported", reportNumber, false)).replaceAll("_Reason_", reason));
		u.startCooldown(ReportUtils.getCooldown());
		return true;
	}

}
