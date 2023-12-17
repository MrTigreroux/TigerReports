package fr.mrtigreroux.tigerreports.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.TestClass;
import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.Holder;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.TestsDatabaseManager;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.Report.AppreciationDetails;
import fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails;
import fr.mrtigreroux.tigerreports.objects.reports.TestsReport;
import fr.mrtigreroux.tigerreports.objects.users.OfflineUserData;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TaskCompletion;
import fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler;
import fr.mrtigreroux.tigerreports.utils.ReportUtils.CreatedReport;

/**
 * @author MrTigreroux
 */
class ReportUtilsTest extends TestClass {

	/**
	 * Test method for
	 * {@link fr.mrtigreroux.tigerreports.utils.ReportUtils#stackReportAsynchronously(java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.managers.ReportsManager, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
	 */
	@Nested
	class StackReportAsynchronously {

		@Nested
		class OneReporter {

			@Test
			void test1() {
				testStackReportAsynchronouslyOnExistingReport(true, true, true, false);
			}

			@Test
			void test2() {
				testStackReportAsynchronouslyOnExistingReport(true, true, true, true);
			}

			@Test
			void test3() {
				testStackReportAsynchronouslyOnExistingReport(true, true, false, true);
			}

			@Test
			void test4() {
				testStackReportAsynchronouslyOnExistingReport(false, true, true, true);
			}

			@Test
			void test5() {
				testStackReportAsynchronouslyOnExistingReport(false, false, true, true);
			}

			@Test
			void test6() {
				testStackReportAsynchronouslyOnExistingReport(false, false, false, true);
			}

			@Test
			void test7() {
				testStackReportAsynchronouslyOnExistingReport(true, false, true, false);
			}

		}

		@Nested
		class SeveralReporters {

			@Test
			void test1() {
				testStackReportAsynchronouslyOnExistingReport(true, true, 5, false);
			}

			@Test
			void test2() {
				testStackReportAsynchronouslyOnExistingReport(true, true, 5, true);
			}

			@Test
			void test3() {
				testStackReportAsynchronouslyOnExistingReport(true, true, 5, true);
			}

			@Test
			void test4() {
				testStackReportAsynchronouslyOnExistingReport(false, true, 5, true);
			}

			@Test
			void test5() {
				testStackReportAsynchronouslyOnExistingReport(false, false, 5, true);
			}

			@Test
			void test6() {
				testStackReportAsynchronouslyOnExistingReport(false, false, 5, true);
			}

			@Test
			void test7() {
				testStackReportAsynchronouslyOnExistingReport(true, false, 5, false);
			}

		}

		void testStackReportAsynchronouslyOnExistingReport(boolean reportedOnline, boolean reporterOnline,
		        boolean newReporter, boolean updateDate) {
			testStackReportAsynchronouslyOnExistingReport(reportedOnline, reporterOnline, 1, updateDate);
		}

