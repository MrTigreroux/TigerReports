package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.UUID;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CheckUtils;

/**
 * @author MrTigreroux
 */
public class ProcessBungeeNotification extends BungeeNotification {

	private static final Logger LOGGER = Logger.BUNGEE.newChild(ProcessBungeeNotification.class);

	public final int reportId;
	public final String staffUniqueId;
	public final boolean archive;
	public final String serializedAppreciation;

	public ProcessBungeeNotification(long creationTime, int reportId, UUID staffUUID, boolean archive, Appreciation appreciation) {
		this(creationTime, reportId, staffUUID.toString(), archive, appreciation.getConfigName());
	}

	public ProcessBungeeNotification(long creationTime, int reportId, String staffUniqueId, boolean archive, String serializedAppreciation) {
		super(creationTime);
		this.reportId = reportId;
		this.staffUniqueId = CheckUtils.notEmpty(staffUniqueId);
		this.archive = archive;
		this.serializedAppreciation = CheckUtils.notEmpty(serializedAppreciation);
	}

	@Override
	public boolean isEphemeral() {
		return false;
	}

	public Appreciation getAppreciation() {
		return Appreciation.from(serializedAppreciation);
	}

	@Override
	public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm, VaultManager vm, BungeeManager bm) {
		getReportAsync(reportId, db, ts, um, rm, bm, LOGGER, (r) -> {
			um.getUserByUniqueIdAsynchronously(staffUniqueId, db, ts, (u) -> {
				r.process(u, getAppreciation(), true, archive, isNotifiable(bm), db, rm, vm, bm, ts);
			});
		});
	}

}
