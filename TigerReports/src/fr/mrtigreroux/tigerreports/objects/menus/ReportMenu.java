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
import fr.mrtigreroux.tigerreports.data.Statistic;
import fr.mrtigreroux.tigerreports.data.Status;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportMenu extends Menu {
	
	public ReportMenu(User u, Report r) {
		super(u, 54, 0, r, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		Status reportStatus = r.getStatus();
		if(!r.exist() || ((reportStatus == Status.IMPORTANT || reportStatus == Status.DONE) && !u.hasPermission(Permission.ADVANCED))) {
			MessageUtils.sendErrorMessage(p, Message.PERMISSION_ACCESS_DETAILS.get().replace("_Report_", r.getName()));
			p.closeInventory();
			return;
		}
		
		Inventory inv = getInventory(Message.REPORT_TITLE.get().replace("_Report_", r.getName()), true);
		inv.setItem(0, MenuItem.REPORTS_ICON.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, r.getItem(null));
		inv.setItem(18, r.getItem(Message.REPORT_CHAT_ACTION.get()));
		
		inv.setItem(MenuItem.PUNISH_ABUSE.getPosition(), MenuItem.PUNISH_ABUSE.getWithDetails(Message.PUNISH_ABUSE_DETAILS.get().replace("_Player_", r.getPlayerName("Signalman", false).replace("_Time_", MessageUtils.convertToSentence(ReportUtils.getPunishSeconds())))));
		for(String type : new String[]{"Signalman", "Reported"}) {
			String name = r.getPlayerName(type, false);
			String uuid = UserUtils.getUniqueId(name);
			String details = Message.PLAYER_DETAILS.get();
			for(Statistic stat : Statistic.values()) {
				String statName = stat.getConfigName();
				details = details.replace("_"+statName.replace("Appreciations.", "")+"_", ""+UserUtils.getStat(uuid, statName));
			}
			inv.setItem(type.equals("Signalman") ? 21 : 23, new CustomItem().skullOwner(name).name(Message.valueOf(type.toUpperCase()).get().replace("_Player_", r.getPlayerName(type, true)))
					.lore(details.replace("_Teleportation_", u.hasPermission(Permission.TELEPORT) ? ((UserUtils.isOnline(name) ? Message.TELEPORT_TO_CURRENT_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_CURRENT_POSITION.get()).replace("_Player_", name)+(r.getOldLocation(type) != null ? Message.TELEPORT_TO_OLD_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_OLD_POSITION.get()).replace("_Player_", name)) : "").split(ConfigUtils.getLineBreakSymbol())).create());
		}
		
		inv.setItem(MenuItem.DATA.getPosition(), MenuItem.DATA.getWithDetails(r.implementData(Message.DATA_DETAILS.get(), u.hasPermission(Permission.ADVANCED))));
		
		int statusPosition = 29;
		List<Status> statusList = Arrays.asList(Status.values());
		boolean archive = u.hasPermission(Permission.ARCHIVE);
		for(Status status : statusList) {
			inv.setItem(statusPosition, new CustomItem().type(Material.STAINED_CLAY).damage(status.getColor()).glow(status.equals(r.getStatus())).name(status == Status.DONE ? Message.PROCESS_STATUS.get() : Message.CHANGE_STATUS.get().replace("_Status_", status.getWord(null)))
					.lore((status == Status.DONE ? Message.PROCESS_STATUS_DETAILS.get() : Message.CHANGE_STATUS_DETAILS.get()).replace("_Status_", status.getWord(null)).split(ConfigUtils.getLineBreakSymbol())).create());
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
			int reportNumber = r.getNumber()+(slot == 2  ? -1 : 1);
			int totalReports = ReportUtils.getTotalReports();
			if(reportNumber >= totalReports) reportNumber = 1;
			if(reportNumber < 1) reportNumber = totalReports-1;
			u.openReportMenu(new Report(reportNumber));
		} else if(slot == 18) {
			u.printInChat(r, r.implementDetails(Message.REPORT_CHAT_DETAILS.get()).replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
		} else if(slot == MenuItem.PUNISH_ABUSE.getPosition()) {
			long seconds = ReportUtils.getPunishSeconds();
			String signalman = r.getPlayerName("Signalman", false);
			if(!UserUtils.isOnline(signalman)) {
				MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replace("_Player_", signalman));
				return;
			}
			Player s = UserUtils.getPlayer(signalman);
			UserUtils.getUser(s).startCooldown(seconds);
			String time = MessageUtils.convertToSentence(seconds);
			s.sendMessage(Message.PUNISHED.get().replace("_Time_", time));
			MessageUtils.sendStaffMessage(Message.STAFF_PUNISH.get().replace("_Player_", p.getName()).replace("_Signalman_", s.getName()).replace("_Time_", time), ConfigUtils.getStaffSound());
			r.setAppreciation("False");
			r.setDone(u.getPlayer().getUniqueId());
			u.openReportsMenu(1, false);
		} else if(slot == MenuItem.DATA.getPosition()) {
			if(click == ClickType.LEFT) {
				u.printInChat(r, r.implementData(Message.REPORT_CHAT_DATA.get(), u.hasPermission(Permission.ADVANCED)).replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
			} else if(click == ClickType.RIGHT) {
				String messagesHistory = Message.REPORT_MESSAGES_HISTORY.get();
				for(String type : Arrays.asList("Reported", "Signalman")) messagesHistory = messagesHistory.replace("_"+type+"_", r.getPlayerName(type, false)).replace("_"+type+"Messages_", r.getMessagesHistory(type));
				u.printInChat(r, messagesHistory.replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
			}
		} else if(slot == MenuItem.REMOVE.getPosition()) u.openConfirmationMenu(r, "REMOVE");
		else if(slot == MenuItem.COMMENTS.getPosition()) u.openCommentsMenu(1, r);
		else if(slot == 21 || slot == 23) {
			if(!u.hasPermission(Permission.TELEPORT)) return;
			String type = slot == 21 ? "Signalman" : "Reported";
			String name = r.getPlayerName(type, false);
			Player t = UserUtils.getPlayer(name);
			String locType;
			Location loc;
			if(click.toString().contains("LEFT")) {
				if(t == null) {
					MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replace("_Player_", name));
					return;
				}
				loc = t.getLocation();
				locType = "CURRENT";
			} else if(click.toString().contains("RIGHT")){
				loc = r.getOldLocation(type);
				if(loc == null) {
					MessageUtils.sendErrorMessage(p, Message.LOCATION_UNKNOWN.get().replace("_Player_", name));
					return;
				}
				locType = "OLD";
			} else return;
			p.teleport(loc);
			p.playSound(p.getLocation(), ConfigUtils.getTeleportSound(), 1, 1);
			p.sendMessage(Message.valueOf("TELEPORT_"+locType+"_LOCATION").get().replace("_Player_", Message.valueOf(type.toUpperCase()+"_NAME").get().replace("_Player_", name)).replace("_Report_", r.getName()));
		} else if(u.hasPermission(Permission.ARCHIVE)) {
			if(slot >= 29 && slot <= 31) {
				r.setStatus(Arrays.asList(Status.values()).get(slot-29));
				if(!u.hasPermission(Permission.ADVANCED) && slot == 31) u.openReportsMenu(1, true);
				else open(true);
			} else if(slot == 32) u.openAppreciationMenu(r);
			else if(slot == 33) u.openConfirmationMenu(r, "ARCHIVE");
		} else if(slot == 29 || slot == 30 || slot == 32) {
			if(slot == 32) slot = 31;
			r.setStatus(Arrays.asList(Status.values()).get(slot-29));
			if(!u.hasPermission(Permission.ADVANCED) && slot == 31) u.openReportsMenu(1, true);
			else open(true);
		} else if(slot == 33) u.openAppreciationMenu(r);
	}
	
}
