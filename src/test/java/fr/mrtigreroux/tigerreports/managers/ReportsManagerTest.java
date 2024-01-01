package fr.mrtigreroux.tigerreports.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.TestClass;
import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.Holder;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.TestsDatabaseManager;
import fr.mrtigreroux.tigerreports.data.database.TestsSQLite;
import fr.mrtigreroux.tigerreports.managers.TestsReportsManager.FakeReportListener;
import fr.mrtigreroux.tigerreports.managers.TestsReportsManager.FakeReportsPageListener;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPage;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPageCharacteristics;
import fr.mrtigreroux.tigerreports.objects.reports.TestsReport;
import fr.mrtigreroux.tigerreports.tasks.TaskCompletion;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.AssertionUtils;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.RandomUtils;
import fr.mrtigreroux.tigerreports.utils.TestsReportUtils;

/**
 * @author MrTigreroux
 */
class ReportsManagerTest extends TestClass {

    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#freeUnlistenedReportsFromCache()}.
    //	 */
    //	@Test
    //	void testFreeUnlistenedReportsFromCache() {
    //		fail("Not yet implemented");
    //	}

    /**
     * Test methods for
     * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#addReportListener(int, fr.mrtigreroux.tigerreports.objects.reports.Report.ReportListener, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager)}.
     */
    @Nested
    class AddReportListener {

        @Test
        void testAddReportListener1() {
            testAddReportListener(true, false);
        }

        @Test
        void testAddReportListener2() {
            testAddReportListener(false, false);
        }

        @Test
        void testAddReportListenerTwoTimes1() {
            testAddReportListener(true, true);
        }

        @Test
        void testAddReportListenerTwoTimes2() {
            testAddReportListener(false, true);
        }

        void testAddReportListener(boolean updateDataIfNoListener, boolean twoTimes) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            Holder<Integer> menuUpdaterStartIfNeededCalledTimes = new Holder<>(0);
            Holder<Report> reportH = new Holder<>();

            AtomicInteger reportDataChangeNotifications = new AtomicInteger();
            AtomicInteger reportDeleteNotifications = new AtomicInteger();

            Report.ReportListener reportListener = new Report.ReportListener() {

                @Override
                public void onReportDelete(int reportId) {
                    if (reportH.get() != null && reportH.get().getId() == reportId) {
                        reportDeleteNotifications.incrementAndGet();
                    }
                }

                @Override
                public void onReportDataChange(Report r) {
                    if (r != null && r.equals(reportH.get())) {
                        reportDataChangeNotifications.incrementAndGet();
                    }
                }

            };

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r) -> {
                    reportH.set(r);
                    try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                        TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                menuUpdaterStaticMock, menuUpdaterStartIfNeededCalledTimes);

