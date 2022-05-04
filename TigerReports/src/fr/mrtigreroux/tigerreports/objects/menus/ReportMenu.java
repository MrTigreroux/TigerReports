package fr.mrtigreroux.tigerreports.objects.menus;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ReportMenu extends ReportManagerMenu implements User.UserListener {

	private static final Logger LOGGER = Logger.fromClass(ReportMenu.class);

	private final VaultManager vm;
	private final BungeeManager bm;

	public ReportMenu(User u, int reportId, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        VaultManager vm, BungeeManager bm, UsersManager um) {
		super(u, 54, 0, Permission.STAFF, reportId, true, rm, db, taskScheduler, um);
		this.vm = vm;
		this.bm = bm;
	}

	@Override
	public Inventory onOpen() {
		r.getReported().addListener(this, db, taskScheduler, um);
		r.getReporter().addListener(this, db, taskScheduler, um);

		Inventory inv = getInventory(Message.REPORT_TITLE.get().replace("_Report_", r.getName()), true);
		inv.setItem(0, MenuItem.REPORTS.getWithDetails(Message.REPORTS_DETAILS.get()));
		inv.setItem(4, r.getItem(null, vm, bm));
		inv.setItem(18, r.getItem(Message.REPORT_CHAT_ACTION.get(), vm, bm));

		boolean stackedReport = r.isStackedReport();

		inv.setItem(22, MenuItem.PUNISH_ABUSE.clone()
		        .details(Message.PUNISH_ABUSE_DETAILS.get()
		                .replace("_Players_",
		                        stackedReport ? r.getReportersNames(0, false, vm, bm)
		                                : r.getPlayerName(Report.ParticipantType.REPORTER, false, true, vm, bm))
		                .replace("_Time_", DatetimeUtils.convertToSentence(ReportUtils.getAbusiveReportCooldown())))
		        .create());

		implementParticipantsSkull(inv);

		inv.setItem(26, MenuItem.DATA.getWithDetails(
		        r.implementData(Message.DATA_DETAILS.get(), u.hasPermission(Permission.STAFF_ADVANCED), vm, bm)));

		int statusPosition = 29;
		boolean archive = u.canArchive(r);
		for (Status status : Status.values()) {
			inv.setItem(statusPosition,
			        status.getButtonItem()
			                .glow(status.equals(r.getStatus()))
			                .name(status == Status.DONE ? Message.PROCESS_STATUS.get()
			                        : Message.CHANGE_STATUS.get().replace("_Status_", status.getWord(null)))
			                .lore((status == Status.DONE ? Message.PROCESS_STATUS_DETAILS.get()
			                        : Message.CHANGE_STATUS_DETAILS.get()).replace("_Status_", status.getWord(null))
			                                .split(ConfigUtils.getLineBreakSymbol()))
			                .create());
			statusPosition += status == Status.IN_PROGRESS && !archive ? 2 : 1;
		}
		if (archive) {
			inv.setItem(33, MenuItem.ARCHIVE.create());
		}

		if (u.hasPermission(Permission.STAFF_DELETE)) {
			inv.setItem(36, MenuItem.DELETE.get());
		}
		inv.setItem(44, MenuItem.COMMENTS.getWithDetails(Message.COMMENTS_DETAILS.get()));

		return inv;
	}

	private void implementParticipantsSkull(Inventory inv) {
		boolean stackedReport = r.isStackedReport();
		for (Report.ParticipantType type : new Report.ParticipantType[] { Report.ParticipantType.REPORTER,
		        Report.ParticipantType.REPORTED }) {
			boolean reporter = type == Report.ParticipantType.REPORTER;
			User participant = reporter ? r.getReporter() : r.getReported();
			String name = participant.getName();

			String details = reporter && stackedReport
			        ? Message.get("Menus.Stacked-report-reporters-details")
			                .replace("_First_", r.getPlayerName(type, true, true, vm, bm))
			                .replace("_Others_", r.getReportersNames(1, true, vm, bm))
			        : Message.PLAYER_DETAILS.get();
			Map<String, Integer> statistics = participant.getStatistics();

			for (Statistic stat : Statistic.values()) {
				String statName = stat.getConfigName();
				String value = null;
				try {
					value = statistics != null ? String.valueOf(statistics.get(statName)) : null;
				} catch (Exception notFound) {}
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
			if (u.hasPermission(Permission.STAFF_TELEPORT)) {
				tp = (participant.isOnlineInNetwork(bm) ? Message.TELEPORT_TO_CURRENT_POSITION
				        : Message.CAN_NOT_TELEPORT_TO_CURRENT_POSITION).get()
				        + (r.getOldLocation(type) != null ? Message.TELEPORT_TO_OLD_POSITION
				                : Message.CAN_NOT_TELEPORT_TO_OLD_POSITION).get();
			}
			inv.setItem(reporter ? 21 : 23, new CustomItem().skullOwner(name)
			        .name((reporter && stackedReport ? Message.get("Menus.Stacked-report-reporters") : type.getName())
			                .replace("_Player_", r.getPlayerName(type, true, true, vm, bm)))
			        .lore(details.replace("_Server_", serverName)
			                .replace("_Teleportation_", tp.replace("_Player_", name))
			                .split(ConfigUtils.getLineBreakSymbol()))
			        .amount(reporter && stackedReport ? r.getReportersAmount() : 1)
			        .create());
		}
	}

	@Override
	public void onClick(ItemStack item, int slot, ClickType click) {
		switch (slot) {
		case 0:
			u.openReportsMenu(1, true, rm, db, taskScheduler, vm, bm, um);
			break;
		case 18:
			u.sendLinesWithReportButton(r.implementDetails(Message.REPORT_CHAT_DETAILS.get(), false, vm, bm)
			        .replace("_Report_", r.getName())
			        .split(ConfigUtils.getLineBreakSymbol()), r);
			break;
		case 21:
		case 23:
			if (!u.hasPermission(Permission.STAFF_TELEPORT) || click == null)
				return;

			Report.ParticipantType targetType = slot == 21 ? Report.ParticipantType.REPORTER
			        : Report.ParticipantType.REPORTED;

			boolean currentLocation = false;
			if (click.isLeftClick()) {
				currentLocation = true;
			} else if (click.isRightClick()) {
				currentLocation = false;
			} else {
				return;
			}
			u.teleportToReportParticipant(r, targetType, currentLocation, vm, bm);
			break;
		case 22:
			u.openReportsMenu(1, false, rm, db, taskScheduler, vm, bm, um);
			r.processAbusive(u, false, u.hasPermission(Permission.STAFF_ARCHIVE_AUTO),
			        ReportUtils.getAbusiveReportCooldown(), true, db);
			break;
		case 26:
			if (click.isLeftClick()) {
				u.sendLinesWithReportButton(
				        r.implementData(Message.REPORT_CHAT_DATA.get(), u.hasPermission(Permission.STAFF_ADVANCED), vm,
				                bm).replace("_Report_", r.getName()).split(ConfigUtils.getLineBreakSymbol()),
				        r);
			} else if (click.isRightClick()) {
				Map<Long, String> sortedMessages = new TreeMap<>();
				for (Report.ParticipantType type : new Report.ParticipantType[] { Report.ParticipantType.REPORTED,
				        Report.ParticipantType.REPORTER }) {
					for (String message : r.getMessagesHistory(type)) {
						if (message != null && message.length() >= 20) {
							String date = message.substring(0, 19);
							sortedMessages.put(DatetimeUtils.getSeconds(date),
							        Message.REPORT_MESSAGE_FORMAT.get()
							                .replace("_Date_", date)
							                .replace("_Player_", r.getPlayerName(type, false, true, vm, bm))
							                .replace("_Message_", message.substring(20)));
						}
					}
				}
				StringBuilder messages = new StringBuilder();
				for (String message : sortedMessages.values())
					messages.append(message);
				u.sendLinesWithReportButton(Message.REPORT_MESSAGES_HISTORY.get()
				        .replace("_Report_", r.getName())
				        .replace("_Messages_",
				                !messages.toString().isEmpty() ? messages.toString() : Message.NONE_MALE.get())
				        .split(ConfigUtils.getLineBreakSymbol()), r);
			}
			break;
		case 36:
			u.openConfirmationMenu(r, ConfirmationMenu.Action.DELETE, rm, db, taskScheduler, vm, bm, um);
			break;
		case 44:
			u.openCommentsMenu(1, r, rm, db, taskScheduler, um, bm, vm);
			break;
		default:
			if ((slot == 32 || slot == 33) && !(u.hasPermission(Permission.STAFF_ARCHIVE)
			        && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives())))
				slot--;
			switch (slot) {
			case 29:
			case 30:
			case 31:
				StatusDetails sd = Report.StatusDetails.from(Arrays.asList(Status.values()).get(slot - 29), u);

				if (slot == 31 && !u.hasPermission(Permission.STAFF_ADVANCED)) {
					u.openReportsMenu(1, true, rm, db, taskScheduler, vm, bm, um);
				} else {
					ConfigSound.MENU.play(p);
				}
				r.setStatus(sd, false, db, rm, bm);
				break;
			case 32:
				u.openProcessMenu(r, rm, db, taskScheduler, vm, bm, um);
				break;
			case 33:
				if (u.canArchive(r)) {
					u.openConfirmationMenu(r, ConfirmationMenu.Action.ARCHIVE, rm, db, taskScheduler, vm, bm, um);
				} else {
					update(false);
				}
				break;
			default:
				break;
			}
			break;
		}
	}

	@Override
	public void onCooldownChange(User u) {
		// Ignored
	}

	@Override
	public void onStatisticsChange(User u) {
		Inventory inv = getOpenInventory();
		if (inv != null) {
			implementParticipantsSkull(inv);
		} else {
			LOGGER.info(
			        () -> this + ": onStatisticsChanged(" + u.getName() + "): open inventoy = null, calls update()");

			update(false);
		}
	}

	@Override
	public void onClose() {
		r.getReported().removeListener(this);
		r.getReporter().removeListener(this);
		super.onClose();
	}

}
