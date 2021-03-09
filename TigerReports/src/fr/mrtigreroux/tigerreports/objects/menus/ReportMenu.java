package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.*;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.utils.*;

/**
 * @author MrTigreroux
 */

public class ReportMenu extends ReportManagerMenu {

	private QueryResult statisticsQuery = null;
	private boolean statisticsCollected = false;

	public ReportMenu(OnlineUser u, int reportId) {
		super(u, 54, 0, Permission.STAFF, reportId);
	}

	@Override
	protected boolean collectReport() {
		if (reportCollected && statisticsCollected) {
			return true;
		} else {
			Report r = this.r;
			TigerReports tr = TigerReports.getInstance();
			Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

				@Override
				public void run() {
					Report upr = r != null ? r : tr.getReportsManager().getReportById(reportId, true);
					QueryResult upStatisticsQuery = statisticsQuery != null ? statisticsQuery
							: upr != null ? tr.getDb().query(
									"SELECT uuid,true_appreciations,uncertain_appreciations,false_appreciations,reports,reported_times,processed_reports FROM tigerreports_users WHERE uuid = ? OR uuid = ? LIMIT 2",
									Arrays.asList(upr.getReporterUniqueId(), upr.getReportedUniqueId())) : null;
					Bukkit.getScheduler().runTask(tr, new Runnable() {

						@Override
						public void run() {
							setReport(upr);
							setStatistics(upStatisticsQuery);
							open(true);
						}

					});
				}

			});
			return false;
		}
	}

	private void setStatistics(QueryResult statistics) {
		this.statisticsCollected = true;
		this.statisticsQuery = statistics;
	}

	@Override
	public Inventory onOpen() {
		Inventory inv = getInventory(Message.REPORT_TITLE.get().replace("_Report_", r.getName()), true);
		inv.setItem(0, MenuItem.REPORTS.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, r.getItem(null));
		inv.setItem(18, r.getItem(Message.REPORT_CHAT_ACTION.get()));

		boolean stackedReport = r.isStackedReport();
		if (!stackedReport) {
			inv.setItem(22,
					MenuItem.PUNISH_ABUSE.clone()
							.details(Message.PUNISH_ABUSE_DETAILS.get()
									.replace("_Player_", r.getPlayerName("Reporter", false, true))
									.replace("_Time_", MessageUtils.convertToSentence(ReportUtils.getPunishSeconds())))
							.create());
		}

		Map<String, Object> reporter_stats = statisticsQuery != null ? statisticsQuery.getResult(0) : null;
		Map<String, Object> reported_stats = null;
		if (reporter_stats != null && !reporter_stats.get("uuid").equals(r.getReporterUniqueId())) {
			reported_stats = reporter_stats;
			if (statisticsQuery != null)
				reporter_stats = statisticsQuery.getResult(1);
		} else {
			if (statisticsQuery != null)
				reported_stats = statisticsQuery.getResult(1);
		}

		for (String type : new String[] { "Reporter", "Reported" }) {
			String name = r.getPlayerName(type, false, false);
			String details = stackedReport && type.equals("Reporter") ? Message
					.get("Menus.Stacked-report-reporters-details").replace("_First_", r.getPlayerName(type, true, true))
					.replace("_Others_", r.getReportersNames(1)) : Message.PLAYER_DETAILS.get();
			Map<String, Object> statistics = type.equals("Reporter") ? reporter_stats : reported_stats;

			for (Statistic stat : Statistic.values()) {
				String statName = stat.getConfigName();
				String value = null;
				try {
					value = statistics != null ? String.valueOf(statistics.get(statName)) : null;
				} catch (Exception notFound) {
				}
				if (value == null)
					value = Message.NOT_FOUND_MALE.get();
				details = details.replace(
						"_" + statName.substring(0, 1).toUpperCase() + statName.substring(1).replace("_", "") + "_",
						value);
			}
			String serverName = (serverName = MessageUtils.getServer(r.getOldLocation(type))) != null
					? MessageUtils.getServerName(serverName)
					: Message.NOT_FOUND_MALE.get();

			String tp = "";
			if (Permission.STAFF_TELEPORT.isOwned(u)) {
				tp = (UserUtils.isOnline(name) ? Message.TELEPORT_TO_CURRENT_POSITION
						: Message.CAN_NOT_TELEPORT_TO_CURRENT_POSITION).get()
						+ (r.getOldLocation(type) != null ? Message.TELEPORT_TO_OLD_POSITION
								: Message.CAN_NOT_TELEPORT_TO_OLD_POSITION).get();
			}
			inv.setItem(type.equals("Reporter") ? 21 : 23, new CustomItem().skullOwner(name)
					.name((stackedReport && type.equals("Reporter") ? Message.get("Menus.Stacked-report-reporters")
							: Message.valueOf(type.toUpperCase()).get()).replace("_Player_",
									r.getPlayerName(type, true, true)))
					.lore(details.replace("_Server_", serverName)
							.replace("_Teleportation_", tp.replace("_Player_", name))
							.split(ConfigUtils.getLineBreakSymbol()))
					.create());
		}

		inv.setItem(26, MenuItem.DATA
				.getWithDetails(r.implementData(Message.DATA_DETAILS.get(), Permission.STAFF_ADVANCED.isOwned(u))));

		int statusPosition = 29;
		boolean archive = u.canArchive(r);
		for (Status status : Status.values()) {
			inv.setItem(statusPosition,
					status.getButtonItem().glow(status.equals(r.getStatus()))
							.name(status == Status.DONE ? Message.PROCESS_STATUS.get()
									: Message.CHANGE_STATUS.get().replace("_Status_", status.getWord(null)))
							.lore((status == Status.DONE ? Message.PROCESS_STATUS_DETAILS.get()
									: Message.CHANGE_STATUS_DETAILS.get()).replace("_Status_", status.getWord(null))
											.split(ConfigUtils.getLineBreakSymbol()))
							.create());
			statusPosition += status.equals(Status.IN_PROGRESS) && !archive ? 2 : 1;
		}
		if (archive)
			inv.setItem(33, MenuItem.ARCHIVE.create());

		if (Permission.STAFF_DELETE.isOwned(u))
			inv.setItem(36, MenuItem.DELETE.get());
		inv.setItem(44, MenuItem.COMMENTS.getWithDetails(Message.COMMENTS_DETAILS.get()));

		return inv;
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		switch (slot) {
		case 0:
			u.openReportsMenu(1, true);
			break;
		case 18:
			u.printInChat(r, r.implementDetails(Message.REPORT_CHAT_DETAILS.get(), false)
					.replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
			break;
		case 21:
		case 23:
			if (!Permission.STAFF_TELEPORT.isOwned(u))
				return;
			String targetType = slot == 21 ? "Reporter" : "Reported";
			String target = r.getPlayerName(targetType, false, false);
			Player t = UserUtils.getPlayer(target);
			String locType;
			String serverName = null;
			Location loc = null;
			String configLoc = null;
			boolean tpDifferentServer = false;

			BungeeManager bm = TigerReports.getInstance().getBungeeManager();
			if (click.toString().contains("LEFT")) {
				if (t == null) {
					if (bm.isOnline(target)) {
						tpDifferentServer = true;
					} else {
						MessageUtils.sendErrorMessage(p, Message.PLAYER_OFFLINE.get().replace("_Player_", target));
						return;
					}
				} else {
					serverName = "localhost";
					loc = t.getLocation();
				}
				locType = "CURRENT";
			} else if (click.toString().contains("RIGHT")) {
				configLoc = r.getOldLocation(targetType);
				loc = MessageUtils.getLocation(configLoc);
				if (loc == null) {
					MessageUtils.sendErrorMessage(p, Message.LOCATION_UNKNOWN.get().replace("_Player_", target));
					return;
				}
				serverName = MessageUtils.getServer(configLoc);
				locType = "OLD";
			} else {
				return;
			}
			u.sendMessageWithReportButton(Message.valueOf("TELEPORT_" + locType + "_LOCATION").get()
					.replace("_Player_",
							Message.valueOf(targetType.toUpperCase() + "_NAME").get().replace("_Player_", target))
					.replace("_Report_", r.getName()), r);
			if (tpDifferentServer) {
				bm.sendPluginNotification(p.getName() + " tp_player " + target);
			} else if (serverName.equals("localhost") || bm.getServerName().equals(serverName)) {
				p.teleport(loc);
				ConfigSound.TELEPORT.play(p);
			} else {
				bm.sendPluginMessage("ConnectOther", p.getName(), serverName);
				bm.sendServerPluginNotification(serverName,
						System.currentTimeMillis() + " " + p.getName() + " tp_loc " + configLoc);
			}
			break;
		case 22:
			if (!r.isStackedReport()) {
				long seconds = ReportUtils.getPunishSeconds();
				TigerReports.getInstance().getUsersManager().getUser(r.getReporterUniqueId()).punish(seconds,
						p.getName(), false);
				r.process(p.getUniqueId().toString(), p.getName(), "False", false,
						Permission.STAFF_ARCHIVE_AUTO.isOwned(u), false);
				u.openReportsMenu(1, false);
			}
			break;
		case 26:
			if (click == ClickType.LEFT) {
				u.printInChat(r, r.implementData(Message.REPORT_CHAT_DATA.get(), Permission.STAFF_ADVANCED.isOwned(u))
						.replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()));
			} else if (click == ClickType.RIGHT) {
				Map<Double, String> sortedMessages = new TreeMap<Double, String>();
				for (String type : new String[] { "Reported", "Reporter" }) {
					for (String message : r.getMessagesHistory(type)) {
						if (message != null && message.length() >= 20) {
							String date = message.substring(0, 19);
							sortedMessages.put(MessageUtils.getSeconds(date),
									Message.REPORT_MESSAGE_FORMAT.get().replace("_Date_", date)
											.replace("_Player_", r.getPlayerName(type, false, true))
											.replace("_Message_", message.substring(20)));
						}
					}
				}
				StringBuilder messages = new StringBuilder();
				for (String message : sortedMessages.values())
					messages.append(message);
				u.printInChat(r,
						Message.REPORT_MESSAGES_HISTORY.get().replace("_Report_", r.getName())
								.replace("_Messages_",
										!messages.toString().isEmpty() ? messages.toString() : Message.NONE_MALE.get())
								.split(ConfigUtils.getLineBreakSymbol()));
			}
			break;
		case 36:
			u.openConfirmationMenu(r, "DELETE");
			break;
		case 44:
			u.openCommentsMenu(1, r);
			break;
		default:
			if ((slot == 32 || slot == 33) && !(Permission.STAFF_ARCHIVE.isOwned(u)
					&& (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives())))
				slot--;
			switch (slot) {
			case 29:
			case 30:
			case 31:
				r.setStatus(Arrays.asList(Status.values()).get(slot - 29), p.getUniqueId().toString(), false);
				if (slot == 31 && !Permission.STAFF_ADVANCED.isOwned(u)) {
					u.openReportsMenu(1, true);
				} else {
					open(true);
				}
				break;
			case 32:
				u.openProcessMenu(r);
				break;
			case 33:
				if (u.canArchive(r)) {
					u.openConfirmationMenu(r, "ARCHIVE");
				} else {
					open(false);
				}
				break;
			default:
				break;
			}
			break;
		}
	}

}
