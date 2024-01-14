package fr.mrtigreroux.tigerreports.bungee.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */
public class NewReportBungeeNotification extends BungeeNotification {
    
    private static final Logger LOGGER = Logger.BUNGEE.newChild(NewReportBungeeNotification.class);
    
    private final String reportServer;
    private final boolean missingData;
    private final String reportBasicData;
    
    public NewReportBungeeNotification(long creationTime, String reportServer, boolean missingData,
            String reportBasicData) {
        super(creationTime);
        this.reportServer = reportServer;
        this.missingData = missingData;
        this.reportBasicData = reportBasicData;
    }
    
    @Override
    public boolean isEphemeral() {
        return false;
    }
    
    @Override
    public void onReceive(Database db, TaskScheduler ts, UsersManager um, ReportsManager rm,
            VaultManager vm, BungeeManager bm) {
        bm.setServerLastNotificationTime(reportServer, creationTime);
        
        Map<String, Object> reportData = Report.parseBasicDataFromString(reportBasicData);
        if (reportData == null) {
            LOGGER.info(
                    () -> "processNewReportMessage(): reportData = null, reportDataAsString = "
                            + reportBasicData
            );
            return;
        }
        
        int reportId = (int) reportData.get(Report.REPORT_ID);
        
        boolean isRecent = isRecent(bm);
        boolean notify = isNotifiable(bm);
        LOGGER.info(
                () -> "processNewReportMessage(): reportData = " + CollectionUtils
                        .toString(reportData) + ", isRecent = " + isRecent + ", notify = " + notify
        );
        
        if (isRecent) {
            rm.updateAndGetReport(
                    reportId,
                    reportData,
                    false,
                    false,
                    db,
                    ts,
                    um,
                    createNewReportResultCallback(
                            notify,
                            reportServer,
                            missingData,
                            reportBasicData,
                            reportData,
                            db,
                            ts,
                            um,
                            vm,
                            bm
                    )
            );
        } else if (notify) {
            rm.getReportByIdAsynchronously(
                    reportId,
                    false,
                    false,
                    db,
                    ts,
                    um,
                    createNewReportResultCallback(
                            true,
                            reportServer,
                            missingData,
                            reportBasicData,
                            reportData,
                            db,
                            ts,
                            um,
                            vm,
                            bm
                    )
            );
        } else {
            rm.getReportByIdAsynchronously(reportId, false, false, db, ts, um, (r) -> {
                if (r != null) {
                    ReportUtils.sendReport(r, reportServer, notify, db, vm, bm);
                }
            });
        }
    }
    
    private ResultCallback<Report> createNewReportResultCallback(boolean notify,
            String reportServer, boolean reportMissingData, String reportDataAsString,
            Map<String, Object> reportData, Database db, TaskScheduler ts, UsersManager um,
            VaultManager vm, BungeeManager bm) {
        return (r) -> {
            if (r != null && r.getBasicDataAsString().equals(reportDataAsString)) {
                sendReportAndImplementMissingData(
                        r,
                        reportServer,
                        notify,
                        reportMissingData,
                        db,
                        vm,
                        bm
                );
            } else {
                sendReportWithReportData(
                        reportData,
                        reportServer,
                        notify,
                        reportMissingData,
                        db,
                        ts,
                        um,
                        vm,
                        bm
                );
            }
        };
    }
    
    private void sendReportAndImplementMissingData(Report r, String reportServer, boolean notify,
            boolean reportMissingData, Database db, VaultManager vm, BungeeManager bm) {
        ReportUtils.sendReport(r, reportServer, notify, db, vm, bm);
        if (reportMissingData) {
            implementMissingData(r, db, bm);
        }
    }
    
    private void implementMissingData(Report r, Database db, BungeeManager bm) {
        Map<String, Object> reportData = new HashMap<>();
        if (ReportUtils.collectAndFillReportedData(r.getReported(), bm, reportData)) {
            StringBuilder queryArguments = new StringBuilder();
            List<Object> queryParams = new ArrayList<>();
            for (Entry<String, Object> data : reportData.entrySet()) {
                if (queryArguments.length() > 0) {
                    queryArguments.append(",");
                }
                queryArguments.append("`").append(data.getKey()).append("`=?");
                queryParams.add(data.getValue());
            }
            queryParams.add(r.getId());
            
            String query =
                    "UPDATE tigerreports_reports SET " + queryArguments + " WHERE report_id=?";
            db.updateAsynchronously(query, queryParams);
        }
    }
    
    private void sendReportWithReportData(Map<String, Object> reportData, String reportServer,
            boolean notify, boolean reportMissingData, Database db, TaskScheduler ts,
            UsersManager um, VaultManager vm, BungeeManager bm) {
        Report.asynchronouslyFrom(reportData, false, db, ts, um, (r) -> {
            if (r != null) {
                sendReportAndImplementMissingData(
                        r,
                        reportServer,
                        notify,
                        reportMissingData,
                        db,
                        vm,
                        bm
                );
            }
        });
    }
    
    @Override
    public String toString() {
        return "NewReportBungeeNotification [reportServer=" + reportServer + ", missingData="
                + missingData + ", reportBasicData=" + reportBasicData + "]";
    }
    
}
