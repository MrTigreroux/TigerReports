package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportMenu extends Menu {
	
	private QueryResult statisticsQuery = null;
	
	public ReportMenu(OnlineUser u, int reportId) {
		super(u, 54, 0, reportId, null, null);
	}
	
	@Override
	public void open(boolean sound) {
		if(!checkReport()) return;
		
		Status reportStatus = r.getStatus();
		if((reportStatus == Status.IMPORTANT || reportStatus == Status.DONE) && !u.hasPermission(Permission.ADVANCED)) {
			MessageUtils.sendErrorMessage(p, Message.PERMISSION_ACCESS_DETAILS.get().replace("_Report_", r.getName()));
			p.closeInventory();
			return;
		}
		
		Inventory inv = getInventory(Message.REPORT_TITLE.get().replace("_Report_", r.getName()), true);
		inv.setItem(0, MenuItem.REPORTS_ICON.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, r.getItem(null));
		inv.setItem(18, r.getItem(Message.REPORT_CHAT_ACTION.get()));
		
		inv.setItem(MenuItem.PUNISH_ABUSE.getPosition(), MenuItem.PUNISH_ABUSE.getWithDetails(Message.PUNISH_ABUSE_DETAILS.get().replace("_Player_", r.getPlayerName("Signalman", false)).replace("_Time_", MessageUtils.convertToSentence(ReportUtils.getPunishSeconds()))));
		
		if(statisticsQuery == null) statisticsQuery = TigerReports.getDb().query("SELECT true_appreciations,uncertain_appreciations,false_appreciations,reports,reported_times,processed_reports FROM users WHERE uuid IN (?,?)", Arrays.asList(r.getSignalmanUniqueId(), r.getReportedUniqueId()));
		for(String type : new String[]{"Signalman", "Reported"}) {
			String name = r.getPlayerName(type, false);
			String details = Message.PLAYER_DETAILS.get();
			Map<String, Object> statistics = statisticsQuery.getResult(type.equals("Signalman") ? 0 : 1);
			for(Statistic stat : Statistic.values()) {
				String statName = stat.getConfigName();
				String value;
				try {
					value = ""+statistics.get(statName);
				} catch (Exception notFound) {
					value = Message.NOT_FOUND_MALE.get();
				}
				details = details.replace("_"+statName.substring(0, 1).toUpperCase()+statName.substring(1).replace("_", "")+"_", value);
			}
			inv.setItem(type.equals("Signalman") ? 21 : 23, new CustomItem().skullOwner(name).name(Message.valueOf(type.toUpperCase()).get().replace("_Player_", r.getPlayerName(type, true)))
					.lore(details.replace("_Teleportation_", u.hasPermission(Permission.TELEPORT) ? ((UserUtils.isOnline(name) ? Message.TELEPORT_TO_CURRENT_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_CURRENT_POSITION.get()).replace("_Player_", name)+(r.getOldLocation(type) != null ? Message.TELEPORT_TO_OLD_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_OLD_POSITION.get()).replace("_Player_", name)) : "").split(ConfigUtils.getLineBreakSymbol())).create());
		}
		
		inv.setItem(MenuItem.DATA.getPosition(), MenuItem.DATA.getWithDetails(r.implementData(Message.DATA_DETAILS.get(), u.hasPermission(Permission.ADVANCED))));
		
		int statusPosition = 29;
		boolean archive = u.hasPermission(Permission.ARCHIVE);
		for(Status status : Status.values()) {
			inv.setItem(statusPosition, new CustomItem().type(Material.STAINED_CLAY).damage(status.getColor()).glow(status.equals(r.getStatus())).name(status == Status.DONE ? Message.PROCESS_STATUS.get() : Message.CHANGE_STATUS.get().replace("_Status_", status.getWord(null)))
					.lore((status == Status.DONE ? Message.PROCESS_STATUS_DETAILS.get() : Message.CHANGE_STATUS_DETAILS.get()).replace("_Status_", status.getWord(null)).split(ConfigUtils.getLineBreakSymbol())).create());
			statusPosition += status.equals(Status.IN_PROGRESS) && !archive ? 2 : 1;
		}
		if(archive) inv.setItem(MenuItem.ARCHIVE.getPosition(), MenuItem.ARCHIVE.get());
		
		if(u.hasPermission(Permission.REMOVE)) inv.setItem(MenuItem.REMOVE.getPosition(), MenuItem.REMOVE.get());
		inv.setItem(MenuItem.COMMENTS.getPosition(), MenuItem.COMMENTS.getWithDetails(Message.COMMENTS_DETAILS.get()));
		
		p.openInventory(inv);
		if(sound) u.playSound(ConfigSound.MENU.get());
		u.setOpenedMenu(this);
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		if(!checkReport()) return;
		if(slot == 0) u.openReportsMenu(1, true);
		else if(slot == 18) u.printInChat(r, r.implementDetails(Message.REPORT_CHAT_DETAILS.get()).replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
		else if(slot == MenuItem.PUNISH_ABUSE.getPosition()) {
			long seconds = ReportUtils.getPunishSeconds();
			UserUtils.getUser(r.getSignalmanUniqueId()).punish(seconds, p.getName(), false);
			r.process(p.getUniqueId().toString(), null, "False", false);
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
			String serverName;
			Location loc;
			String configLoc = null;
			if(click.toString().contains("LEFT")) {
				if(t == null) {
					MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replace("_Player_", name));
					return;
				}
				serverName = "localhost";
				loc = t.getLocation();
				locType = "CURRENT";
			} else if(click.toString().contains("RIGHT")) {
				configLoc = r.getOldLocation(type);
				serverName = MessageUtils.getConfigServerLocation(configLoc);
				loc = MessageUtils.getConfigLocation(configLoc);
				if(loc == null) {
					MessageUtils.sendErrorMessage(p, Message.LOCATION_UNKNOWN.get().replace("_Player_", name));
					return;
				}
				locType = "OLD";
			} else return;
			p.sendMessage(Message.valueOf("TELEPORT_"+locType+"_LOCATION").get().replace("_Player_", Message.valueOf(type.toUpperCase()+"_NAME").get().replace("_Player_", name)).replace("_Report_", r.getName()));
			u.playSound(ConfigSound.TELEPORT.get());
			BungeeManager bungeeManager = TigerReports.getBungeeManager();
			if(serverName.equals("localhost") || bungeeManager.getServerName().equals(serverName)) p.teleport(loc);
			else {
				bungeeManager.sendPluginMessage("ConnectOther", p.getName(), serverName);
				bungeeManager.sendServerPluginNotification(serverName, p.getName()+" teleport "+configLoc);
			}
		} else if(u.hasPermission(Permission.ARCHIVE)) {
			if(slot >= 29 && slot <= 31) {
				r.setStatus(Arrays.asList(Status.values()).get(slot-29), false);
				if(!u.hasPermission(Permission.ADVANCED) && slot == 31) u.openReportsMenu(1, true);
				else open(true);
			} else if(slot == 32) u.openAppreciationMenu(r);
			else if(slot == 33) u.openConfirmationMenu(r, "ARCHIVE");
		} else if(slot == 29 || slot == 30 || slot == 32) {
			if(slot == 32) slot = 31;
			r.setStatus(Arrays.asList(Status.values()).get(slot-29), false);
			if(!u.hasPermission(Permission.ADVANCED) && slot == 31) u.openReportsMenu(1, true);
			else open(true);
		} else if(slot == 33) u.openAppreciationMenu(r);
	}
	
}