		void testStackReportAsynchronouslyOnExistingReport(boolean reportedOnline, boolean reporterOnline,
		        int newReportersAmount, boolean updateDate) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);
			testStackReportAsynchronouslyOnExistingReport(reportedOnline, reporterOnline, newReportersAmount,
			        updateDate, taskScheduler, db);
		}

		void testStackReportAsynchronouslyOnExistingReport(boolean reportedOnline, boolean reporterOnline,
		        int newReportersAmount, boolean updateDate, TestsTaskScheduler taskScheduler, Database db) {
			User reported = mock(User.class);
			TestsReportUtils.setupRandomUserMockForCreateReportAsynchronously(reported,
			        reportedOnline ? mock(Player.class) : null);

			User reporter = mock(User.class);
			TestsReportUtils.setupRandomUserMockForCreateReportAsynchronously(reporter,
			        reporterOnline ? mock(Player.class) : null);
			List<User> createReportReporters = Arrays.asList(reporter);

			LOGGER.info(() -> "reporter = " + reporter.getUniqueId() + "\n reported = " + reported.getUniqueId());
			String reason = "report reason";
			String createReportDate = DatetimeUtils.getNowDatetime();

			BungeeManager bm = mock(BungeeManager.class);
			when(bm.getServerName()).thenReturn("bungeeServerName");

			Map<String, Object> reportAdvancedData = new HashMap<>();
			reportAdvancedData.put(Report.AdvancedData.REPORTER_IP, reporter.getIPAddress());
			reportAdvancedData.put(Report.AdvancedData.REPORTER_LOCATION,
			        reporterOnline ? SerializationUtils.serializeLocation(reporter.getPlayer().getLocation(), bm) : null);
			reportAdvancedData.put(Report.AdvancedData.REPORTER_MESSAGES,
			        Report.AdvancedData.serializeMessages(reporter.getLastMessages()));

			Map<String, Object> reportedAdvancedData = new HashMap<>();
			reportedAdvancedData.put(Report.AdvancedData.REPORTED_IP, reported.getIPAddress());
			reportedAdvancedData.put(Report.AdvancedData.REPORTED_LOCATION,
			        reportedOnline ? SerializationUtils.serializeLocation(reported.getPlayer().getLocation(), bm) : null);
			reportedAdvancedData.put(Report.AdvancedData.REPORTED_MESSAGES,
			        Report.AdvancedData.serializeMessages(reported.getLastMessages()));
			if (reportedOnline) {
				TestsReport.fillMissingWithRandomReportedAdvancedData(reportedAdvancedData, bm);
			}

			reportAdvancedData.putAll(reportedAdvancedData);

			UsersManager um = mock(UsersManager.class);
			ReportsManager rm = new ReportsManager();

			boolean newReporter = newReportersAmount > 0;

			List<User> reporters = new ArrayList<>(createReportReporters);
			String stackReportDate = DatetimeUtils.getRelativeDatetime(5);
			if (newReporter) {
				for (int i = 0; i < newReportersAmount; i++) {
					reporters.add(new User(UUID.randomUUID(), "stackReportReporter" + i + " display name",
					        new OfflineUserData("stackReportReporter" + i + " name")));
				}
			}

			TestsReport.setupUMForAsynchronouslyFrom(um, reported, createReportReporters);

			Holder<CreatedReport> crResult = new Holder<>();
			Holder<Report> dbReportResult = new Holder<>();
			Holder<Boolean> stackReportResultCallbackCalled = new Holder<>(false);
			Holder<Object> stackReportResult = new Holder<>();

			assertTrue(taskScheduler.runTaskAndWait((tc) -> {
				try (MockedStatic<ReportUtils> reportUtilsMock = mockStatic(ReportUtils.class,
				        Mockito.CALLS_REAL_METHODS)) {
					TestsReportUtils.setupMockOfCollectAndFillReportedData(reportUtilsMock, reported,
					        reportedAdvancedData);

					ReportUtils.createReportAsynchronously(reporter, reported, reason, createReportDate, false,
					        taskScheduler, db, bm, um, (cr) -> {
						        crResult.set(cr);

						        if (cr != null && cr.r != null && cr.r.getId() >= 0) {
							        stackReportAsynchronouslyAndCollectReportFromDatabase(updateDate, taskScheduler, db,
							                reported, reason, um, rm, reporters, stackReportDate, dbReportResult,
							                stackReportResultCallbackCalled, stackReportResult, tc, cr,
							                newReporter ? 1 : 0); // if new reporters, start at 1 to skip first reporter (createReportReporter)
						        } else {
							        LOGGER.error(
							                "testStackReportAsynchronouslyOnExistingReport(): invalid report or id");
							        tc.setDone();
						        }
					        });
				} catch (Exception e) {
					e.printStackTrace();
				}
			}, TestsTaskScheduler.DEFAULT_TIMEOUT));
			assertTrue(stackReportResultCallbackCalled.get());

			assertNotNull(crResult.get());
			Report crReport = crResult.get().r;
			assertNotNull(crReport);
			Report dbReport = dbReportResult.get();

			if (newReporter) {
				assertNotNull(stackReportResult.get());
				assertTrue(stackReportResult.get() instanceof Report);
				Report stackedReport = (Report) stackReportResult.get();

				assertNotNull(dbReport);

				assertEquals(dbReport, stackedReport);
				assertThat(dbReport).usingRecursiveComparison().ignoringFields("advancedData").isEqualTo(stackedReport);

				if (updateDate) {
					assertEquals(stackReportDate, dbReport.getDate());
				} else {
					assertEquals(createReportDate, dbReport.getDate());
				}
			} else {
				assertNotNull(stackReportResult.get());
				assertEquals(Boolean.FALSE, stackReportResult.get());

				assertEquals(createReportDate, dbReport.getDate());
			}

			assertTrue(dbReport.hasAdvancedData());
			assertEquals(Report.AdvancedData.fromMap(reportAdvancedData), dbReport.getAdvancedData());

			assertEquals(Status.WAITING, dbReport.getStatus());
			assertEquals(StatusDetails.from(Status.WAITING, null).toString(), dbReport.getStatusDetails());
			assertEquals(null, dbReport.getProcessingStaff());
			assertEquals(null, dbReport.getProcessorStaff());
			assertEquals(Appreciation.NONE, dbReport.getAppreciation());
			assertEquals(AppreciationDetails.from(Appreciation.NONE, null).toString(),
			        dbReport.getAppreciationDetails());
			assertEquals(reported, dbReport.getReported());
			assertEquals(reported.getUniqueId(), dbReport.getReportedUniqueId());
			assertEquals(reporters, dbReport.getReporters());
			assertEquals(reporters.size(), dbReport.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), dbReport.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), dbReport.getLastReporter());
			assertEquals(reporters.size() > 1, dbReport.isStackedReport());
			assertEquals(reason, dbReport.getReason(false));
			assertEquals(false, dbReport.isArchived());
			assertEquals(Message.NONE_FEMALE.get(), dbReport.getPunishment());
		}

		private void stackReportAsynchronouslyAndCollectReportFromDatabase(boolean updateDate,
		        TestsTaskScheduler taskScheduler, Database db, User reported, String reason, UsersManager um,
		        ReportsManager rm, List<User> reporters, String stackReportDate, Holder<Report> dbReportResult,
		        Holder<Boolean> stackReportResultCallbackCalled, Holder<Object> stackReportResult, TaskCompletion tc,
		        CreatedReport cr, int i) {
			String currentReporter = reporters.get(i).getUniqueId().toString();
			List<User> afterStackReportCallReporters = reporters.subList(0, i + 1); // +1 for current reporter
			TestsReport.setupUMForAsynchronouslyFrom(um, reported, afterStackReportCallReporters);
			if (i == reporters.size() - 1) {
				ReportUtils.stackReportAsynchronously(currentReporter, reported.getUniqueId().toString(), reason,
				        stackReportDate, updateDate, taskScheduler, db, rm, um, (result) -> {
					        stackReportResultCallbackCalled.set(true);
					        stackReportResult.set(result);
					        rm.getReportByIdAsynchronously(cr.r.getId(), true, false, db, taskScheduler, um, (r) -> {
						        dbReportResult.set(r);
						        tc.setDone();
					        });
				        });
			} else {
				ReportUtils.stackReportAsynchronously(currentReporter, reported.getUniqueId().toString(), reason,
				        stackReportDate, updateDate, taskScheduler, db, rm, um, (result) -> {
					        stackReportAsynchronouslyAndCollectReportFromDatabase(updateDate, taskScheduler, db,
					                reported, reason, um, rm, reporters, stackReportDate, dbReportResult,
					                stackReportResultCallbackCalled, stackReportResult, tc, cr, i + 1);
				        });
			}
		}

		@Test
		void testStackReportAsynchronouslyOnNonExistentReport() {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			boolean updateDate = true;
			String reason = "report reason";

			BungeeManager bm = mock(BungeeManager.class);
			when(bm.getServerName()).thenReturn("bungeeServerName");

			UsersManager um = mock(UsersManager.class);
			ReportsManager rm = new ReportsManager();

			String stackReportReported = UUID.randomUUID().toString();

			String stackReportDate = DatetimeUtils.getRelativeDatetime(5);
			UUID stackReportReporterUUID = UUID.randomUUID();
			String stackReportReporter = stackReportReporterUUID.toString();

			Holder<Boolean> stackReportResultCallbackCalled = new Holder<>(false);
			Holder<Object> stackReportResult = new Holder<>();

			assertTrue(taskScheduler.runTaskAndWait((tc) -> {
				ReportUtils.stackReportAsynchronously(stackReportReporter, stackReportReported, reason, stackReportDate,
				        updateDate, taskScheduler, db, rm, um, (result) -> {
					        stackReportResultCallbackCalled.set(true);
					        stackReportResult.set(result);
					        tc.setDone();
				        });
			}, TestsTaskScheduler.DEFAULT_TIMEOUT));
			assertTrue(stackReportResultCallbackCalled.get());

			assertNull(stackReportResult.get());
		}

	}

	/**
	 * Test methods for
	 * {@link fr.mrtigreroux.tigerreports.utils.ReportUtils#createReportAsynchronously(fr.mrtigreroux.tigerreports.objects.users.User, fr.mrtigreroux.tigerreports.objects.users.User, java.lang.String, java.lang.String, boolean, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.bungee.BungeeManager, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
	 */
	@Nested
	class CreateReportAsynchronously {

		@Test
		void testCreateReportAsynchronously1() {
			testCreateReportAsynchronously(true, true, false);
		}

		@Test
		void testCreateReportAsynchronously2() {
			testCreateReportAsynchronously(true, true, true);
		}

		@Test
		void testCreateReportAsynchronously3() {
			testCreateReportAsynchronously(false, true, true);
		}

		@Test
		void testCreateReportAsynchronously4() {
			testCreateReportAsynchronously(false, true, false);
		}

		@Test
		void testCreateReportAsynchronously5() {
			testCreateReportAsynchronously(false, false, false);
		}

		@Test
		void testCreateSeveralReportsAsynchronously() {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			testCreateReportAsynchronously(true, true, false, taskScheduler, db);
			testCreateReportAsynchronously(true, true, true, taskScheduler, db);
			testCreateReportAsynchronously(false, true, true, taskScheduler, db);
			testCreateReportAsynchronously(false, true, false, taskScheduler, db);
			testCreateReportAsynchronously(false, false, false, taskScheduler, db);
		}

		void testCreateReportAsynchronously(boolean reportedOnline, boolean reporterOnline, boolean maxReportsReached) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			testCreateReportAsynchronously(reportedOnline, reporterOnline, maxReportsReached, taskScheduler, db);
		}

		void testCreateReportAsynchronously(boolean reportedOnline, boolean reporterOnline, boolean maxReportsReached,
		        TestsTaskScheduler taskScheduler, Database db) {
			User reported = mock(User.class);
			TestsReportUtils.setupRandomUserMockForCreateReportAsynchronously(reported,
			        reportedOnline ? mock(Player.class) : null);

			User reporter = mock(User.class);
			TestsReportUtils.setupRandomUserMockForCreateReportAsynchronously(reporter,
			        reporterOnline ? mock(Player.class) : null);
			List<User> reporters = Arrays.asList(reporter);

			LOGGER.info(() -> "reporter = " + reporter.getUniqueId() + "\n reported = " + reported.getUniqueId());
			String reason = "report reason";
			String date = DatetimeUtils.getNowDatetime();

			BungeeManager bm = mock(BungeeManager.class);
			when(bm.getServerName()).thenReturn("bungeeServerName");

			Map<String, Object> reportAdvancedData = new HashMap<>();
			reportAdvancedData.put(Report.AdvancedData.REPORTER_IP, reporter.getIPAddress());
			reportAdvancedData.put(Report.AdvancedData.REPORTER_LOCATION,
			        reporterOnline ? SerializationUtils.serializeLocation(reporter.getPlayer().getLocation(), bm) : null);
			reportAdvancedData.put(Report.AdvancedData.REPORTER_MESSAGES,
			        Report.AdvancedData.serializeMessages(reporter.getLastMessages()));

			Map<String, Object> reportedAdvancedData = new HashMap<>();
			reportedAdvancedData.put(Report.AdvancedData.REPORTED_IP, reported.getIPAddress());
			reportedAdvancedData.put(Report.AdvancedData.REPORTED_LOCATION,
			        reportedOnline ? SerializationUtils.serializeLocation(reported.getPlayer().getLocation(), bm) : null);
			reportedAdvancedData.put(Report.AdvancedData.REPORTED_MESSAGES,
			        Report.AdvancedData.serializeMessages(reported.getLastMessages()));
			if (reportedOnline) {
				TestsReport.fillMissingWithRandomReportedAdvancedData(reportedAdvancedData, bm);
			}

			reportAdvancedData.putAll(reportedAdvancedData);

			UsersManager um = mock(UsersManager.class);
			TestsReport.setupUMForAsynchronouslyFrom(um, reported, reporters);
			ReportsManager rm = new ReportsManager();

			Holder<Boolean> resultCallbackCalled = new Holder<>(false);
			Holder<CreatedReport> crResult = new Holder<>();
			Holder<Report> dbReportResult = new Holder<>();

			assertTrue(taskScheduler.runTaskAndWait((tc) -> {
				try (MockedStatic<ReportUtils> reportUtilsMock = mockStatic(ReportUtils.class,
				        Mockito.CALLS_REAL_METHODS)) {
					TestsReportUtils.setupMockOfCollectAndFillReportedData(reportUtilsMock, reported,
					        reportedAdvancedData);

					ReportUtils.createReportAsynchronously(reporter, reported, reason, date, maxReportsReached,
					        taskScheduler, db, bm, um, (cr) -> {
						        resultCallbackCalled.set(true);
						        crResult.set(cr);

						        if (cr != null && cr.r != null && cr.r.getId() >= 0) {
							        rm.getReportByIdAsynchronously(cr.r.getId(), true, false, db, taskScheduler, um,
							                (r) -> {
								                dbReportResult.set(r);
								                tc.setDone();
							                });
						        } else {
							        LOGGER.info(
							                () -> "testCreateReportAsynchronously(): invalid report or id, don't call getReportByIdAsynchronously");
							        tc.setDone();
						        }
					        });
				} catch (Exception e) {
					e.printStackTrace();
				}
			}, TestsTaskScheduler.DEFAULT_TIMEOUT));
			assertTrue(resultCallbackCalled.get());

			assertNotNull(crResult.get());
			Report crReport = crResult.get().r;
			assertNotNull(crReport);
			Report dbReport = dbReportResult.get();

			if (!maxReportsReached) {
				assertNotNull(dbReport);

				assertEquals(dbReport, crReport);
				assertThat(dbReport).usingRecursiveComparison().ignoringFields("advancedData").isEqualTo(crReport);

				assertTrue(dbReport.hasAdvancedData());
				assertEquals(Report.AdvancedData.fromMap(reportAdvancedData), dbReport.getAdvancedData());

				assertEquals(!reportedOnline, crResult.get().missingData);
			} else {
				assertNull(dbReport);
				assertEquals(-1, crReport.getId());
				assertFalse(crResult.get().missingData);
			}

			assertEquals(Status.WAITING, crReport.getStatus());
			assertEquals(StatusDetails.from(Status.WAITING, null).toString(), crReport.getStatusDetails());
			assertEquals(null, crReport.getProcessingStaff());
			assertEquals(null, crReport.getProcessorStaff());
			assertEquals(Appreciation.NONE, crReport.getAppreciation());
			assertEquals(AppreciationDetails.from(Appreciation.NONE, null).toString(),
			        crReport.getAppreciationDetails());
			assertEquals(date, crReport.getDate());
			assertEquals(reported, crReport.getReported());
			assertEquals(reported.getUniqueId(), crReport.getReportedUniqueId());
			assertEquals(reporters, crReport.getReporters());
			assertEquals(reporters.size(), crReport.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), crReport.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), crReport.getLastReporter());
			assertEquals(reason, crReport.getReason(false));
			assertEquals(false, crReport.isArchived());
			assertEquals(reporters.size() > 1, crReport.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), crReport.getPunishment());
			assertFalse(crReport.hasAdvancedData());
		}

	}

//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.utils.ReportUtils#sendReport(fr.mrtigreroux.tigerreports.objects.reports.Report, java.lang.String, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.managers.BungeeManager)}.
//	 */
//	@Test
//	void testSendReport() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.utils.ReportUtils#checkMaxReportsReachedAsynchronously(fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testCheckMaxReportsReachedAsynchronously() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link fr.mrtigreroux.tigerreports.utils.ReportUtils#getTotalReports(fr.mrtigreroux.tigerreports.data.database.Database)}.
//	 */
//	@Test
//	void testGetTotalReports() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link fr.mrtigreroux.tigerreports.utils.ReportUtils#getMaxReports()}.
//	 */
//	@Test
//	void testGetMaxReports() {
//		fail("Not yet implemented");
//	}

}
