package fr.mrtigreroux.tigerreports.objects.menus;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;

/**
 * @author MrTigreroux
 */

public abstract class ReportManagerMenu extends Menu {

	final Report r;

	public ReportManagerMenu(OnlineUser u, int size, int page, Permission permission, Report report) {
		super(u, size, page, permission);
		this.r = report;
	}

	String checkReport() {
		if (r == null || !TigerReports.getInstance().getReportsManager().isSaved(r.getId()))
			return Message.INVALID_REPORT.get();

		Status reportStatus = r.getStatus();
		return (reportStatus == Status.IMPORTANT || reportStatus == Status.DONE) && !Permission.STAFF_ADVANCED.isOwned(u)
				? Message.PERMISSION_ACCESS_DETAILS.get().replace("_Report_", r.getName()) : null;
	}

}
