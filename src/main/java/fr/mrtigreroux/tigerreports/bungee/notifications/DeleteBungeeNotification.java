package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.Map;
import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class DeleteBungeeNotification extends BungeeNotification {

	private static final Logger LOGGER = Logger.BUNGEE.newChild(DeleteBungeeNotification.class);

	public final String reportBasicDataString;
	public final String staffUniqueId;

	public DeleteBungeeNotification(long creationTime, String reportBasicDataString, UUID staffUUID) {
		this(creationTime, reportBasicDataString, staffUUID.toString());
	}

	public DeleteBungeeNotification(long creationTime, String reportBasicDataString, String staffUniqueId) {
		super(creationTime);
		this.reportBasicDataString = CheckUtils.notEmpty(reportBasicDataString);
		this.staffUniqueId = CheckUtils.notEmpty(staffUniqueId);
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}

	@Override
	public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
	BungeeManager bm) {
		// The report is not saved in cache, and is even deleted from cache if cached.
		Map<String, Object> reportData = Report.parseBasicDataFromString(reportBasicDataString);
		if (reportData == null) {
			throw new IllegalStateException("Invalid reportBasicDataString: " + reportBasicDataString);
		}

		Report.asynchronouslyFrom(reportData, false, db, ts, um, (r) -> {
			if (r == null) {
				LOGGER.error("Report.asynchronouslyFrom failed for reportBasicDataString = " + reportBasicDataString);
				return;
			}

			if (isNotifiable(bm)) {
				um.getUserByUniqueIdAsynchronously(staffUniqueId, db, ts, (u) -> {
					r.delete(u, true, db, ts, rm, vm, bm);
				});
			} else {
				r.delete(null, true, db, ts, rm, vm, bm);
			}
		});
	}

}
