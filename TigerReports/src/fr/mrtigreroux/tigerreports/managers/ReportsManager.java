package fr.mrtigreroux.tigerreports.managers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class ReportsManager {

	private final Map<Integer, Report> reports = new HashMap<>();

	public ReportsManager() {}

	public void saveReport(Report r) {
		reports.put(r.getId(), r);
	}

	public void removeReport(int id) {
		reports.remove(id);
	}

	public boolean isSaved(int id) {
		return reports.containsKey(id);
	}

	public void clearReports() {
		reports.clear();
	}

	public Report getReportById(int reportId, boolean notArchived) {
		return reportId <= 0	? null
								: reports.containsKey(reportId)	? reports.get(reportId)
																: formatFullReport(TigerReports.getInstance()
																		.getDb()
																		.query("SELECT * FROM tigerreports_reports WHERE report_id = ? "+(notArchived
																																						? "AND archived = 0 "
																																						: "")
																				+"LIMIT 1", Arrays.asList(reportId))
																		.getResult(0));
	}

	public Report getReport(int reportIndex) {
		return formatFullReport(TigerReports.getInstance()
				.getDb()
				.query("SELECT * FROM tigerreports_reports WHERE archived = ? LIMIT 1 OFFSET ?", Arrays.asList(0, reportIndex-1))
				.getResult(0));
	}

	public Report formatFullReport(Map<String, Object> result) {
		Report r = ReportUtils.formatEssentialOfReport(result);

		Map<String, String> advancedData = new HashMap<>();
		Set<String> advancedKeys = new HashSet<>(result.keySet());
		advancedKeys.removeAll(Arrays.asList("report_id", "status", "appreciation", "date", "reported_uuid", "reporter_uuid", "reason", "archived"));
		for (String key : advancedKeys)
			advancedData.put(key, (String) result.get(key));
		r.setAdvancedData(advancedData);
		saveReport(r);

		return r;
	}

}
