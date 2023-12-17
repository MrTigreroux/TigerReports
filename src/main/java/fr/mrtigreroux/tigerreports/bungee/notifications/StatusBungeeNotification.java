package fr.mrtigreroux.tigerreports.bungee.notifications;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class StatusBungeeNotification extends BungeeNotification {

	public final int reportId;
	public final String statusDetails;

	public StatusBungeeNotification(long creationTime, int reportId, String statusDetails) {
		super(creationTime);
		this.reportId = reportId;
		this.statusDetails = statusDetails;
	}

	@Override
	public boolean isEphemeral() {
		return false;
	}

	@Override
	public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm,
	        BungeeManager bm) {
		rm.getReportByIdAsynchronously(reportId, false, true, db, ts, um, (r) -> {
			if (r != null) {
				Report.StatusDetails.asynchronouslyFrom(statusDetails, db, ts, um, (sd) -> {
					r.setStatus(sd, true, db, rm, bm);
				});
			}
		});
	}

}
