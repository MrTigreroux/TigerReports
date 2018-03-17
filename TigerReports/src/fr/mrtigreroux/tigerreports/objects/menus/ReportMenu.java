package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

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

public class ReportMenu extends ReportManagerMenu {
	
	private QueryResult statisticsQuery = null;
	
	public ReportMenu(OnlineUser u, int reportId) {
		super(u, 54, 0, Permission.STAFF, reportId);
	}
	
	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.REPORT_TITLE.get().replace("_Report_", r.getName()), true);
		inv.setItem(0, MenuItem.REPORTS.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, r.getItem(null));
		inv.setItem(18, r.getItem(Message.REPORT_CHAT_ACTION.get()));
		
		inv.setItem(22, MenuItem.PUNISH_ABUSE.getWithDetails(Message.PUNISH_ABUSE_DETAILS.get().replace("_Player_", r.getPlayerName("Signalman", false, true)).replace("_Time_", MessageUtils.convertToSentence(ReportUtils.getPunishSeconds()))));
		
		if(statisticsQuery == null) statisticsQuery = TigerReports.getDb().query("SELECT true_appreciations,uncertain_appreciations,false_appreciations,reports,reported_times,processed_reports FROM tigerreports_users WHERE uuid IN (?,?)", Arrays.asList(r.getSignalmanUniqueId(), r.getReportedUniqueId()));
		for(String type : new String[]{"Signalman", "Reported"}) {
			String name = r.getPlayerName(type, false, false);
			String details = Message.PLAYER_DETAILS.get();
			Map<String, Object> statistics = statisticsQuery.getResult(type.equals("Signalman") ? 0 : 1);
			for(Statistic stat : Statistic.values()) {
				String statName = stat.getConfigName();
				String value;
				try {
					value = String.valueOf(statistics.get(statName));
				} catch (Exception notFound) {
					value = Message.NOT_FOUND_MALE.get();
				}
				details = details.replace("_"+statName.substring(0, 1).toUpperCase()+statName.substring(1).replace("_", "")+"_", value);
			}
			String server = (server = MessageUtils.getConfigServerLocation(r.getOldLocation(type))) != null ? server : Message.NOT_FOUND_MALE.get();
			inv.setItem(type.equals("Signalman") ? 21 : 23, new CustomItem().skullOwner(name).name(Message.valueOf(type.toUpperCase()).get().replace("_Player_", r.getPlayerName(type, true, true)))
					.lore(details.replace("_Server_", server).replace("_Teleportation_", Permission.STAFF_TELEPORT.isOwned(u) ? ((UserUtils.isOnline(name) ? Message.TELEPORT_TO_CURRENT_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_CURRENT_POSITION.get()).replace("_Player_", name)+(r.getOldLocation(type) != null ? Message.TELEPORT_TO_OLD_POSITION.get() : Message.CAN_NOT_TELEPORT_TO_OLD_POSITION.get()).replace("_Player_", name)) : "").split(ConfigUtils.getLineBreakSymbol())).create());
		}
		
		inv.setItem(26, MenuItem.DATA.getWithDetails(r.implementData(Message.DATA_DETAILS.get(), Permission.STAFF_ADVANCED.isOwned(u))));
		
		int statusPosition = 29;
		boolean archive = Permission.STAFF_ARCHIVE.isOwned(u) && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives());
		for(Status status : Status.values()) {
			inv.setItem(statusPosition, new CustomItem().type(Material.STAINED_CLAY).damage(status.getColor()).glow(status.equals(r.getStatus())).name(status == Status.DONE ? Message.PROCESS_STATUS.get() : Message.CHANGE_STATUS.get().replace("_Status_", status.getWord(null)))
					.lore((status == Status.DONE ? Message.PROCESS_STATUS_DETAILS.get() : Message.CHANGE_STATUS_DETAILS.get()).replace("_Status_", status.getWord(null)).split(ConfigUtils.getLineBreakSymbol())).create());
			statusPosition += status.equals(Status.IN_PROGRESS) && !archive ? 2 : 1;
		}
		if(archive) inv.setItem(33, MenuItem.ARCHIVE.get());
		
		if(Permission.STAFF_REMOVE.isOwned(u)) inv.setItem(36, MenuItem.REMOVE.get());
		inv.setItem(44, MenuItem.COMMENTS.getWithDetails(Message.COMMENTS_DETAILS.get()));
		
		return inv;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		switch(slot) {
			case 0: u.openReportsMenu(1, true); break;
			case 18: u.printInChat(r, r.implementDetails(Message.REPORT_CHAT_DETAILS.get(), false).replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol())); break;
			case 21: case 23:
				if(!Permission.STAFF_TELEPORT.isOwned(u)) return;
				String targetType = slot == 21 ? "Signalman" : "Reported";
				String name = r.getPlayerName(targetType, false, false);
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
					configLoc = r.getOldLocation(targetType);
					loc = MessageUtils.getConfigLocation(configLoc);
					if(loc == null) {
						MessageUtils.sendErrorMessage(p, Message.LOCATION_UNKNOWN.get().replace("_Player_", name));
						return;
					}
					serverName = MessageUtils.getConfigServerLocation(configLoc);
					locType = "OLD";
				} else return;
				p.sendMessage(Message.valueOf("TELEPORT_"+locType+"_LOCATION").get().replace("_Player_", Message.valueOf(targetType.toUpperCase()+"_NAME").get().replace("_Player_", name)).replace("_Report_", r.getName()));
				ConfigSound.TELEPORT.play(p);
				BungeeManager bungeeManager = TigerReports.getBungeeManager();
				if(serverName.equals("localhost") || bungeeManager.getServerName().equals(serverName)) p.teleport(loc);
				else {
					bungeeManager.sendPluginMessage("ConnectOther", p.getName(), serverName);
					bungeeManager.sendServerPluginNotification(serverName, p.getName()+" teleport "+configLoc);
				}
				break;
			case 22:
				long seconds = ReportUtils.getPunishSeconds();
				UserUtils.getUser(r.getSignalmanUniqueId()).punish(seconds, p.getName(), false);
				r.process(p.getUniqueId().toString(), null, "False", false, false);
				u.openReportsMenu(1, false);
				break;
			case 26:
				if(click == ClickType.LEFT) u.printInChat(r, r.implementData(Message.REPORT_CHAT_DATA.get(), Permission.STAFF_ADVANCED.isOwned(u)).replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
				else if(click == ClickType.RIGHT) {
					Map<Double, String> sortedMessages = new TreeMap<>();
					for(String type : new String[] {"Reported", "Signalman"}) {
						for(String message : r.getMessagesHistory(type)) {
							if(message != null && message.length() >= 20) {
								String date = message.substring(0, 19);
								sortedMessages.put(MessageUtils.getSeconds(date), Message.REPORT_MESSAGE_FORMAT.get().replace("_Date_", date).replace("_Player_", r.getPlayerName(type, false, true)).replace("_Message_", message.substring(20)));
							}
						}
					}
					StringBuilder messages = new StringBuilder();
					for(String message : sortedMessages.values()) messages.append(message);
					u.printInChat(r, Message.REPORT_MESSAGES_HISTORY.get().replace("_Report_", r.getName()).replace("_Messages_", !messages.toString().isEmpty() ? messages.toString() : Message.NONE_MALE.get()).split(ConfigUtils.getLineBreakSymbol()));
				}
				break;
			case 36: u.openConfirmationMenu(r, "REMOVE"); break;
			case 44: u.openCommentsMenu(1, r); break;
			default:
				if((slot == 32 || slot == 33) && !(Permission.STAFF_ARCHIVE.isOwned(u) && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives()))) slot--;
				switch(slot) {
					case 29: case 30: case 31:
						r.setStatus(Arrays.asList(Status.values()).get(slot-29), false);
						if(slot == 31 && !Permission.STAFF_ADVANCED.isOwned(u)) u.openReportsMenu(1, true);
						else open(true);
						break;
					case 32: u.openAppreciationMenu(r); break;
					case 33: u.openConfirmationMenu(r, "ARCHIVE"); break;
					default: break;
				}
				break;
		}
	}
	
}