                        rm.addReportListener(r.getId(), reportListener, updateDataIfNoListener, db, taskScheduler, um);
                        if (twoTimes) {
                            rm.addReportListener(r.getId(), reportListener, updateDataIfNoListener, db, taskScheduler,
                                    um);
                        }
                        rm.broadcastReportDataChanged(r);
                        tc.setDone();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT));
            Report r = reportH.get();
            assertNotNull(r);
            assertTrue(rm.isReportListenerListeningToReport(r.getId(), reportListener));
            assertTrue(rm.hasReportListener(r.getId()));

            verify(rm, times(updateDataIfNoListener ? 1 : 0)).updateDataWhenPossible(any(Database.class),
                    any(TaskScheduler.class), any(UsersManager.class));
            assertEquals(updateDataIfNoListener ? 1 : 0, menuUpdaterStartIfNeededCalledTimes.get());

            assertEquals(1, reportDataChangeNotifications.get());
            assertEquals(0, reportDeleteNotifications.get());
        }

        @Test
        void testAddSeveralReportListeners1() {
            testAddSeveralReportListeners(true, 1);
        }

        @Test
        void testAddSeveralReportListeners2() {
            testAddSeveralReportListeners(false, 1);
        }

        @Test
        void testAddSeveralReportListeners3() {
            testAddSeveralReportListeners(true, 3);
        }

        void testAddSeveralReportListeners(boolean updateDataIfNoListener, int broadcastReportDataChangedAmount) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            Holder<Integer> menuUpdaterStartIfNeededCalledTimes = new Holder<>(0);
            Holder<Report> reportH = new Holder<>();

            List<FakeReportListener> fakeReportListeners = Arrays.asList(
                    new TestsReportsManager.FakeReportListener(reportH),
                    new TestsReportsManager.FakeReportListener(reportH));

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r) -> {
                    reportH.set(r);
                    try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                        TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                menuUpdaterStaticMock, menuUpdaterStartIfNeededCalledTimes);

                        for (FakeReportListener fakeReportListener : fakeReportListeners) {
                            rm.addReportListener(r.getId(), fakeReportListener.reportListener, updateDataIfNoListener,
                                    db, taskScheduler, um);
                        }
                        for (int i = 0; i < broadcastReportDataChangedAmount; i++) {
                            rm.broadcastReportDataChanged(r);
                        }
                        tc.setDone();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT));
            Report r = reportH.get();
            assertNotNull(r);
            assertTrue(rm.hasReportListener(r.getId()));
            verify(rm, times(updateDataIfNoListener ? 1 : 0)).updateDataWhenPossible(any(Database.class),
                    any(TaskScheduler.class), any(UsersManager.class));
            assertEquals(updateDataIfNoListener ? 1 : 0, menuUpdaterStartIfNeededCalledTimes.get());

            for (FakeReportListener fakeReportListener : fakeReportListeners) {
                assertTrue(rm.isReportListenerListeningToReport(r.getId(), fakeReportListener.reportListener));
                assertEquals(broadcastReportDataChangedAmount, fakeReportListener.reportDataChangeNotifications.get());
                assertEquals(0, fakeReportListener.reportDeleteNotifications.get());
            }
        }

        @Test
        void testAddReportListenerToSeveralReports1() {
            testAddReportListenerToSeveralReports(true);
        }

        @Test
        void testAddReportListenerToSeveralReports2() {
            testAddReportListenerToSeveralReports(false);
        }

        void testAddReportListenerToSeveralReports(boolean updateDataIfNoListener) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            Holder<Integer> menuUpdaterStartIfNeededCalledTimes = new Holder<>(0);
            Holder<Report> report1H = new Holder<>();
            Holder<Report> report2H = new Holder<>();

            AtomicInteger report1DataChangeNotifications = new AtomicInteger();
            AtomicInteger report2DataChangeNotifications = new AtomicInteger();
            AtomicInteger reportDeleteNotifications = new AtomicInteger();

            Report.ReportListener reportListener = new Report.ReportListener() {

                @Override
                public void onReportDelete(int reportId) {
                    if ((report1H.get() != null && report1H.get().getId() == reportId)
                            || (report2H.get() != null && report2H.get().getId() == reportId)) {
                        reportDeleteNotifications.incrementAndGet();
                    }
                }

                @Override
                public void onReportDataChange(Report r) {
                    if (r != null && r.equals(report1H.get())) {
                        report1DataChangeNotifications.incrementAndGet();
                    }
                    if (r != null && r.equals(report2H.get())) {
                        report2DataChangeNotifications.incrementAndGet();
                    }
                }

            };

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r1) -> {
                    TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r2) -> {
                        report1H.set(r1);
                        report2H.set(r2);
                        try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                            TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                    menuUpdaterStaticMock, menuUpdaterStartIfNeededCalledTimes);

                            rm.addReportListener(r1.getId(), reportListener, updateDataIfNoListener, db, taskScheduler,
                                    um);
                            rm.broadcastReportDataChanged(r1);
                            rm.addReportListener(r2.getId(), reportListener, updateDataIfNoListener, db, taskScheduler,
                                    um);
                            rm.broadcastReportDataChanged(r1);
                            rm.broadcastReportDataChanged(r2);
                            tc.setDone();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT));
            Report r1 = report1H.get();
            assertNotNull(r1);
            assertTrue(rm.isReportListenerListeningToReport(r1.getId(), reportListener));
            assertTrue(rm.hasReportListener(r1.getId()));

            Report r2 = report2H.get();
            assertNotNull(r2);
            assertTrue(rm.isReportListenerListeningToReport(r2.getId(), reportListener));
            assertTrue(rm.hasReportListener(r2.getId()));

            assertNotEquals(r1, r2);

            verify(rm, times(updateDataIfNoListener ? 2 : 0)).updateDataWhenPossible(any(Database.class),
                    any(TaskScheduler.class), any(UsersManager.class));
            assertEquals(updateDataIfNoListener ? 2 : 0, menuUpdaterStartIfNeededCalledTimes.get());

            assertEquals(2, report1DataChangeNotifications.get());
            assertEquals(1, report2DataChangeNotifications.get());
            assertEquals(0, reportDeleteNotifications.get());
        }

    }

    /**
     * Test methods for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#removeReportListener(int, fr.mrtigreroux.tigerreports.objects.reports.Report.ReportListener)}.
     */
    @Nested
    class RemoveReportListener {

        @Test
        void testRemoveReportListener1() {
            testRemoveReportListener(true, false, false);
        }

        @Test
        void testRemoveReportListener2() {
            testRemoveReportListener(false, false, false);
        }

        @Test
        void testRemoveReportListenerTwoTimes1() {
            testRemoveReportListener(true, true, false);
        }

        @Test
        void testRemoveReportListenerTwoTimes2() {
            testRemoveReportListener(false, true, false);
        }

        @Test
        void testRemoveReportListenerTwoTimes3() {
            testRemoveReportListener(false, true, true);
        }

        @Test
        void testRemoveReportListenerTwoTimes4() {
            testRemoveReportListener(true, true, true);
        }

        @Test
        void testRemoveReportListenerTwoTimes5() {
            testRemoveReportListener(true, false, true);
        }

        void testRemoveReportListener(boolean updateDataIfNoListener, boolean addTwoTimes, boolean removeTwoTimes) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            Holder<Integer> menuUpdaterStartIfNeededCalledTimes = new Holder<>(0);
            Holder<Report> reportH = new Holder<>();

            AtomicInteger reportDataChangeNotifications = new AtomicInteger();
            AtomicInteger reportDeleteNotifications = new AtomicInteger();

            Report.ReportListener reportListener = new Report.ReportListener() {

                @Override
                public void onReportDelete(int reportId) {
                    if (reportH.get() != null && reportH.get().getId() == reportId) {
                        reportDeleteNotifications.incrementAndGet();
                    }
                }

                @Override
                public void onReportDataChange(Report r) {
                    if (r != null && r.equals(reportH.get())) {
                        reportDataChangeNotifications.incrementAndGet();
                    }
                }

            };

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r) -> {
                    reportH.set(r);
                    try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                        TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                menuUpdaterStaticMock, menuUpdaterStartIfNeededCalledTimes);

                        rm.addReportListener(r.getId(), reportListener, updateDataIfNoListener, db, taskScheduler, um);
                        if (addTwoTimes) {
                            rm.addReportListener(r.getId(), reportListener, updateDataIfNoListener, db, taskScheduler,
                                    um);
                        }
                        rm.broadcastReportDataChanged(r);
                        rm.removeReportListener(r.getId(), reportListener);
                        if (removeTwoTimes) {
                            rm.removeReportListener(r.getId(), reportListener);
                        }
                        rm.broadcastReportDataChanged(r);
                        tc.setDone();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT));
            Report r = reportH.get();
            assertNotNull(r);
            assertFalse(rm.isReportListenerListeningToReport(r.getId(), reportListener));
            assertFalse(rm.hasReportListener(r.getId()));

            verify(rm, times(updateDataIfNoListener ? 1 : 0)).updateDataWhenPossible(any(Database.class),
                    any(TaskScheduler.class), any(UsersManager.class));
            assertEquals(updateDataIfNoListener ? 1 : 0, menuUpdaterStartIfNeededCalledTimes.get());

            assertEquals(1, reportDataChangeNotifications.get());
            assertEquals(0, reportDeleteNotifications.get());
        }

        @Test
        void testRemoveSeveralReportListeners1() {
            testRemoveSeveralReportListeners(true, 1);
        }

        @Test
        void testRemoveSeveralReportListeners2() {
            testRemoveSeveralReportListeners(false, 1);
        }

        @Test
        void testRemoveSeveralReportListeners3() {
            testRemoveSeveralReportListeners(true, 3);
        }

        void testRemoveSeveralReportListeners(boolean updateDataIfNoListener, int broadcastReportDataChangedAmount) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            Holder<Integer> menuUpdaterStartIfNeededCalledTimes = new Holder<>(0);
            Holder<Report> reportH = new Holder<>();

            List<FakeReportListener> fakeReportListenersToKeep = Arrays.asList(
                    new TestsReportsManager.FakeReportListener(reportH),
                    new TestsReportsManager.FakeReportListener(reportH));

            List<FakeReportListener> fakeReportListenersToRemove = Arrays.asList(
                    new TestsReportsManager.FakeReportListener(reportH),
                    new TestsReportsManager.FakeReportListener(reportH));

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r) -> {
                    reportH.set(r);
                    try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                        TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                menuUpdaterStaticMock, menuUpdaterStartIfNeededCalledTimes);

                        for (FakeReportListener fakeReportListener : fakeReportListenersToKeep) {
                            rm.addReportListener(r.getId(), fakeReportListener.reportListener, updateDataIfNoListener,
                                    db, taskScheduler, um);
                        }
                        for (FakeReportListener fakeReportListener : fakeReportListenersToRemove) {
                            rm.addReportListener(r.getId(), fakeReportListener.reportListener, updateDataIfNoListener,
                                    db, taskScheduler, um);
                        }

                        for (int i = 0; i < broadcastReportDataChangedAmount; i++) {
                            rm.broadcastReportDataChanged(r);
                        }

                        for (FakeReportListener fakeReportListener : fakeReportListenersToRemove) {
                            rm.removeReportListener(r.getId(), fakeReportListener.reportListener);
                        }

                        for (int i = 0; i < broadcastReportDataChangedAmount; i++) {
                            rm.broadcastReportDataChanged(r);
                        }
                        tc.setDone();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT));
            Report r = reportH.get();
            assertNotNull(r);
            assertTrue(rm.hasReportListener(r.getId()));
            verify(rm, times(updateDataIfNoListener ? 1 : 0)).updateDataWhenPossible(any(Database.class),
                    any(TaskScheduler.class), any(UsersManager.class));
            assertEquals(updateDataIfNoListener ? 1 : 0, menuUpdaterStartIfNeededCalledTimes.get());

            for (FakeReportListener fakeReportListener : fakeReportListenersToKeep) {
                assertTrue(rm.isReportListenerListeningToReport(r.getId(), fakeReportListener.reportListener));
                assertEquals(broadcastReportDataChangedAmount * 2,
                        fakeReportListener.reportDataChangeNotifications.get());
                assertEquals(0, fakeReportListener.reportDeleteNotifications.get());
            }

            for (FakeReportListener fakeReportListener : fakeReportListenersToRemove) {
                assertFalse(rm.isReportListenerListeningToReport(r.getId(), fakeReportListener.reportListener));
                assertEquals(broadcastReportDataChangedAmount, fakeReportListener.reportDataChangeNotifications.get());
                assertEquals(0, fakeReportListener.reportDeleteNotifications.get());
            }
        }

        @Test
        void testRemoveReportListenerToSeveralReports1() {
            testRemoveReportListenerToSeveralReports(true);
        }

        @Test
        void testRemoveReportListenerToSeveralReports2() {
            testRemoveReportListenerToSeveralReports(false);
        }

        void testRemoveReportListenerToSeveralReports(boolean updateDataIfNoListener) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            Holder<Integer> menuUpdaterStartIfNeededCalledTimes = new Holder<>(0);
            Holder<Report> report1H = new Holder<>();
            Holder<Report> report2H = new Holder<>();
            Holder<Report> report3H = new Holder<>();

            AtomicInteger report1DataChangeNotifications = new AtomicInteger();
            AtomicInteger report2DataChangeNotifications = new AtomicInteger();
            AtomicInteger report3DataChangeNotifications = new AtomicInteger();
            AtomicInteger reportDeleteNotifications = new AtomicInteger();

            Report.ReportListener reportListener = new Report.ReportListener() {

                @Override
                public void onReportDelete(int reportId) {
                    if ((report1H.get() != null && report1H.get().getId() == reportId)
                            || (report2H.get() != null && report2H.get().getId() == reportId)
                            || (report3H.get() != null && report3H.get().getId() == reportId)) {
                        reportDeleteNotifications.incrementAndGet();
                    }
                }

                @Override
                public void onReportDataChange(Report r) {
                    if (r != null && r.equals(report1H.get())) {
                        report1DataChangeNotifications.incrementAndGet();
                    }
                    if (r != null && r.equals(report2H.get())) {
                        report2DataChangeNotifications.incrementAndGet();
                    }
                    if (r != null && r.equals(report3H.get())) {
                        report3DataChangeNotifications.incrementAndGet();
                    }
                }

            };

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r1) -> {
                    TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r2) -> {
                        TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r3) -> {
                            report1H.set(r1);
                            report2H.set(r2);
                            report3H.set(r3);
                            try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                                TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                        menuUpdaterStaticMock, menuUpdaterStartIfNeededCalledTimes);

                                rm.addReportListener(r1.getId(), reportListener, updateDataIfNoListener, db,
                                        taskScheduler, um);
                                rm.addReportListener(r2.getId(), reportListener, updateDataIfNoListener, db,
                                        taskScheduler, um);
                                rm.addReportListener(r3.getId(), reportListener, updateDataIfNoListener, db,
                                        taskScheduler, um);
                                rm.broadcastReportDataChanged(r1);
                                rm.broadcastReportDataChanged(r2);
                                rm.broadcastReportDataChanged(r3);

                                rm.removeReportListener(r1.getId(), reportListener);
                                rm.removeReportListener(r2.getId(), reportListener);
                                rm.broadcastReportDataChanged(r1);
                                rm.broadcastReportDataChanged(r2);
                                rm.broadcastReportDataChanged(r3);
                                tc.setDone();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT));
            Report r1 = report1H.get();
            assertNotNull(r1);
            assertFalse(rm.isReportListenerListeningToReport(r1.getId(), reportListener));
            assertFalse(rm.hasReportListener(r1.getId()));

            Report r2 = report2H.get();
            assertNotNull(r2);
            assertFalse(rm.isReportListenerListeningToReport(r2.getId(), reportListener));
            assertFalse(rm.hasReportListener(r2.getId()));

            Report r3 = report3H.get();
            assertNotNull(r3);
            assertTrue(rm.isReportListenerListeningToReport(r3.getId(), reportListener));
            assertTrue(rm.hasReportListener(r3.getId()));

            assertNotEquals(r1, r2);
            assertNotEquals(r1, r3);
            assertNotEquals(r2, r3);

            verify(rm, times(updateDataIfNoListener ? 3 : 0)).updateDataWhenPossible(any(Database.class),
                    any(TaskScheduler.class), any(UsersManager.class));
            assertEquals(updateDataIfNoListener ? 3 : 0, menuUpdaterStartIfNeededCalledTimes.get());

            assertEquals(1, report1DataChangeNotifications.get());
            assertEquals(1, report2DataChangeNotifications.get());
            assertEquals(2, report3DataChangeNotifications.get());
            assertEquals(0, reportDeleteNotifications.get());
        }

    }

    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getAndListenReportsPage(fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics, int, boolean, fr.mrtigreroux.tigerreports.objects.reports.ReportsPage.ReportsPageListener, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager)}.
    //	 */
    //	@Test
    //	void testGetAndListenReportsPage() {
    //		fail("Not yet implemented");
    //	}

    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getReportsPage(java.util.UUID, java.util.UUID, boolean, int, boolean)}.
    //	 */
    //	@Test
    //	void testGetReportsPageUUIDUUIDBooleanIntBoolean() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getReportsPage(fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics, int, boolean)}.
    //	 */
    //	@Test
    //	void testGetReportsPageReportsCharacteristicsIntBoolean() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getReportsPage(fr.mrtigreroux.tigerreports.objects.reports.ReportsPageCharacteristics, boolean)}.
    //	 */
    //	@Test
    //	void testGetReportsPageReportsPageCharacteristicsBoolean() {
    //		fail("Not yet implemented");
    //	}

    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#removeReportsPageFromCache(fr.mrtigreroux.tigerreports.objects.reports.ReportsPageCharacteristics)}.
    //	 */
    //	@Test
    //	void testRemoveReportsPage() {
    //		fail("Not yet implemented");
    //	}

    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#addReportCommentsPagesListener(int, int, fr.mrtigreroux.tigerreports.managers.ReportsManager.ReportCommentsPageListener, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager)}.
    //	 */
    //	@Test
    //	void testAddReportCommentsPagesListener() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#removeReportCommentsPagesListener(int, int, fr.mrtigreroux.tigerreports.managers.ReportsManager.ReportCommentsPageListener)}.
    //	 */
    //	@Test
    //	void testRemoveReportCommentsPagesListener() {
    //		fail("Not yet implemented");
    //	}

    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateDataWhenPossible(fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager)}.
    //	 */
    //	@Test
    //	void testUpdateDataWhenPossible() {
    //		fail("Not yet implemented");
    //	}

    /**
     * Test methods for
     * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateData(fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager)}.
     */
    @Nested
    class UpdateData {

        @Nested
        class SingleCall {

            @Test
            void test1() {
                testUpdateDataSingleCall(1);
            }

            @Test
            void test2() {
                testUpdateDataSingleCall(10);
            }

            @Test
            void test3() {
                testUpdateDataSingleCall(28);
            }

            @Test
            void test4() {
                testUpdateDataSingleCall(50);
            }

            @Test
            void test5() {
                testUpdateDataSingleCall(100);
            }

            void testUpdateDataSingleCall(int reportsAmount) {
                TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
                Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

                ReportsManager rm = mock(ReportsManager.class,
                        Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                UsersManager um = mock(UsersManager.class);

                List<Report> reports = new ArrayList<>();
                List<FakeReportListener> fakeReportListeners = new ArrayList<>();

                assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                    TestsReportUtils.createRandomReportsAsynchronously(reportsAmount, taskScheduler, db,
                            (randomReports) -> {
                                reports.addAll(randomReports);
                                for (Report r : reports) {
                                    fakeReportListeners.add(new TestsReportsManager.FakeReportListener(r));
                                }

                                TestsReportUtils.setupUMForReports(um, reports);

                                try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                                    TestsReportsManager
                                            .setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                                    menuUpdaterStaticMock, null);

                                    for (int i = 0; i < reports.size(); i++) {
                                        Report r = reports.get(i);
                                        rm.addReportListener(r.getId(), fakeReportListeners.get(i).reportListener,
                                                false, db, taskScheduler, um); // updateDataIfNoListener set to false to avoid several calls to updateDataWhenPossible
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                when(rm.getTimeBeforeNextDataUpdate()).thenReturn(0L);
                                when(rm.isDataUpdateRequested()).thenAnswer((inv) -> {
                                    tc.setDone(); // isDataUpdateRequested() is called at the end of updateData()
                                    return false;
                                });
                                assertTrue(rm.updateData(db, taskScheduler, um));
                            });
                }, TestsTaskScheduler.DEFAULT_TIMEOUT));

                assertTrue(taskScheduler.waitForTerminationOrStop(TestsTaskScheduler.DEFAULT_TIMEOUT));

                assertEquals(reportsAmount, reports.size());
                assertEquals(reportsAmount, fakeReportListeners.size());

                for (FakeReportListener fakeListener : fakeReportListeners) {
                    assertEquals(1, fakeListener.reportDataChangeNotifications.get()); // notification because report is added in cache
                    assertEquals(0, fakeListener.reportDeleteNotifications.get());
                }

                verify(rm, times(0)).updateDataWhenPossible(db, taskScheduler, um);

                for (Report r : reports) {
                    LOGGER.debug(() -> "check deep equals report id = " + r.getId());
                    AssertionUtils.assertDeepEquals(r, rm.getCachedReportById(r.getId()), "advancedData"); // updateData doesn't collect advancedData
                }
            }

        }

        @Nested
        class WhenNoChange {

            @Test
            void test1() {
                testUpdateDataWhenNoChange(28);
            }

            void testUpdateDataWhenNoChange(int reportsAmount) {
                TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
                Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

                ReportsManager rm = mock(ReportsManager.class,
                        Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                UsersManager um = mock(UsersManager.class);

                List<Report> dbReports = new ArrayList<>();
                List<Report> cachedReportsBefore = new ArrayList<>();
                List<FakeReportListener> fakeReportListeners = new ArrayList<>();

                assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                    TestsReportUtils.createRandomReportsAsynchronously(reportsAmount, taskScheduler, db,
                            (randomReports) -> {
                                dbReports.addAll(randomReports);
                                for (Report r : dbReports) {
                                    fakeReportListeners.add(new TestsReportsManager.FakeReportListener(r));
                                }

                                TestsReportUtils.setupUMForReports(um, dbReports);

                                try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                                    TestsReportsManager
                                            .setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                                                    menuUpdaterStaticMock, null);

                                    for (int i = 0; i < dbReports.size(); i++) {
                                        Report r = dbReports.get(i);
                                        rm.addReportListener(r.getId(), fakeReportListeners.get(i).reportListener,
                                                false, db, taskScheduler, um); // updateDataIfNoListener set to false to avoid several calls to updateDataWhenPossible
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                when(rm.getTimeBeforeNextDataUpdate()).thenReturn(0L);
                                when(rm.isDataUpdateRequested()).thenAnswer((inv) -> { // isDataUpdateRequested() is called at the end of updateData()
                                    LOGGER.debug(() -> "isDataUpdateRequested() 1");
                                    for (Report r : dbReports) {
                                        cachedReportsBefore.add(rm.getCachedReportById(r.getId()));
                                    }

                                    assertTrue(rm.updateData(db, taskScheduler, um));
                                    return false;
                                }).thenAnswer((inv) -> {
                                    LOGGER.debug(() -> "isDataUpdateRequested() 2");
                                    tc.setDone();
                                    return false;
                                });

                                assertTrue(rm.updateData(db, taskScheduler, um)); // first call to save dbReports in cache
                            });
                }, TestsTaskScheduler.DEFAULT_TIMEOUT));

                assertTrue(taskScheduler.waitForTerminationOrStop(TestsTaskScheduler.DEFAULT_TIMEOUT));

                assertEquals(reportsAmount, dbReports.size());
                assertEquals(reportsAmount, fakeReportListeners.size());
                assertEquals(reportsAmount, cachedReportsBefore.size());

                for (FakeReportListener fakeListener : fakeReportListeners) {
                    assertEquals(1, fakeListener.reportDataChangeNotifications.get()); // notification because report is added in cache
                    assertEquals(0, fakeListener.reportDeleteNotifications.get());
                }

                verify(rm, times(0)).updateDataWhenPossible(db, taskScheduler, um);

                for (Report r : dbReports) {
                    LOGGER.debug(() -> "check deep equals report before id = " + r.getId());
                    AssertionUtils.assertDeepEquals(r, rm.getCachedReportById(r.getId()), "advancedData"); // updateData doesn't collect advancedData
                }

                for (Report r : cachedReportsBefore) {
                    assertTrue(r == rm.getCachedReportById(r.getId()));
                }
            }

        }

        @Nested
        class WhenStatusChanges {

            @Test
            void test1() {
                testUpdateDataWhenStatusChanges(28, 10, 25, 1);
            }

            @Test
            void test2() {
                testUpdateDataWhenStatusChanges(50, 10, 45, 3);
            }

            @Test
            void test3() {
                testUpdateDataWhenStatusChanges(100, 30, 90, 10);
            }

            void testUpdateDataWhenStatusChanges(int reportsAmount, int reportsWithListenerAmountAtStart,
                    int reportsWithListenerAmountAtEnd, int actionsBetweenUpdateDataTimes) {
                TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
                Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

                ReportsManager rm = mock(ReportsManager.class,
                        Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                UsersManager um = mock(UsersManager.class);

                int minChangedDataReportsAmount = 5; // min amount of reports which data is changed

                List<TestsReportData> testsReportsData = new ArrayList<>();

                testUpdateData(reportsAmount, (actionsBetweenUpdateDataCurrentTime) -> {
                    RandomUtils.executeActionOnRandomElements(testsReportsData, minChangedDataReportsAmount,
                            reportsAmount, (rData) -> {
                                rData.changeReportStatus(actionsBetweenUpdateDataCurrentTime, db);
                            });
                }, actionsBetweenUpdateDataTimes, testsReportsData,
                        getCurrentAndArchivedFirstReportsPageCharacteristics(), taskScheduler, db, rm, um, () -> {

                        });
            }

        }

        @Nested
        class WhenReportsDeleted {

            @Test
            void test1() {
                testUpdateDataWhenReportsDeleted(28, 10, 25, 1, 5);
            }

            @Test
            void test2() {
                testUpdateDataWhenReportsDeleted(40, 30, 35, 6, 10);
            }

            void testUpdateDataWhenReportsDeleted(int reportsAmount, int reportsWithListenerAmountAtStart,
                    int reportsWithListenerAmountAtEnd, int actionsBetweenUpdateDataTimes, int reportsToDeleteAmount) {
                TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
                Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

                ReportsManager rm = mock(ReportsManager.class,
                        Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                UsersManager um = mock(UsersManager.class);

                BungeeManager bm = mock(BungeeManager.class);
                VaultManager vm = mock(VaultManager.class);

                AtomicInteger currentDeletedReportsAmount = new AtomicInteger(0);

                List<TestsReportData> testsReportsData = new ArrayList<>();
                testUpdateData(reportsAmount, (actionsBetweenUpdateDataCurrentTime) -> {
                    int remainingReportsToDeleteAmount = reportsToDeleteAmount - currentDeletedReportsAmount.get();
                    boolean isLastActionsInterval = actionsBetweenUpdateDataTimes - 1
                            - actionsBetweenUpdateDataCurrentTime <= 0;
                    if (remainingReportsToDeleteAmount > 0) {
                        currentDeletedReportsAmount.addAndGet(RandomUtils.executeActionOnRandomElements(
                                testsReportsData.stream()
                                        .filter((rData) -> !rData.isDbReportDeleted)
                                        .collect(Collectors.toList()),
                                isLastActionsInterval ? remainingReportsToDeleteAmount : 0,
                                remainingReportsToDeleteAmount, (rData) -> {
                                    rData.deleteReport(actionsBetweenUpdateDataCurrentTime, taskScheduler, db, rm, bm,
                                            vm);
                                }));
                    }
                }, actionsBetweenUpdateDataTimes, testsReportsData,
                        getCurrentAndArchivedFirstReportsPageCharacteristics(), taskScheduler, db, rm, um, () -> {

                        });
            }

        }

        @Nested
        class WhenReportsArchived {

            @Test
            void test1() {
                testUpdateDataWhenReportsArchived(28, 10, 25, 1, 5);
            }

            @Test
            void test2() {
                testUpdateDataWhenReportsArchived(40, 30, 35, 6, 10);
            }

            @Test
            void test3() {
                testUpdateDataWhenReportsArchived(50, 1, 5, 6, 20);
            }

            void testUpdateDataWhenReportsArchived(int reportsAmount, int reportsWithListenerAmountAtStart,
                    int reportsWithListenerAmountAtEnd, int actionsBetweenUpdateDataTimes, int reportsToArchiveAmount) {
                TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
                Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

                ReportsManager rm = mock(ReportsManager.class,
                        Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                UsersManager um = mock(UsersManager.class);

                BungeeManager bm = mock(BungeeManager.class);
                VaultManager vm = mock(VaultManager.class);

                AtomicInteger currentArchivedReportsAmount = new AtomicInteger(0);

                List<TestsReportData> testsReportsData = new ArrayList<>();
                testUpdateData(reportsAmount, (actionsBetweenUpdateDataCurrentTime) -> {
                    int remainingReportsToArchiveAmount = reportsToArchiveAmount - currentArchivedReportsAmount.get();
                    boolean isLastActionsInterval = actionsBetweenUpdateDataTimes - 1
                            - actionsBetweenUpdateDataCurrentTime <= 0;
                    if (remainingReportsToArchiveAmount > 0) {
                        currentArchivedReportsAmount.addAndGet(RandomUtils.executeActionOnRandomElements(
                                testsReportsData.stream()
                                        .filter((rData) -> rData.getReportForDatabaseModification() != null
                                                && !rData.getReportForDatabaseModification().isArchived())
                                        .collect(Collectors.toList()),
                                isLastActionsInterval ? remainingReportsToArchiveAmount : 0,
                                remainingReportsToArchiveAmount, (rData) -> {
                                    rData.archiveOrUnarchiveReport(actionsBetweenUpdateDataCurrentTime, taskScheduler,
                                            db, rm, bm, vm);
                                }));
                    }
                }, actionsBetweenUpdateDataTimes, testsReportsData,
                        getCurrentAndArchivedFirstReportsPageCharacteristics(), taskScheduler, db, rm, um, () -> {

                        });
            }

        }

        @Nested
        class WhenRandomChanges {

            @Test
            void test1() {
                testUpdateDataWhenRandomChanges(28, 10, 25, 1, 5, 10);
            }

            @Test
            void test2() {
                testUpdateDataWhenRandomChanges(40, 30, 35, 6, 10, 20);
            }

            @Test
            void test3() {
                testUpdateDataWhenRandomChanges(150, 1, 5, 20, 10, 50);
            }

            void testUpdateDataWhenRandomChanges(int reportsAmount, int reportsWithListenerAmountAtStart,
                    int reportsWithListenerAmountAtEnd, int actionsBetweenUpdateDataTimes,
                    int minReportsToChangePerActionsIntervalAmount, int maxReportsToChangePerActionsIntervalAmount) {
                TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
                Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

                ReportsManager rm = mock(ReportsManager.class,
                        Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                UsersManager um = mock(UsersManager.class);

                BungeeManager bm = mock(BungeeManager.class);
                VaultManager vm = mock(VaultManager.class);
                Random rand = new Random();

                List<TestsReportData> testsReportsData = new ArrayList<>();
                testUpdateData(reportsAmount, (actionsBetweenUpdateDataCurrentTime) -> {
                    RandomUtils.executeActionOnRandomElements(testsReportsData,
                            minReportsToChangePerActionsIntervalAmount, maxReportsToChangePerActionsIntervalAmount,
                            (rData) -> {
                                int randomActionIndex = rand.nextInt(100);

                                if (randomActionIndex < 5) {
                                    rData.deleteReport(actionsBetweenUpdateDataCurrentTime, taskScheduler, db, rm, bm,
                                            vm);
                                } else if (randomActionIndex < 50) {
                                    rData.archiveOrUnarchiveReport(actionsBetweenUpdateDataCurrentTime, taskScheduler,
                                            db, rm, bm, vm);
                                } else {
                                    rData.changeReportStatus(actionsBetweenUpdateDataCurrentTime, db);
                                }
                            });
                }, actionsBetweenUpdateDataTimes, testsReportsData,
                        getCurrentAndArchivedFirstReportsPageCharacteristics(), taskScheduler, db, rm, um, () -> {

                        });
            }

        }

        class TestsReportData {

            Report dbReportBefore;
            Report dbReportAfterDataChange;
            Report cachedReportBefore;
            FakeReportListener fakeReportListener;
            boolean isFakeReportListenerUsed = false;
            int expectedDataChangeNotificationsAmount = 0;
            int expectedDeleteNotificationsAmount = 0;
            int lastModificationActionsBetweenUpdateDataTime = -1;
            boolean isDbReportDeleted = false;

            private TestsReportData(Report dbReportBefore, FakeReportListener fakeReportListener) {
                this.dbReportBefore = Objects.requireNonNull(dbReportBefore);
                this.fakeReportListener = fakeReportListener;
            }

            int getId() {
                return dbReportBefore.getId();
            }

            void changeReportStatus(int actionsBetweenUpdateDataCurrentTime, Database db) {
                if (isDbReportDeleted) {
                    LOGGER.debug(() -> "changeReportStatus(): report id = " + getId() + " is deleted, ignoring call");
                    return;
                }

                LOGGER.debug(() -> "changeReportStatus(): report id = " + getId());
                Report r = getReportForDatabaseModification();
                TestsReport.changeReportStatus(r, db);
                if (cachedReportBefore != null) { // updateData already done before on this report, ie this report is already cached, ie the DataChangeNotification for caching for the first time has already been fired.
                    incrementExpectedDataChangeNotificationsAmount();
                }
                setLastModificationActionsBetweenUpdateDataTime(actionsBetweenUpdateDataCurrentTime);
            }

            void deleteReport(int actionsBetweenUpdateDataCurrentTime, TaskScheduler taskScheduler, Database db,
                    ReportsManager rm, BungeeManager bm, VaultManager vm) {
                if (isDbReportDeleted) {
                    LOGGER.debug(() -> "deleteReport(): report id = " + getId() + " is already deleted, ignoring call");
                    return;
                }

                LOGGER.debug(() -> "deleteReport(): report id = " + getId());
                Report r = getReportForDatabaseModification();
                TestsReport.deleteReport(r, taskScheduler, db, rm, bm, vm);
                if (isFakeReportListenerUsed) {
                    expectedDeleteNotificationsAmount = 1;
                }
                setLastModificationActionsBetweenUpdateDataTime(actionsBetweenUpdateDataCurrentTime);
                isDbReportDeleted = true;
                dbReportAfterDataChange = null;
            }

            void archiveOrUnarchiveReport(int actionsBetweenUpdateDataCurrentTime, TaskScheduler taskScheduler,
                    Database db, ReportsManager rm, BungeeManager bm, VaultManager vm) {
                if (isDbReportDeleted) {
                    LOGGER.debug(
                            () -> "archiveOrUnarchiveReport(): report id = " + getId() + " is deleted, ignoring call");
                    return;
                }

                Report r = getReportForDatabaseModification();
                if (r.isArchived()) {
                    TestsReport.unarchiveReport(r, taskScheduler, db, rm, bm, vm);
                    LOGGER.debug(() -> "archiveOrUnarchiveReport(): report id = " + getId() + " unarchived");
                } else {
                    TestsReport.archiveReport(r, taskScheduler, db, rm, bm, vm);
                    LOGGER.debug(() -> "archiveOrUnarchiveReport(): report id = " + getId() + " archived");
                }

                if (cachedReportBefore != null) {
                    incrementExpectedDataChangeNotificationsAmount();
                }
                setLastModificationActionsBetweenUpdateDataTime(actionsBetweenUpdateDataCurrentTime);
            }

            void incrementExpectedDataChangeNotificationsAmount() {
                if (isFakeReportListenerUsed) {
                    LOGGER.debug(() -> "incrementExpectedDataChangeNotificationsAmount(): for report id = "
                            + dbReportBefore.getId());
                    expectedDataChangeNotificationsAmount++;
                }
            }

            Report getReportForDatabaseModification() {
                if (dbReportAfterDataChange == null && !isDbReportDeleted) {
                    dbReportAfterDataChange = dbReportBefore.deepClone(); // dbReportAfterDataChange is used to change the status of the report in the database without needing to query again the report from the database and without modifying dbReportBefore. But
                                                                          // dbReportAfterDataChange is replaced by the result
                                                                          // of a query to the db at the end of testUpdateData.
                }
                return dbReportAfterDataChange;
            }

            void setLastModificationActionsBetweenUpdateDataTime(int lastModificationActionsBetweenUpdateDataTime) {
                this.lastModificationActionsBetweenUpdateDataTime = lastModificationActionsBetweenUpdateDataTime;
            }

            boolean hasDbReportChanged() {
                return lastModificationActionsBetweenUpdateDataTime >= 0;
            }

        }

        class TestsReportsPageData {

            ReportsPageCharacteristics characteristics;
            ReportsPage reportsPage;
            FakeReportsPageListener fakeListener;
            boolean isFakeListenerUsed = false;
            int expectedPageChangeNotificationsAmount = 0;
            List<Integer> lastReportsId = null;

            public TestsReportsPageData(ReportsPageCharacteristics characteristics) {
                this.characteristics = Objects.requireNonNull(characteristics);
                fakeListener = new TestsReportsManager.FakeReportsPageListener(characteristics.page);
            }

            void incrementExpectedDataChangeNotificationsAmount() {
                LOGGER.debug(() -> "incrementExpectedDataChangeNotificationsAmount(): page " + characteristics);
                expectedPageChangeNotificationsAmount++;
            }

            boolean couldContainReport(Report r) {
                return r != null && characteristics.reportsCharacteristics.archived == r.isArchived()
                        && (characteristics.reportsCharacteristics.reportedUUID == null
                                || characteristics.reportsCharacteristics.reportedUUID.equals(r.getReportedUniqueId()))
                        && (characteristics.reportsCharacteristics.reporterUUID == null
                                || r.getReportersUUID().contains(characteristics.reportsCharacteristics.reporterUUID));
            }

        }

        List<ReportsPageCharacteristics> getCurrentAndArchivedFirstReportsPageCharacteristics() {
            List<ReportsPageCharacteristics> res = new ArrayList<>();
            res.add(new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 1));
            res.add(new ReportsPageCharacteristics(ReportsCharacteristics.ARCHIVED_REPORTS, 1));
            return res;
        }

        void testUpdateData(int reportsAmount, Consumer<Integer> actionsBetweenUpdateData,
                int actionsBetweenUpdateDataTimes, List<TestsReportData> testsReportsData,
                List<ReportsPageCharacteristics> reportsPagesToListen, TestsTaskScheduler taskScheduler, Database db,
                ReportsManager rm, UsersManager um, Runnable callback) {
            testUpdateData(reportsAmount, reportsAmount, reportsAmount, actionsBetweenUpdateData,
                    actionsBetweenUpdateDataTimes, testsReportsData, reportsPagesToListen, taskScheduler, db, rm, um,
                    callback);
        }

        /**
         * Ignores #updateDataWhenPossible. Ignores page change notifications when actions between updateData are executed.
         * 
         * @param reportsAmount
         * @param reportsWithListenerAmountAtStart
         * @param reportsWithListenerAmountAtEnd
         * @param actionsBetweenUpdateData
         * @param actionsBetweenUpdateDataTimes
         * @param testsReportsData
         * @param taskScheduler
         * @param db
         * @param rm
         * @param um
         * @param callback
         */
        void testUpdateData(int reportsAmount, int reportsWithListenerAmountAtStart, int reportsWithListenerAmountAtEnd,
                Consumer<Integer> actionsBetweenUpdateData, int actionsBetweenUpdateDataTimes,
                List<TestsReportData> testsReportsData, List<ReportsPageCharacteristics> reportsPagesToListen,
                TestsTaskScheduler taskScheduler, Database db, ReportsManager rm, UsersManager um, Runnable callback) {
            if (reportsWithListenerAmountAtStart > reportsAmount || reportsWithListenerAmountAtEnd > reportsAmount) {
                throw new IllegalArgumentException("invalid settings");
            }

            AtomicInteger actionsBetweenUpdateDataCurrentTime = new AtomicInteger(0);
            List<TestsReportsPageData> testsReportsPagesData = new ArrayList<>();
            if (reportsPagesToListen != null) {
                for (ReportsPageCharacteristics pCharac : reportsPagesToListen) {
                    testsReportsPagesData.add(new TestsReportsPageData(pCharac));
                }
            }

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportsAsynchronously(reportsAmount, taskScheduler, db,
                        (randomReports) -> {
                            for (Report r : randomReports) {
                                testsReportsData
                                        .add(new TestsReportData(r, new TestsReportsManager.FakeReportListener(r)));
                            }
                            LOGGER.debug(() -> "testUpdateData(): random reports id = " + CollectionUtils
                                    .toString(randomReports.stream().map(r -> r.getId()).collect(Collectors.toList())));

                            TestsReportUtils.setupUMForReports(um, randomReports);

                            doNothing().when(rm)
                                    .updateDataWhenPossible(any(Database.class), any(TaskScheduler.class),
                                            any(UsersManager.class));

                            addReportListenerToRandomReports(taskScheduler, db, rm, um,
                                    reportsWithListenerAmountAtStart, reportsWithListenerAmountAtStart,
                                    testsReportsData);

                            when(rm.getTimeBeforeNextDataUpdate()).thenReturn(0L);
                            when(rm.isDataUpdateRequested()).thenAnswer((inv) -> { // isDataUpdateRequested() is called at the end of updateData()
                                afterUpdateDataOfTestUpdateData(actionsBetweenUpdateDataCurrentTime.getAndIncrement(),
                                        actionsBetweenUpdateDataTimes, actionsBetweenUpdateData, testsReportsData,
                                        reportsWithListenerAmountAtEnd, testsReportsPagesData, taskScheduler, db, rm,
                                        um, tc);
                                return false;
                            });

                            assertTrue(rm.updateData(db, taskScheduler, um)); // first call to save dbReports in cache
                        });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT * 3));
            assertTrue(((TestsSQLite) db).noPendingAsyncUpdateAndNoCallback());

            assertEquals(reportsAmount, testsReportsData.size());

            for (TestsReportData rData : testsReportsData) {
                int reportId = rData.dbReportBefore.getId();
                LOGGER.debug(() -> "testUpdateData(): report id = " + reportId);
                Report cachedReport = rm.getCachedReportById(reportId);

                if (rData.isDbReportDeleted) {
                    assertNull(rData.dbReportAfterDataChange);
                    assertNull(cachedReport);
                }

                if (rData.isFakeReportListenerUsed) {
                    if (!rData.isDbReportDeleted) {
                        LOGGER.debug(() -> "testUpdateData(): check deep equals report before id = " + reportId);
                        AssertionUtils.assertDeepEquals(rData.dbReportAfterDataChange, cachedReport, "advancedData"); // updateData doesn't collect advancedData

                        assertTrue(rData.cachedReportBefore == cachedReport);
                    }

                    assertEquals(rData.expectedDataChangeNotificationsAmount,
                            rData.fakeReportListener.reportDataChangeNotifications.get()); // notification because report is added in cache
                    assertEquals(rData.expectedDeleteNotificationsAmount,
                            rData.fakeReportListener.reportDeleteNotifications.get());
                } else {
                    assertNull(cachedReport);
                    if (!rData.hasDbReportChanged()) {
                        AssertionUtils.assertDeepEquals(rData.dbReportBefore, rData.dbReportAfterDataChange,
                                "advancedData");
                    }

                    assertEquals(0, rData.fakeReportListener.reportDataChangeNotifications.get()); // notification because report is added in cache
                    assertEquals(0, rData.fakeReportListener.reportDeleteNotifications.get());
                }
            }

            pages: for (TestsReportsPageData pData : testsReportsPagesData) {
                LOGGER.debug(
                        () -> "testUpdateData(): checking page " + pData.characteristics + ": " + pData.reportsPage);
                assertNotNull(pData.reportsPage);
                assertEquals(pData.characteristics, pData.reportsPage.characteristics);
                assertTrue(pData.isFakeListenerUsed);

                assertEquals(pData.expectedPageChangeNotificationsAmount,
                        pData.fakeListener.pageChangeNotifications.get(),
                        () -> "unexpected page change notif amount for " + pData.characteristics + ": "
                                + pData.reportsPage);

                List<Integer> pageReportsId = pData.reportsPage.getReportsId();
                int curReportGlobalIndexOfPage = 0;
                int firstGlobalIndexOfPage = ReportsPage.firstGlobalIndexOfPage(pData.characteristics.page);
                int lastGlobalIndexOfPage = ReportsPage.lastGlobalIndexOfPage(pData.characteristics.page);

                boolean ascendingOrder = !pData.characteristics.reportsCharacteristics.archived;
                for (TestsReportData rData : ascendingOrder ? testsReportsData
                        : CollectionUtils.reversedList(testsReportsData)) {
                    if (curReportGlobalIndexOfPage > lastGlobalIndexOfPage) {
                        LOGGER.debug(() -> "testUpdateData(): page " + pData.characteristics
                                + " successfully checked (lastGlobalIndexOfPage = " + lastGlobalIndexOfPage
                                + " passed)");
                        continue pages;
                    }

                    if (rData.isDbReportDeleted) {
                        continue;
                    }

                    LOGGER.debug(() -> "testUpdateData(): checking page " + pData.characteristics + " report id = "
                            + rData.getId());
                    if (pData.couldContainReport(rData.dbReportAfterDataChange)) {
                        if (curReportGlobalIndexOfPage >= firstGlobalIndexOfPage
                                && curReportGlobalIndexOfPage <= lastGlobalIndexOfPage) {
                            int fcurReportGlobalIndexOfPage = curReportGlobalIndexOfPage;
                            assertTrue(pageReportsId.contains(rData.getId()),
                                    () -> "testUpdateData(): reports page " + pData.characteristics
                                            + " (curReportGlobalIndexOfPage = " + fcurReportGlobalIndexOfPage
                                            + ") should contain report " + rData.dbReportAfterDataChange);
                        }

                        curReportGlobalIndexOfPage++;
                    }
                }
            }

            if (testsReportsPagesData.size() == 0) {
                verify(rm, times(0)).updateDataWhenPossible(db, taskScheduler, um);
            } // else updateDataWhenPossible() is effectively called but mocked to do nothing.

            callback.run();
        }

        private void addReportListenerToRandomReports(TestsTaskScheduler taskScheduler, Database db, ReportsManager rm,
                UsersManager um, int minPickedReportsAmount, int maxPickedReportsAmount,
                List<TestsReportData> testsReportsDataWithNoListener) {
            try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                        menuUpdaterStaticMock, null);

                RandomUtils.executeActionOnRandomElements(new ArrayList<>(testsReportsDataWithNoListener),
                        minPickedReportsAmount, maxPickedReportsAmount, (rData) -> {
                            LOGGER.debug(() -> "addReportListenerToRandomReports(): addReportListener to report id = "
                                    + rData.dbReportBefore.getId());
                            rm.addReportListener(rData.dbReportBefore.getId(), rData.fakeReportListener.reportListener,
                                    false, db, taskScheduler, um); // updateDataIfNoListener set to false to avoid several calls to updateDataWhenPossible
                            rData.isFakeReportListenerUsed = true;
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void getAndListenToRandomReportsPages(TestsTaskScheduler taskScheduler, Database db, ReportsManager rm,
                UsersManager um, int minPickedReportsPagesAmount, int maxPickedReportsPagesAmount,
                List<TestsReportsPageData> testsReportsPagesDataWithNoListener) {
            try (MockedStatic<MenuUpdater> menuUpdaterStaticMock = mockStatic(MenuUpdater.class)) {
                TestsReportsManager.setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(rm,
                        menuUpdaterStaticMock, null);

                RandomUtils.executeActionOnRandomElements(new ArrayList<>(testsReportsPagesDataWithNoListener),
                        minPickedReportsPagesAmount, maxPickedReportsPagesAmount, (pData) -> {
                            LOGGER.debug(() -> "addReportListenerToRandomReportsPages(): getAndListen to reports page "
                                    + pData.characteristics);
                            pData.reportsPage = rm.getAndListenReportsPage(pData.characteristics.reportsCharacteristics,
                                    pData.characteristics.page, pData.fakeListener.reportsPageListener, db,
                                    taskScheduler, um);
                            pData.isFakeListenerUsed = true;
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void afterUpdateDataOfTestUpdateData(int actionsBetweenUpdateDataCurrentTime,
                int actionsBetweenUpdateDataTimes, Consumer<Integer> actionsBetweenUpdateData,
                List<TestsReportData> testsReportsData, int reportsWithListenerAmountAtEnd,
                List<TestsReportsPageData> testsReportsPagesData, TestsTaskScheduler taskScheduler, Database db,
                ReportsManager rm, UsersManager um, TaskCompletion tc) {
            int remainingActionsTimes = actionsBetweenUpdateDataTimes - actionsBetweenUpdateDataCurrentTime - 1;

            List<TestsReportData> testsReportsDataWithNoListener = testsReportsData.stream()
                    .filter((rData) -> !rData.isFakeReportListenerUsed)
                    .collect(Collectors.toList());

            int reportsNeedingListenerAmount = reportsWithListenerAmountAtEnd
                    - (testsReportsData.size() - testsReportsDataWithNoListener.size());
            int reportsNeedingListenerNowAmount = remainingActionsTimes > 0
                    ? reportsNeedingListenerAmount / remainingActionsTimes
                    : reportsNeedingListenerAmount;
            LOGGER.debug(
                    () -> "afterUpdateDataOfTestUpdateData(): addReportListenerToRandomReports() with reportsNeedingListenerAmount = "
                            + reportsNeedingListenerAmount + "\nreportsNeedingListenerNowAmount = "
                            + reportsNeedingListenerNowAmount + "\nremainingActionsTimes = " + remainingActionsTimes);
            addReportListenerToRandomReports(taskScheduler, db, rm, um, reportsNeedingListenerNowAmount,
                    reportsNeedingListenerAmount, testsReportsDataWithNoListener);

            for (TestsReportData rData : testsReportsData) {
                if (rData.cachedReportBefore == null) {
                    rData.cachedReportBefore = rm.getCachedReportById(rData.dbReportBefore.getId());
                    if (rData.cachedReportBefore != null) { // the first updateData concerning this report was made just before, therefore a DataChangeNotification was sent when this report was cached for the first time.
                        rData.incrementExpectedDataChangeNotificationsAmount();
                    }
                }
            }

            pages: for (TestsReportsPageData pData : testsReportsPagesData) {
                if (pData.isFakeListenerUsed) {
                    List<Integer> pageReportsId = pData.reportsPage.getReportsId();
                    List<Integer> curLastReportsId = pData.lastReportsId != null ? new ArrayList<>(pData.lastReportsId)
                            : null;
                    pData.lastReportsId = new ArrayList<>(pageReportsId);
                    if (curLastReportsId == null && !pageReportsId.isEmpty()) {
                        LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): reports page " + pData.characteristics
                                + " is collected from db for the first time since it is listened");
                        pData.incrementExpectedDataChangeNotificationsAmount();
                        continue;
                    }

                    if (curLastReportsId != null && !curLastReportsId.equals(pageReportsId)) {
                        LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): reports page " + pData.characteristics
                                + " has not the same reports than at last time (reports have been removed/added or it's the first collect of the page since it is listened)");
                        pData.incrementExpectedDataChangeNotificationsAmount();
                        continue;
                    }

                    for (TestsReportData rData : testsReportsData) {
                        LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): checking for page "
                                + pData.characteristics + " report id = " + rData.getId());
                        if (pageReportsId.contains(rData.getId())) {
                            if (rData.lastModificationActionsBetweenUpdateDataTime == actionsBetweenUpdateDataCurrentTime
                                    - 1) {
                                pData.incrementExpectedDataChangeNotificationsAmount();
                                LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): found report of "
                                        + pData.characteristics
                                        + " modified in last updateData, checking other pages...");
                                continue pages;
                            }
                        }
                    }
                }
            }

            List<TestsReportsPageData> testsReportsPagesWithNoListener = testsReportsPagesData.stream()
                    .filter((pData) -> !pData.isFakeListenerUsed)
                    .collect(Collectors.toList());
            if (testsReportsPagesWithNoListener.size() > 0) {
                int minReportsPagesToPick = remainingActionsTimes < 2 ? testsReportsPagesWithNoListener.size()
                        : remainingActionsTimes < 4 ? 1 : 0;
                getAndListenToRandomReportsPages(taskScheduler, db, rm, um, minReportsPagesToPick,
                        testsReportsPagesWithNoListener.size(), testsReportsPagesWithNoListener);
            }

            if (actionsBetweenUpdateDataCurrentTime < actionsBetweenUpdateDataTimes) {
                setReportsPagesListenersListening(testsReportsPagesData, false);
                LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): execute actionsBetweenUpdateData...");
                actionsBetweenUpdateData.accept(actionsBetweenUpdateDataCurrentTime);
                ((TestsSQLite) db).whenNoAsyncUpdate(() -> { // ensure all db.updateAsync in actionsBetweenUpdateData are executed before continuing
                    LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): actionsBetweenUpdateData executed");
                    setReportsPagesListenersListening(testsReportsPagesData, true);

                    assertTrue(rm.updateData(db, taskScheduler, um));
                });
            } else {
                LOGGER.debug(() -> "afterUpdateDataOfTestUpdateData(): collect reports from db and finish");
                Set<Integer> reportsId = testsReportsData.stream()
                        .map((rData) -> rData.getId())
                        .collect(Collectors.toSet());

                // Use an independent rm to don't affect the reports in the cache of the rm used in tests.
                //				ReportsManager independentRm = mock(ReportsManager.class,
                //						Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
                ReportsManager independentRm = new ReportsManager();
                independentRm.getReportsByIdAsynchronously(reportsId, false, db, taskScheduler, um, (dbReports) -> {
                    if (dbReports != null) {
                        testsReports: for (TestsReportData rData : testsReportsData) {
                            int reportId = rData.getId();
                            for (Report r : dbReports) {
                                if (reportId == r.getId()) {
                                    rData.dbReportAfterDataChange = r;
                                    continue testsReports;
                                }
                            }
                            rData.dbReportAfterDataChange = null; // report doesn't exists (anymore) in db
                        }
                    } else {
                        for (TestsReportData rData : testsReportsData) {
                            rData.dbReportAfterDataChange = null;
                        }
                    }
                    tc.setDone();
                });
            }
        }

        private void setReportsPagesListenersListening(List<TestsReportsPageData> testsReportsPagesData,
                boolean listeningState) {
            LOGGER.debug(() -> "setReportsPagesListenersListening(): listening set to " + listeningState);
            for (TestsReportsPageData testsReportsPage : testsReportsPagesData) {
                testsReportsPage.fakeListener.setListening(listeningState);
            }
        }

    }

    @Nested
    class SwitchBetweenPages {

        @Test
        void testSwitchBetween2Pages1() {
            testSwitchBetween2Pages(ReportsPage.PAGE_MAX_REPORTS_AMOUNT * 2);
        }

        @Test
        void testSwitchBetween2Pages2() {
            testSwitchBetween2Pages((int) (ReportsPage.PAGE_MAX_REPORTS_AMOUNT * 1.5));
        }

        @Test
        void testSwitchBetween2Pages3() {
            testSwitchBetween2Pages(ReportsPage.PAGE_MAX_REPORTS_AMOUNT + 1);
        }

        @Test
        void testSwitchBetween2Pages4() {
            testSwitchBetween2Pages(ReportsPage.PAGE_MAX_REPORTS_AMOUNT);
        }

        @Test
        void testSwitchBetween2Pages5() {
            testSwitchBetween2Pages(ReportsPage.PAGE_MAX_REPORTS_AMOUNT - 1);
        }

        @Test
        void testSwitchBetween2Pages6() {
            testSwitchBetween2Pages(1);
        }

        @Test
        void testSwitchBetween2Pages7() {
            testSwitchBetween2Pages(0);
        }

        @Test
        void testSwitchBetween2PagesWithoutWaiting1() {
            testSwitchBetween2PagesWithoutWaiting(ReportsPage.PAGE_MAX_REPORTS_AMOUNT * 2);
        }

        @Test
        void testSwitchBetween2PagesWithoutWaiting2() {
            testSwitchBetween2PagesWithoutWaiting((int) (ReportsPage.PAGE_MAX_REPORTS_AMOUNT * 1.5));
        }

        class TestsReportsPageListener {

            private Integer expectedNextPageChangeNotifPage = null;
            private Runnable nextActionAfterPageChangeNotif = null;

            final ReportsPage.ReportsPageListener reportsPageListener = new ReportsPage.ReportsPageListener() {

                @Override
                public void onReportsPageChange(int page) {
                    LOGGER.debug(() -> "TestsReportsPageListener.onReportsPageChange(): page = " + page
                            + ", expected = " + expectedNextPageChangeNotifPage);
                    assertEquals(expectedNextPageChangeNotifPage, page);
                    nextActionAfterPageChangeNotif.run();
                }

            };

            void setExpectedNextPageChangeNotif(Integer nextPage, Runnable nextAction) {
                expectedNextPageChangeNotifPage = nextPage;
                nextActionAfterPageChangeNotif = nextAction;
            }

        }

        class TestsReportsPage {

            final ReportsPageCharacteristics characteristics;
            ReportsPage reportsPage = null;
            List<Report> expectedPageReports = new ArrayList<>();

            public TestsReportsPage(ReportsPageCharacteristics characteristics) {
                this.characteristics = characteristics;
            }

            void removeListener(TestsReportsPageListener testsReportsPageListener, ReportsManager rm) {
                reportsPage.removeListener(testsReportsPageListener.reportsPageListener, rm);
            }

            /**
             * NB: Reports of the page must not be modified (data change or deleted) when using this method, because that changes are not expected.
             * 
             * @param testsReportsPageListener
             * @param rm
             * @param db
             * @param taskScheduler
             * @param um
             * @param callback
             */
            void addListenerAndCheckPageAfterChangeNotif(TestsReportsPageListener testsReportsPageListener,
                    ReportsManager rm, Database db, TaskScheduler taskScheduler, UsersManager um, Runnable callback) {
                boolean expectingAPageChangeNotif = expectedPageReports.size() > 0; // There wont be any page change notif if the page has no report
                if (expectingAPageChangeNotif) {
                    testsReportsPageListener.setExpectedNextPageChangeNotif(characteristics.page, () -> {
                        checkPageCachedReports(rm);
                        callback.run();
                    });
                }

                addListener(testsReportsPageListener, rm, db, taskScheduler, um);

                // addListener updates reportsPage, which can then be used to check if it is already cached or not.
                expectingAPageChangeNotif &= expectedPageReports.size() != rm.getReportsPageCachedReports(reportsPage)
                        .size(); // There wont be any page change notif if the page and its reports are already cached (reports are not modified in these tests).

                if (!expectingAPageChangeNotif) {
                    testsReportsPageListener.setExpectedNextPageChangeNotif(null, null);
                    checkPageCachedReports(rm);
                    callback.run();
                }
            }

            void addListener(TestsReportsPageListener testsReportsPageListener, ReportsManager rm, Database db,
                    TaskScheduler taskScheduler, UsersManager um) {
                reportsPage = rm.getAndListenReportsPage(characteristics.reportsCharacteristics, characteristics.page,
                        testsReportsPageListener.reportsPageListener, db, taskScheduler, um);
            }

            private void checkPageCachedReports(ReportsManager rm) {
                List<Report> reportsPageCachedReports = rm.getReportsPageCachedReports(reportsPage);
                LOGGER.debug(() -> "addListenerAndCheckPageAfterChangeNotif(): expected = "
                        + CollectionUtils.toString(expectedPageReports) + ", actual = "
                        + CollectionUtils.toString(reportsPageCachedReports));
                AssertionUtils.assertDeepListEquals(expectedPageReports, reportsPageCachedReports, "advancedData");
            }
        }

        void testSwitchBetween2Pages(int reportsAmount) {
            // NB: MenuUpdater is never executed (started and stopped but no execution) because of the default interval of 10 seconds.
            // NB: Sometimes (depending on how busy the executing computer is) some tasks can remain being executed after the test is considered as finished (tc.setDone()).
            // If it's too difficult to find/fix what task is still being executed, the remaining tasks could be interrupted manually before tc.setDone() with for example the cleanMainTaskSchedulerAfterUse method to avoid the fail in TestClass.afterTest.
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            List<Report> reportsBefore = new ArrayList<>(); // clone of reports before any operation, because the instances could be different depending on the cache system, memory...

            TestsReportsPageListener testsReportsPageListener = new TestsReportsPageListener();

            TestsReportsPage testsReportsPage1 = new TestsReportsPage(
                    new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 1));
            TestsReportsPage testsReportsPage2 = new TestsReportsPage(
                    new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 2));

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportsAsynchronously(reportsAmount, taskScheduler, db,
                        (randomReports) -> {
                            int i = 0;
                            for (Report r : randomReports) {
                                if (ReportsPage.isGlobalIndexInPage(i, testsReportsPage1.characteristics.page)) {
                                    testsReportsPage1.expectedPageReports.add(r.deepClone());
                                } else if (ReportsPage.isGlobalIndexInPage(i, testsReportsPage2.characteristics.page)) {
                                    testsReportsPage2.expectedPageReports.add(r.deepClone());
                                }
                                i++;
                                reportsBefore.add(r.deepClone());
                            }

                            LOGGER.debug(() -> "testSwitchBetween2Pages(): random reports id = "
                                    + CollectionUtils.toString(
                                            randomReports.stream().map(r -> r.getId()).collect(Collectors.toList()))
                                    + "\n expectedPage1Reports id = "
                                    + CollectionUtils.toString(testsReportsPage1.expectedPageReports.stream()
                                            .map(r -> r.getId())
                                            .collect(Collectors.toList()))
                                    + "\n expectedPage2Reports id = "
                                    + CollectionUtils.toString(testsReportsPage2.expectedPageReports.stream()
                                            .map(r -> r.getId())
                                            .collect(Collectors.toList())));

                            TestsReportUtils.setupUMForReports(um, randomReports);

                            LOGGER.debug(() -> "testSwitchBetween2Pages(): 1");
                            testsReportsPage1.addListenerAndCheckPageAfterChangeNotif(testsReportsPageListener, rm, db,
                                    taskScheduler, um, () -> {
                                        testsReportsPage1.removeListener(testsReportsPageListener, rm);

                                        LOGGER.debug(() -> "testSwitchBetween2Pages(): 2");
                                        testsReportsPage2.addListenerAndCheckPageAfterChangeNotif(
                                                testsReportsPageListener, rm, db, taskScheduler, um, () -> {
                                                    testsReportsPage2.removeListener(testsReportsPageListener, rm);

                                                    LOGGER.debug(() -> "testSwitchBetween2Pages(): 3");
                                                    testsReportsPage1.addListenerAndCheckPageAfterChangeNotif(
                                                            testsReportsPageListener, rm, db, taskScheduler, um, () -> {
                                                                testsReportsPage1
                                                                        .removeListener(testsReportsPageListener, rm);

                                                                LOGGER.debug(() -> "testSwitchBetween2Pages(): 4");
                                                                testsReportsPage2
                                                                        .addListenerAndCheckPageAfterChangeNotif(
                                                                                testsReportsPageListener, rm, db,
                                                                                taskScheduler, um, () -> {
                                                                                    LOGGER.debug(
                                                                                            () -> "testSwitchBetween2Pages(): 5");
                                                                                    MenuUpdater.stop(taskScheduler);
                                                                                    TestsReportsManager
                                                                                            .whenReportsManagerIsNotUpdatingData(
                                                                                                    rm, taskScheduler,
                                                                                                    1000L, () -> {
                                                                                                        tc.setDone();
                                                                                                    });
                                                                                });
                                                            });
                                                });
                                    });

                        });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT * 2));
        }

        void testSwitchBetween2PagesWithoutWaiting(int reportsAmount) {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            List<Report> reportsBefore = new ArrayList<>(); // clone of reports before any operation, because the instances could be different depending on the cache system, memory...

            TestsReportsPageListener testsReportsPageListener = new TestsReportsPageListener();

            TestsReportsPage testsReportsPage1 = new TestsReportsPage(
                    new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 1));
            TestsReportsPage testsReportsPage2 = new TestsReportsPage(
                    new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 2));

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportsAsynchronously(reportsAmount, taskScheduler, db,
                        (randomReports) -> {
                            int i = 0;
                            for (Report r : randomReports) {
                                if (ReportsPage.isGlobalIndexInPage(i, testsReportsPage1.characteristics.page)) {
                                    testsReportsPage1.expectedPageReports.add(r.deepClone());
                                } else if (ReportsPage.isGlobalIndexInPage(i, testsReportsPage2.characteristics.page)) {
                                    testsReportsPage2.expectedPageReports.add(r.deepClone());
                                }
                                i++;
                                reportsBefore.add(r.deepClone());
                            }

                            LOGGER.debug(() -> "testSwitchBetween2Pages(): random reports id = "
                                    + CollectionUtils.toString(
                                            randomReports.stream().map(r -> r.getId()).collect(Collectors.toList()))
                                    + "\n expectedPage1Reports id = "
                                    + CollectionUtils.toString(testsReportsPage1.expectedPageReports.stream()
                                            .map(r -> r.getId())
                                            .collect(Collectors.toList()))
                                    + "\n expectedPage2Reports id = "
                                    + CollectionUtils.toString(testsReportsPage2.expectedPageReports.stream()
                                            .map(r -> r.getId())
                                            .collect(Collectors.toList())));

                            TestsReportUtils.setupUMForReports(um, randomReports);

                            when(rm.isDataUpdateRequested()).thenAnswer((inv) -> {
                                LOGGER.debug(() -> "testSwitchBetween2PagesWithoutWaiting(): 3");
                                testsReportsPage1.addListenerAndCheckPageAfterChangeNotif(testsReportsPageListener, rm,
                                        db, taskScheduler, um, () -> {
                                            testsReportsPage1.removeListener(testsReportsPageListener, rm);

                                            testsReportsPage2.addListenerAndCheckPageAfterChangeNotif(
                                                    testsReportsPageListener, rm, db, taskScheduler, um, () -> {
                                                        testsReportsPage2.removeListener(testsReportsPageListener, rm);

                                                        testsReportsPage1.addListenerAndCheckPageAfterChangeNotif(
                                                                testsReportsPageListener, rm, db, taskScheduler, um,
                                                                () -> {
                                                                    testsReportsPage1.removeListener(
                                                                            testsReportsPageListener, rm);

                                                                    testsReportsPage2
                                                                            .addListenerAndCheckPageAfterChangeNotif(
                                                                                    testsReportsPageListener, rm, db,
                                                                                    taskScheduler, um, () -> {
                                                                                        MenuUpdater.stop(taskScheduler);
                                                                                        TestsReportsManager
                                                                                                .whenReportsManagerIsNotUpdatingData(
                                                                                                        rm,
                                                                                                        taskScheduler,
                                                                                                        1000L, () -> {
                                                                                                            tc.setDone();
                                                                                                        });
                                                                                    });
                                                                });
                                                    });
                                        });

                                return false;
                            }).thenCallRealMethod();

                            LOGGER.debug(() -> "testSwitchBetween2PagesWithoutWaiting(): 1");
                            testsReportsPage1.addListener(testsReportsPageListener, rm, db, taskScheduler, um);
                            testsReportsPage1.removeListener(testsReportsPageListener, rm);
                            testsReportsPage2.addListener(testsReportsPageListener, rm, db, taskScheduler, um);
                            testsReportsPage2.removeListener(testsReportsPageListener, rm);
                            testsReportsPage1.addListener(testsReportsPageListener, rm, db, taskScheduler, um);
                            testsReportsPage1.removeListener(testsReportsPageListener, rm);
                            testsReportsPage2.addListener(testsReportsPageListener, rm, db, taskScheduler, um);
                            testsReportsPage2.removeListener(testsReportsPageListener, rm);

                            // Because the listener is directly removed, testsReportsPageListener wont receive any page change notification (because collecting reports takes time and the listener is removed before the reports
                            // are collected and notifications sent)

                            LOGGER.debug(() -> "testSwitchBetween2PagesWithoutWaiting(): 2");
                        });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT * 2));
        }

        @Test
        void testSwitchBetween2PagesWithAnUpdateDataCall() {
            TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
            Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

            int reportsAmount = ReportsPage.PAGE_MAX_REPORTS_AMOUNT * 2;

            ReportsManager rm = mock(ReportsManager.class,
                    Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
            UsersManager um = mock(UsersManager.class);

            List<Report> reportsBefore = new ArrayList<>(); // clone of reports before any operation, because the instances could be different depending on the cache system, memory...

            TestsReportsPageListener testsReportsPageListener = new TestsReportsPageListener();

            TestsReportsPage testsReportsPage1 = new TestsReportsPage(
                    new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 1));
            TestsReportsPage testsReportsPage2 = new TestsReportsPage(
                    new ReportsPageCharacteristics(ReportsCharacteristics.CURRENT_REPORTS, 2));

            assertTrue(taskScheduler.runTaskAndWait((tc) -> {
                TestsReportUtils.createRandomReportsAsynchronously(reportsAmount, taskScheduler, db,
                        (randomReports) -> {
                            int i = 0;
                            for (Report r : randomReports) {
                                if (ReportsPage.isGlobalIndexInPage(i, testsReportsPage1.characteristics.page)) {
                                    testsReportsPage1.expectedPageReports.add(r.deepClone());
                                } else if (ReportsPage.isGlobalIndexInPage(i, testsReportsPage2.characteristics.page)) {
                                    testsReportsPage2.expectedPageReports.add(r.deepClone());
                                }
                                i++;
                                reportsBefore.add(r.deepClone());
                            }

                            LOGGER.debug(() -> "testSwitchBetween2PagesWithAnUpdateDataCall(): random reports id = "
                                    + CollectionUtils.toString(
                                            randomReports.stream().map(r -> r.getId()).collect(Collectors.toList()))
                                    + "\n expectedPage1Reports id = "
                                    + CollectionUtils.toString(testsReportsPage1.expectedPageReports.stream()
                                            .map(r -> r.getId())
                                            .collect(Collectors.toList()))
                                    + "\n expectedPage2Reports id = "
                                    + CollectionUtils.toString(testsReportsPage2.expectedPageReports.stream()
                                            .map(r -> r.getId())
                                            .collect(Collectors.toList())));

                            TestsReportUtils.setupUMForReports(um, randomReports);

                            testsReportsPage1.addListenerAndCheckPageAfterChangeNotif(testsReportsPageListener, rm, db,
                                    taskScheduler, um, () -> {
                                        LOGGER.debug(() -> "testSwitchBetweenPages2(): 1");
                                        when(rm.getTimeBeforeNextDataUpdate()).thenReturn(0L).thenCallRealMethod();
                                        when(rm.isDataUpdateRequested()).thenAnswer((inv) -> { // isDataUpdateRequested() is called at the end of updateData()
                                            testsReportsPage1.removeListener(testsReportsPageListener, rm);
                                            LOGGER.debug(() -> "testSwitchBetweenPages2(): 2");

                                            testsReportsPage2.addListenerAndCheckPageAfterChangeNotif(
                                                    testsReportsPageListener, rm, db, taskScheduler, um, () -> {
                                                        LOGGER.debug(() -> "testSwitchBetweenPages2(): 3");
                                                        testsReportsPage2.removeListener(testsReportsPageListener, rm);

                                                        testsReportsPage1.addListenerAndCheckPageAfterChangeNotif(
                                                                testsReportsPageListener, rm, db, taskScheduler, um,
                                                                () -> {
                                                                    LOGGER.debug(() -> "testSwitchBetweenPages2(): 4");
                                                                    testsReportsPage1.removeListener(
                                                                            testsReportsPageListener, rm);

                                                                    testsReportsPage2
                                                                            .addListenerAndCheckPageAfterChangeNotif(
                                                                                    testsReportsPageListener, rm, db,
                                                                                    taskScheduler, um, () -> {
                                                                                        MenuUpdater.stop(taskScheduler);
                                                                                        TestsReportsManager
                                                                                                .whenReportsManagerIsNotUpdatingData(
                                                                                                        rm,
                                                                                                        taskScheduler,
                                                                                                        1000L, () -> {
                                                                                                            tc.setDone();
                                                                                                        });
                                                                                    });
                                                                });
                                                    });
                                            return false;
                                        }).thenCallRealMethod();

                                        assertTrue(rm.updateData(db, taskScheduler, um));
                                    });

                        });
            }, TestsTaskScheduler.DEFAULT_TIMEOUT * 2));
        }

    }

    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#reportIsDeleted(int)}.
    //	 */
    //	@Test
    //	void testReportIsDeleted() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#commentIsDeleted(fr.mrtigreroux.tigerreports.objects.Comment)}.
    //	 */
    //	@Test
    //	void testCommentIsDeleted() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getReportByIdAsynchronously(int, boolean, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testGetReportByIdAsynchronously() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateReports(java.util.List, int, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testUpdateReportsListOfMapOfStringObjectIntDatabaseTaskSchedulerUsersManagerResultCallbackOfBoolean() {
    //		fail("Not yet implemented");
    //	}

    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateReports(java.util.List, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testUpdateReportsListOfMapOfStringObjectDatabaseTaskSchedulerUsersManagerResultCallbackOfListOfReport() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateAndGetReport(java.util.Map, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testUpdateAndGetReportMapOfStringObjectBooleanBooleanDatabaseTaskSchedulerUsersManagerResultCallbackOfReport() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateAndGetReport(int, fr.mrtigreroux.tigerreports.data.database.QueryResult, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testUpdateAndGetReportIntQueryResultBooleanBooleanDatabaseTaskSchedulerUsersManagerResultCallbackOfReport() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateAndGetReport(int, java.util.Map, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testUpdateAndGetReportIntMapOfStringObjectBooleanBooleanDatabaseTaskSchedulerUsersManagerResultCallbackOfReport() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#updateAndGetReport(int, java.util.Map, boolean, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
    //	 */
    //	@Test
    //	void testUpdateAndGetReportIntMapOfStringObjectBooleanBooleanBooleanDatabaseTaskSchedulerUsersManagerResultCallbackOfReport() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getReportsPageCachedReports(fr.mrtigreroux.tigerreports.objects.reports.ReportsPage)}.
    //	 */
    //	@Test
    //	void testGetReportsPageCachedReports() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getCachedReportComments(int, boolean)}.
    //	 */
    //	@Test
    //	void testGetCachedReportComments() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#getReportCommentsCachedPageComments(int, int)}.
    //	 */
    //	@Test
    //	void testGetReportCommentsCachedPageComments() {
    //		fail("Not yet implemented");
    //	}
    //
    //	/**
    //	 * Test method for
    //	 * {@link fr.mrtigreroux.tigerreports.managers.ReportsManager#fillInventoryWithReportsPage(org.bukkit.inventory.Inventory, fr.mrtigreroux.tigerreports.objects.reports.ReportsPage, java.lang.String, boolean, java.lang.String, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.managers.BungeeManager)}.
    //	 */
    //	@Test
    //	void testFillInventoryWithReportsPage() {
    //		fail("Not yet implemented");
    //	}

}
