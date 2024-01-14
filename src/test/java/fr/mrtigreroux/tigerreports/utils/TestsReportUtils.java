package fr.mrtigreroux.tigerreports.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.TestsReport;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.SeveralTasksHandler;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class TestsReportUtils {
    
    private static final Logger LOGGER = Logger.fromClass(TestsReportUtils.class);
    //	InetSocketAddress mockRandomIPAddress() {
    //		InetAddress ip = mock(InetAddress.class);
    //		Random r = new Random();
    //		when(ip.toString()).thenReturn("/" + getRandomInt(r, 0, 255) + "." + getRandomInt(r, 0, 255) + "."
    //		        + getRandomInt(r, 0, 255) + "." + getRandomInt(r, 0, 255));
    //
    //		return new InetSocketAddress(ip, getRandomInt(r, 0, 65535));
    //	}
    
    /**
     * Used to don't affect tests' reports manager cache and to avoid creating a reports manager for
     * each method call.
     */
    private static ReportsManager independentRM = new ReportsManager();
    
    public static void resetIndependentReportsManager() {
        independentRM = new ReportsManager();
    }
    
    public static void setupRandomUserMockForCreateReportAsynchronously(User userMock,
            Player playerMock) {
        if (playerMock != null) {
            // InetSocketAddress mockRandomIPAddress = mockRandomIPAddress();
            // when(mock.getAddress()).thenReturn(mockRandomIPAddress);
            Location mockRandomLocation = TestsReport.mockRandomLocation();
            if (mockRandomLocation == null) {
                LOGGER.error(
                        "setupRandomUserMockForCreateReportAsynchronously(): randomLoc = null"
                );
            } else if (mockRandomLocation.getWorld() == null) {
                LOGGER.error(
                        "setupRandomUserMockForCreateReportAsynchronously(): randomLoc.getworld() = null"
                );
            }
            LOGGER.info(
                    () -> "setupRandomUserMockForCreateReportAsynchronously(): mockRandomLocation = "
                            + mockRandomLocation
            );
            when(playerMock.getLocation()).thenReturn(mockRandomLocation);
        }
        
        when(userMock.getUniqueId()).thenReturn(UUID.randomUUID());
        when(userMock.getPlayer()).thenReturn(playerMock);
        Random r = new Random();
        if (playerMock != null) {
            when(userMock.getIPAddress()).thenReturn(TestsReport.getRandomIPAddress(r));
        } else {
            when(userMock.getIPAddress()).thenReturn(null);
        }
        when(userMock.getLastMessages()).thenReturn(TestsReport.getRandomSavedMessages(r));
    }
    
    public static void setupMockOfCollectAndFillReportedData(MockedStatic<ReportUtils> mock,
            User reported, Map<String, Object> reportedAdvancedData) {
        mock.when(
                () -> ReportUtils.collectAndFillReportedData(any(User.class), any(BungeeManager.class), ArgumentMatchers.<Map<String, Object>>any())
        ).then((invocation) -> {
            User ru = invocation.getArgument(0);
            Player rp = ru.getPlayer();
            //			        BungeeManager bm = invocation.getArgument(1);
            Map<String, Object> data = invocation.getArgument(2);
            
            if (rp == null) {
                return false;
            }
            
            data.putAll(reportedAdvancedData);
            return true;
        });
    }
    
    /**
     * Create random reports and then collect them from the database to return them in the same
     * order than they are in the database.
     * 
     * @param reportsAmount
     * @param taskScheduler
     * @param db
     * @param resultCallback
     */
    public static void createRandomReportsAsynchronously(int reportsAmount,
            TaskScheduler taskScheduler, Database db, ResultCallback<List<Report>> resultCallback) {
        SeveralTasksHandler<Report> reportsTaskHandler = new SeveralTasksHandler<>();
        
        Random rand = new Random();
        for (int i = 0; i < reportsAmount; i++) {
            createRandomReportAsynchronously(
                    rand.nextBoolean(),
                    rand.nextBoolean(),
                    taskScheduler,
                    db,
                    false,
                    reportsTaskHandler.newTaskResultSlot()
            );
        }
        
        reportsTaskHandler.whenAllTasksDone(true, (reports) -> {
            Set<Integer> reportsId =
                    reports.stream().map(r -> r.getId()).collect(Collectors.toSet());
            LOGGER.info(
                    () -> "createRandomReportsAsynchronously(): ids = "
                            + CollectionUtils.toString(reportsId)
            );
            
            int failedReportsAmount = reportsAmount - reports.size();
            if (failedReportsAmount > 0) {
                fail(
                        "createRandomReportsAsynchronously(): failed creation of "
                                + failedReportsAmount + " reports"
                );
            }
            
            UsersManager um = mock(UsersManager.class);
            setupUMForReports(um, reports);
            
            independentRM.getReportsByIdAsynchronously(
                    reportsId,
                    true,
                    db,
                    taskScheduler,
                    um,
                    (finalReports) -> {
                        if (reportsAmount != 0) {
                            assertEquals(reportsAmount, finalReports.size());
                        } else {
                            assertNull(finalReports);
                            finalReports = new ArrayList<>();
                        }
                        resultCallback.onResultReceived(finalReports);
                    }
            );
        });
    }
    
    public static void createRandomReportAsynchronously(boolean reportedOnline,
            boolean reporterOnline, TaskScheduler taskScheduler, Database db,
            ResultCallback<Report> resultCallback) {
        createRandomReportAsynchronously(
                reportedOnline,
                reporterOnline,
                taskScheduler,
                db,
                true,
                resultCallback
        );
    }
    
    public static void createRandomReportAsynchronously(boolean reportedOnline,
            boolean reporterOnline, TaskScheduler taskScheduler, Database db, boolean collectFromDb,
            ResultCallback<Report> resultCallback) {
        User reported = mock(User.class);
        TestsReportUtils.setupRandomUserMockForCreateReportAsynchronously(
                reported,
                reportedOnline ? mock(Player.class) : null
        );
        
        User reporter = mock(User.class);
        TestsReportUtils.setupRandomUserMockForCreateReportAsynchronously(
                reporter,
                reporterOnline ? mock(Player.class) : null
        );
        List<User> reporters = Arrays.asList(reporter);
        
        String reason = "report reason";
        String date = DatetimeUtils.getNowDatetime();
        
        BungeeManager bm = mock(BungeeManager.class);
        when(bm.getServerName()).thenReturn("bungeeServerName");
        
        Map<String, Object> reportAdvancedData = new HashMap<>();
        reportAdvancedData.put(Report.AdvancedData.REPORTER_IP, reporter.getIPAddress());
        reportAdvancedData.put(
                Report.AdvancedData.REPORTER_LOCATION,
                reporterOnline
                        ? SerializationUtils
                                .serializeLocation(reporter.getPlayer().getLocation(), bm)
                        : null
        );
        reportAdvancedData.put(
                Report.AdvancedData.REPORTER_MESSAGES,
                Report.AdvancedData.serializeMessages(reporter.getLastMessages())
        );
        
        Map<String, Object> reportedAdvancedData = new HashMap<>();
        reportedAdvancedData.put(Report.AdvancedData.REPORTED_IP, reported.getIPAddress());
        reportedAdvancedData.put(
                Report.AdvancedData.REPORTED_LOCATION,
                reportedOnline
                        ? SerializationUtils
                                .serializeLocation(reported.getPlayer().getLocation(), bm)
                        : null
        );
        reportedAdvancedData.put(
                Report.AdvancedData.REPORTED_MESSAGES,
                Report.AdvancedData.serializeMessages(reported.getLastMessages())
        );
        if (reportedOnline) {
            TestsReport.fillMissingWithRandomReportedAdvancedData(reportedAdvancedData, bm);
        }
        
        reportAdvancedData.putAll(reportedAdvancedData);
        
        UsersManager um = mock(UsersManager.class);
        TestsReport.setupUMForAsynchronouslyFrom(um, reported, reporters);
        
        try (
                MockedStatic<ReportUtils> reportUtilsMock =
                        mockStatic(ReportUtils.class, Mockito.CALLS_REAL_METHODS)
        ) {
            TestsReportUtils.setupMockOfCollectAndFillReportedData(
                    reportUtilsMock,
                    reported,
                    reportedAdvancedData
            );
            
            ReportUtils.createReportAsynchronously(
                    reporter,
                    reported,
                    reason,
                    date,
                    false,
                    taskScheduler,
                    db,
                    bm,
                    um,
                    (cr) -> {
                        if (cr != null && cr.r != null && cr.r.getId() >= 0) {
                            if (collectFromDb) {
                                independentRM.getReportByIdAsynchronously(
                                        cr.r.getId(),
                                        true,
                                        false,
                                        db,
                                        taskScheduler,
                                        um,
                                        resultCallback
                                );
                            } else {
                                resultCallback.onResultReceived(cr.r);
                            }
                        } else {
                            LOGGER.error(
                                    "createRandomReportAsynchronously(): invalid report or id"
                            );
                            resultCallback.onResultReceived(null);
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void setupUMForReports(UsersManager um, List<Report> reports) {
        Map<String, User> reportedsByUUID = new HashMap<>();
        Map<String, List<User>> reportersByUUID = new HashMap<>();
        Map<UUID, User> staffsByUUID = new HashMap<>();
        
        for (Report r : reports) {
            User reported = r.getReported();
            List<User> reporters = r.getReporters();
            User staff = r.getProcessorStaff();
            if (staff == null) {
                staff = r.getProcessingStaff();
            }
            
            String reportedUUID = reported.getUniqueId().toString();
            String reportersUUID = TestsReport.getReportersStr(reporters);
            UUID staffUUID = staff != null ? staff.getUniqueId() : null;
            
            reportedsByUUID.put(reportedUUID, reported);
            reportersByUUID.put(reportersUUID, reporters);
            staffsByUUID.put(staffUUID, staff);
        }
        LOGGER.debug(() -> "setupUMForReports()");
        
        doAnswer((invocation) -> {
            String uuid = invocation.getArgument(0);
            ResultCallback<User> callback = invocation.getArgument(3);
            User reported = reportedsByUUID.get(uuid);
            if (reported != null) {
                callback.onResultReceived(reported);
            } else {
                LOGGER.warn(
                        () -> "setupUMForReports(): getUserAsynchronously(String) unexpected reported uuid: "
                                + uuid
                );
                callback.onResultReceived(null);
            }
            return null;
        }).when(um)
                .getUserByUniqueIdAsynchronously(anyString(), any(Database.class), any(TaskScheduler.class), ArgumentMatchers.<ResultCallback<User>>any());
        
        doAnswer((invocation) -> {
            Object uuid = invocation.getArgument(0);
            ResultCallback<User> callback = invocation.getArgument(3);
            User staff = staffsByUUID.get(uuid);
            if (staff != null) {
                callback.onResultReceived(staff);
            } else {
                LOGGER.warn(
                        () -> "setupUMForReports(): getUserAsynchronously(UUID) unexpected staff uuid: "
                                + uuid
                );
                callback.onResultReceived(null);
            }
            return null;
        }).when(um)
                .getUserByUniqueIdAsynchronously(any(UUID.class), any(Database.class), any(TaskScheduler.class), ArgumentMatchers.<ResultCallback<User>>any());
        
        doAnswer((invocation) -> {
            String[] uuids = invocation.getArgument(0);
            ResultCallback<List<User>> callback = invocation.getArgument(3);
            List<User> reporters =
                    reportersByUUID.get(String.join(Report.REPORTERS_SEPARATOR, uuids));
            if (reporters != null) {
                callback.onResultReceived(reporters);
            } else {
                LOGGER.warn(
                        () -> "setupUMForReports(): getUsersAsynchronously(): unexpected reporters uuid: "
                                + Arrays.toString(uuids)
                );
                callback.onResultReceived(null);
            }
            return null;
        }).when(um)
                .getUsersByUniqueIdAsynchronously(any(String[].class), any(Database.class), any(TaskScheduler.class), ArgumentMatchers.<ResultCallback<List<User>>>any());
    }
    
    public static void getReportByIdAsynchronously(Report r, TaskScheduler taskScheduler,
            Database db, UsersManager umMock, ReportsManager rm,
            ResultCallback<Report> resultCallback) {
        User staff = r.getProcessingStaff();
        if (staff == null) {
            staff = r.getProcessorStaff();
        }
        getReportByIdAsynchronously(
                r.getId(),
                r.getReported(),
                r.getReporters(),
                staff,
                taskScheduler,
                db,
                umMock,
                rm,
                resultCallback
        );
    }
    
    public static void getReportByIdAsynchronously(int reportId, User reported,
            List<User> reporters, TaskScheduler taskScheduler, Database db, UsersManager umMock,
            ReportsManager rm, ResultCallback<Report> resultCallback) {
        getReportByIdAsynchronously(
                reportId,
                reported,
                reporters,
                null,
                taskScheduler,
                db,
                umMock,
                rm,
                resultCallback
        );
    }
    
    public static void getReportByIdAsynchronously(int reportId, User reported,
            List<User> reporters, User staff, TaskScheduler taskScheduler, Database db,
            UsersManager umMock, ReportsManager rm, ResultCallback<Report> resultCallback) {
        UsersManager um = mock(UsersManager.class);
        TestsReport.setupUMForAsynchronouslyFrom(um, reported, reporters, staff);
        
        rm.getReportByIdAsynchronously(
                reportId,
                true,
                false,
                db,
                taskScheduler,
                um,
                resultCallback
        );
    }
    
}
