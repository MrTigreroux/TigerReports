package fr.mrtigreroux.tigerreports.objects.menus;

import org.bukkit.Bukkit;

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

	final int reportId;
	protected Report r = null;
	protected boolean reportCollected = false;

	public ReportManagerMenu(OnlineUser u, int size, int page, Permission permission, int reportId) {
		super(u, size, page, permission);
		this.reportId = reportId;
	}

	String checkReport() {
		if (r == null || !TigerReports.getInstance().getReportsManager().isSaved(r.getId()))
			return Message.INVALID_REPORT.get();

		Status reportStatus = r.getStatus();
		return (reportStatus == Status.IMPORTANT || reportStatus == Status.DONE) && !Permission.STAFF_ADVANCED.isOwned(u) ? Message.PERMISSION_ACCESS_DETAILS.get().replace("_Report_",r.getName()) : null;
	}

	protected boolean collectReport() {
		if (reportCollected) {
			return true;
		} else {
			TigerReports tr = TigerReports.getInstance();
			Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

				@Override
				public void run() {
					Report r = tr.getReportsManager().getReportById(reportId, true);
					Bukkit.getScheduler().runTask(tr, new Runnable() {

						@Override
						public void run() {
							setReport(r);
							open(true);
						}

					});
				}

			});
			return false;
		}
	}

	public void setReport(Report r) {
		this.reportCollected = true;
		this.r = r;
	}

}
