package fr.mrtigreroux.tigerreports.objects.menus;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;

/**
 * @author MrTigreroux
 */

public abstract class ReportManagerMenu extends Menu implements Report.ReportListener {

	private static final Logger LOGGER = Logger.fromClass(ReportManagerMenu.class);

	final int reportId;
	final boolean withAdvancedData;
	final boolean allowAccessIfArchived;
	protected Report r = null;
	protected boolean reportCollectionRequested = false;
	protected final ReportsManager rm;
	protected final Database db;
	protected final TaskScheduler taskScheduler;
	protected final UsersManager um;

	public ReportManagerMenu(User u, int size, int page, Permission permission, int reportId, ReportsManager rm,
	        Database db, TaskScheduler taskScheduler, UsersManager um) {
		this(u, size, page, permission, reportId, false, rm, db, taskScheduler, um);
	}

	public ReportManagerMenu(User u, int size, int page, Permission permission, int reportId,
	        boolean allowAccessIfArchived, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        UsersManager um) {
		this(u, size, page, permission, reportId, false, allowAccessIfArchived, rm, db, taskScheduler, um);
	}

	public ReportManagerMenu(User u, int size, int page, Permission permission, int reportId, boolean withAdvancedData,
	        boolean allowAccessIfArchived, ReportsManager rm, Database db, TaskScheduler taskScheduler,
	        UsersManager um) {
		super(u, size, page, permission);
		this.reportId = reportId;
		this.withAdvancedData = withAdvancedData;
		this.allowAccessIfArchived = allowAccessIfArchived;
		this.rm = rm;
		this.db = db;
		this.taskScheduler = taskScheduler;
		this.um = um;
	}

	@Override
	public void open(boolean sound) {
		LOGGER.info(() -> this + ": open(): add listener for report " + reportId);
		rm.addReportListener(reportId, this, false, db, taskScheduler, um); // Prevents the report from being removed from cache, this.r must be the same instance that in cache
		if (isValidReport(r)) {
			LOGGER.info(() -> this + ": open(): valid report " + reportId + ", opening...");
			super.open(sound);
		} else if (!reportCollectionRequested) {
			reportCollectionRequested = true;
			LOGGER.info(() -> this + ": open(): start collection of report " + reportId);
			rm.getReportByIdAsynchronously(reportId, withAdvancedData, true, db, taskScheduler, um,
			        new ResultCallback<Report>() {

				        @Override
				        public void onResultReceived(Report r) {
					        ReportManagerMenu.this.r = r;
					        LOGGER.info(() -> this + ": open(): report " + reportId + " collected, opening...");
					        ReportManagerMenu.super.open(sound);
					        reportCollectionRequested = false;
				        }

			        });
		}
	}

	private boolean isValidReport(Report r) {
		return r != null && (!withAdvancedData || r.hasAdvancedData());
	}

	@Override
	public void onReportDataChange(Report r) {
		if (reportId == r.getId()) {
			if (!isValidReport(this.r)) { // Menu has not yet been opened, this.r is probably not yet valid and update() would therefore trigger an error.
				LOGGER.info(() -> this + ": onReportDataChanged(" + r.getId() + "): user = " + u.getName()
				        + ", menu report is not (yet) valid, no update");
				return;
			}

			LOGGER.info(() -> this + ": onReportDataChanged(" + r.getId() + "): user = " + u.getName()
			        + ", calls update()");
			update(false);
		}
	}

	@Override
	public void onReportDelete(int reportId) {
		if (this.reportId == reportId) {
			LOGGER.info(() -> this + ": onReportDeleted(" + reportId + "): user = " + u.getName());

			MessageUtils.sendErrorMessage(p, Message.INVALID_REPORT.get());
			p.closeInventory();
		}
	}

	String checkReport() {
		if (!isValidReport(r)) {
			return Message.INVALID_REPORT.get();
		} else if (!u.canAccessToReport(r, allowAccessIfArchived)) {
			return Message.PERMISSION_ACCESS_DETAILS.get().replace("_Report_", r.getName());
		} else {
			return null;
		}
	}

	public ReportManagerMenu setReport(Report r) {
		if (isValidReport(r)) {
			this.r = r;
		}
		return this;
	}

	@Override
	public void onClose() {
		LOGGER.info(() -> this + ": onClose(): remove listener for report " + reportId);
		rm.removeReportListener(reportId, this);
		super.onClose();
	}

}
