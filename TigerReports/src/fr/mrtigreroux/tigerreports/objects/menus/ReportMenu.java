package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.MenuItem;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportMenu extends Menu {
	
	public ReportMenu(User u, int reportNumber) {
		super(u, 54, 0, reportNumber, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		Status reportStatus = ReportUtils.getStatus(reportNumber);
		if(!ReportUtils.exist(reportNumber) || ((reportStatus == Status.IMPORTANT || reportStatus == Status.DONE) && !u.hasPermission(Permission.ADVANCED))) {
			MessageUtils.sendErrorMessage(p, Message.PERMISSION_ACCESS_DETAILS.get().replaceAll("_Report_", ReportUtils.getName(reportNumber)));
			p.closeInventory();
			return;
		}
		Inventory inv = getInventory(Message.REPORT_TITLE.get().replaceAll("_Report_", ReportUtils.getName(reportNumber)), true);
		inv.setItem(0, MenuItem.REPORTS_ICON.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, ReportUtils.getItem(reportNumber, null));
		inv.setItem(18, ReportUtils.getItem(reportNumber, Message.REPORT_CHAT_ACTION.get()));
		
		inv.setItem(MenuItem.PUNISH_ABUSE.getPosition(), MenuItem.PUNISH_ABUSE.getWithDetails(Message.PUNISH_ABUSE_DETAILS.get().replaceAll("_Player_", ReportUtils.getPlayerName("Signalman", reportNumber, false).replaceAll("_Time_", MessageUtils.convertToSentence(ReportUtils.getPunishSeconds())))));
		for(String type : new String[]{"Signalman", "Reported"}) {
			String name = ReportUtils.getPlayerName(type, reportNumber, false);
			inv.setItem(type.equals("Signalman") ? 21 : 23, new CustomItem().skullOwner(name).name(Message.valueOf(type.toUpperCase()).get().replaceAll("_Player_", ReportUtils.getPlayerName(type, reportNumber, true)))
					.lore(u.hasPermission(Permission.TELEPORT) ? ((UserUtils.isOnline(name) ? Message.TELEPORT_TO_CURRENT_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_CURRENT_POSITION.get()).replaceAll("_Player_", name)+(ReportUtils.getOldLocation(type, reportNumber) != null ? Message.TELEPORT_TO_OLD_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_OLD_POSITION.get()).replaceAll("_Player_", name)).split(ConfigUtils.getLineBreakSymbol()) : null).create());
		}
		
		inv.setItem(MenuItem.DATA.getPosition(), MenuItem.DATA.getWithDetails(ReportUtils.getData(reportNumber, u.hasPermission(Permission.ADVANCED))));
		
		int statusPosition = 29;
		List<Status> statusList = Arrays.asList(Status.values());
		boolean archive = u.hasPermission(Permission.ARCHIVE);
		for(Status status : statusList) {
			inv.setItem(statusPosition, new CustomItem().type(Material.STAINED_CLAY).damage(status.getColor()).glow(status.equals(ReportUtils.getStatus(reportNumber))).name(Message.CHANGE_STATUS.get().replaceAll("_Status_", status.getWord()))
					.lore(Message.CHANGE_STATUS_DETAILS.get().replaceAll("_Status_", status.getWord()).split(ConfigUtils.getLineBreakSymbol())).create());
			statusPosition += status.equals(Status.IN_PROGRESS) && !archive ? 2 : 1;
		}
		if(archive) inv.setItem(MenuItem.ARCHIVE.getPosition(), MenuItem.ARCHIVE.get());
		
		if(u.hasPermission(Permission.REMOVE)) inv.setItem(MenuItem.REMOVE.getPosition(), MenuItem.REMOVE.get());
		inv.setItem(MenuItem.COMMENTS.getPosition(), MenuItem.COMMENTS.getWithDetails(Message.COMMENTS_DETAILS.get()));
		
		p.openInventory(inv);
		if(sound) p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		Player p = u.getPlayer();
		if(slot == 0) u.openReportsMenu(1, true);
		else if(slot == 2 || slot == 6) {
			reportNumber += slot == 2  ? -1 : 1;
			int totalSounds = ReportUtils.getTotalReports();
			if(reportNumber >= totalSounds) reportNumber = 1;
			if(reportNumber < 1) reportNumber = totalSounds-1;
			u.openReportMenu(reportNumber);
		} else if(slot == 18) {
			for(String line : ReportUtils.implementDetails(reportNumber, Message.REPORT_CHAT_DETAILS.get()).replaceAll("_Report_", ReportUtils.getName(reportNumber)).split(ConfigUtils.getLineBreakSymbol()))
				p.sendMessage(line);
			p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
			p.closeInventory();
			return;
		} else if(slot == MenuItem.PUNISH_ABUSE.getPosition()) {
			long seconds = ReportUtils.getPunishSeconds();
			String signalman = ReportUtils.getPlayerName("Signalman", reportNumber, false);
			if(!UserUtils.isOnline(signalman)) {
				MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replaceAll("_Player_", signalman));
				return;
			}
			Player s = UserUtils.getPlayer(signalman);
			new User(s).startCooldown(seconds);
			String time = MessageUtils.convertToSentence(seconds);
			s.sendMessage(Message.PUNISHED.get().replaceAll("_Time_", time));
			MessageUtils.sendStaffMessage(Message.STAFF_PUNISH.get().replaceAll("_Player_", p.getName()).replaceAll("_Signalman_", s.getName()).replaceAll("_Time_", time), ConfigUtils.getStaffSound());
			ReportUtils.setAppreciation(reportNumber, "False");
			ReportUtils.setStatus(reportNumber, Status.DONE);
			u.openReportsMenu(1, false);
		} else if(slot == MenuItem.DATA.getPosition()) {
			String messagesHistory = Message.REPORT_MESSAGES_HISTORY.get();
			for(String type : Arrays.asList("Reported", "Signalman")) messagesHistory = messagesHistory.replaceAll("_"+type+"_", ReportUtils.getPlayerName(type, reportNumber, false)).replaceAll("_"+type+"Messages_", ReportUtils.getMessagesHistory(type, reportNumber));
			p.sendMessage(messagesHistory.replaceAll("_Report_", ReportUtils.getName(reportNumber)).split(ConfigUtils.getLineBreakSymbol()));
			p.playSound(p.getLocation(), ConfigUtils.getMenuSound(), 1, 1);
			p.closeInventory();
		} else if(slot == MenuItem.REMOVE.getPosition()) u.openConfirmationMenu(reportNumber, "REMOVE");
		else if(slot == MenuItem.COMMENTS.getPosition()) u.openCommentsMenu(1, reportNumber);
		else if(slot == 21 || slot == 23) {
			if(!u.hasPermission(Permission.TELEPORT)) return;
			String type = slot == 21 ? "Signalman" : "Reported";
			String name = ReportUtils.getPlayerName(type, reportNumber, false);
			Player t = UserUtils.getPlayer(name);
			String locType;
			Location loc;
			if(click.toString().contains("LEFT")) {
				if(t == null) {
					MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replaceAll("_Player_", name));
					return;
				}
				loc = t.getLocation();
				locType = "CURRENT";
			} else if(click.toString().contains("RIGHT")){
				loc = ReportUtils.getOldLocation(type, reportNumber);
				if(loc == null) {
					MessageUtils.sendErrorMessage(p, Message.LOCATION_UNKNOWN.get().replaceAll("_Player_", name));
					return;
				}
				locType = "OLD";
			} else return;
			p.teleport(loc);
			p.playSound(p.getLocation(), ConfigUtils.getTeleportSound(), 1, 1);
			p.sendMessage(Message.valueOf("TELEPORT_"+locType+"_LOCATION").get().replaceAll("_Player_", Message.valueOf(type.toUpperCase()+"_NAME").get().replaceAll("_Player_", name)));
		} else if(u.hasPermission(Permission.ARCHIVE)) {
			if(slot >= 29 && slot <= 31) {
				ReportUtils.setStatus(reportNumber, Arrays.asList(Status.values()).get(slot-29));
				if(!u.hasPermission(Permission.ADVANCED) && slot == 31) u.openReportsMenu(1, true);
				else open(true);
			} else if(slot == 32) u.openAppreciationMenu(reportNumber);
			else if(slot == 33) u.openConfirmationMenu(reportNumber, "ARCHIVE");
		} else if(slot == 29 || slot == 30 || slot == 32) {
			if(slot == 32) slot = 31;
			ReportUtils.setStatus(reportNumber, Arrays.asList(Status.values()).get(slot-29));
			if(!u.hasPermission(Permission.ADVANCED) && slot == 31) u.openReportsMenu(1, true);
			else open(true);
		} else if(slot == 33) u.openAppreciationMenu(reportNumber);
	}
	
}
