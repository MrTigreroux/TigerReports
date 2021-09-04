package fr.mrtigreroux.tigerreports.managers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.constants.Pair;
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
		return reportId <= 0 ? null
		        : reports.containsKey(reportId) ? reports.get(reportId)
		                : getFullReport(TigerReports.getInstance()
		                        .getDb()
		                        .query("SELECT * FROM tigerreports_reports WHERE report_id = ? "
		                                + (notArchived ? "AND archived = 0 " : "") + "LIMIT 1",
		                                Collections.singletonList(reportId))
		                        .getResult(0));
	}

	public Pair<Report, Boolean> getReportByIdAndArchiveInfo(int reportId) {
		Map<String, Object> result = TigerReports.getInstance()
		        .getDb()
		        .query("SELECT * FROM tigerreports_reports WHERE report_id = ? LIMIT 1",
		                Collections.singletonList(reportId))
		        .getResult(0);
		return reportId <= 0 || result == null ? null
		        : new Pair<Report, Boolean>(getFullReport(result), (Boolean) result.get("archived"));
	}

	public Report getReport(boolean archived, int reportIndex) {
		return getFullReport(TigerReports.getInstance()
		        .getDb()
		        .query("SELECT * FROM tigerreports_reports WHERE archived = ?"
		                + (archived ? " ORDER BY report_id DESC" : "") + " LIMIT 1 OFFSET ?",
		                Arrays.asList(archived ? 1 : 0, reportIndex - 1))
		        .getResult(0));
	}

	public Report getFullReport(Map<String, Object> result) {
		if (result == null)
			return null;

		Report r = ReportUtils.getEssentialOfReport(result);

		Map<String, String> advancedData = new HashMap<>();
		Set<String> advancedKeys = new HashSet<>(result.keySet());
		advancedKeys.removeAll(Arrays.asList("report_id", "status", "appreciation", "date", "reported_uuid",
		        "reporter_uuid", "reason", "archived"));
		for (String key : advancedKeys)
			advancedData.put(key, (String) result.get(key));
		r.setAdvancedData(advancedData);
		saveReport(r);

		return r;
	}

}