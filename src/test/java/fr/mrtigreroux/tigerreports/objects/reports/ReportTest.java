package fr.mrtigreroux.tigerreports.objects.reports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.TestClass;
import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.bungee.TestsBungeeManager;
import fr.mrtigreroux.tigerreports.bungee.notifications.ArchiveBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.DeleteBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.ProcessAbusiveBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.ProcessBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.ProcessPunishBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.StatusBungeeNotification;
import fr.mrtigreroux.tigerreports.bungee.notifications.UnarchiveBungeeNotification;
import fr.mrtigreroux.tigerreports.data.Holder;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Appreciation;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.TestsDatabaseManager;
import fr.mrtigreroux.tigerreports.data.database.TestsSQLite;
import fr.mrtigreroux.tigerreports.events.ProcessReportEvent;
import fr.mrtigreroux.tigerreports.events.ReportStatusChangeEvent;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report.AppreciationDetails;
import fr.mrtigreroux.tigerreports.objects.reports.Report.ParticipantType;
import fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails;
import fr.mrtigreroux.tigerreports.objects.users.OfflineUserData;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUserData;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.objects.users.User.SavedMessage;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler;
import fr.mrtigreroux.tigerreports.utils.AssertionUtils;
import fr.mrtigreroux.tigerreports.utils.DatetimeUtils;
import fr.mrtigreroux.tigerreports.utils.TestsReportUtils;

/**
 * @author MrTigreroux
 */
public class ReportTest extends TestClass {

	/**
	 * Test methods for
	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#asynchronouslyFrom(java.util.Map, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
	 */
	@Nested
	class MainAsynchronouslyFrom {

		@Test
		void testAsynchronouslyFromSaveAdvancedData() throws InterruptedException {
			testAsynchronouslyFrom(false, true);
		}

		@Test
		void testAsynchronouslyFromDontSaveAdvancedData() throws InterruptedException {
			testAsynchronouslyFrom(false, false);
		}

		@Test
		void testAsynchronouslyFromArchivedSaveAdvancedData() throws InterruptedException {
			testAsynchronouslyFrom(true, true);
		}

		@Test
		void testAsynchronouslyFromArchivedDontSaveAdvancedData() throws InterruptedException {
			testAsynchronouslyFrom(true, false);
		}

		void testAsynchronouslyFrom(boolean archived, boolean saveAdvancedData) throws InterruptedException {
			int reportId = 5;

			Status status = Status.WAITING;
			User staff = null;
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));
			String reportedUUID = reported.getUniqueId().toString();
			String reportersUUID = TestsReport.getReportersStr(reporters);

			LOGGER.info(() -> "reported: " + reportedUUID + ", reporters: " + reportersUUID);
			String reason = "report reason";
			String reporterIP = "/127.0.0.145";
			String reporterLocation = "localhost/world/12.5/15.6/17.5/17.2/14.258";
			SavedMessage[] reporterMessages = new SavedMessage[] { new SavedMessage("Hello"),
			        new SavedMessage("Hello2") };

			String reportedIP = "/127.0.0.985";
			String reportedLocation = "server2/world2/12.5/18.6/14.5/17.2/14.258";
			SavedMessage[] reportedMessages = new SavedMessage[0];
			String reportedGamemode = GameMode.SURVIVAL.toString().toLowerCase();
			int reportedOnGround = 1;
			int reportedSneaking = 1;
			int reportedSprinting = 1;
			String reportedHealth = "18/20";
			int reportedFood = 14;
			String reportedEffects = "HEAL:2/32";

			Map<String, Object> reportData = new HashMap<>();
			reportData.put(Report.REPORT_ID, reportId);
			reportData.put(Report.STATUS, statusDetails.toString());
			reportData.put(Report.APPRECIATION, appreciationDetails.toString());
			reportData.put(Report.DATE, date);
			reportData.put(Report.REPORTED_UUID, reportedUUID);
			reportData.put(Report.REPORTER_UUID, reportersUUID);
			reportData.put(Report.REASON, reason);

			Map<String, Object> reportAdvancedData = new HashMap<>();
			reportAdvancedData.put(Report.AdvancedData.REPORTER_IP, reporterIP);
			reportAdvancedData.put(Report.AdvancedData.REPORTER_LOCATION, reporterLocation);
			reportAdvancedData.put(Report.AdvancedData.REPORTER_MESSAGES,
			        Report.AdvancedData.serializeMessages(Arrays.asList(reporterMessages)));

			reportAdvancedData.put(Report.AdvancedData.REPORTED_IP, reportedIP);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_LOCATION, reportedLocation);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_MESSAGES,
			        Report.AdvancedData.serializeMessages(Arrays.asList(reportedMessages)));
			reportAdvancedData.put(Report.AdvancedData.REPORTED_GAMEMODE, reportedGamemode);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_ON_GROUND, reportedOnGround);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_SNEAK, reportedSneaking);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_SPRINT, reportedSprinting);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_HEALTH, reportedHealth);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_FOOD, reportedFood);
			reportAdvancedData.put(Report.AdvancedData.REPORTED_EFFECTS, reportedEffects);

			reportData.putAll(reportAdvancedData);

			Database db = mock(Database.class);
			TaskScheduler taskScheduler = mock(TaskScheduler.class);
			UsersManager um = mock(UsersManager.class);

			TestsReport.setupUMForAsynchronouslyFrom(um, reported, reporters);

			AtomicBoolean resultCallbackCalled = new AtomicBoolean(false);
			Report.asynchronouslyFrom(reportData, archived, saveAdvancedData, db, taskScheduler, um, (r) -> {
				resultCallbackCalled.set(true);
				assertNotNull(r);
				assertEquals(reportId, r.getId());
				assertEquals(status, r.getStatus());
				assertEquals(statusDetails.toString(), r.getStatusDetails());
				assertEquals(null, r.getProcessingStaff());
				assertEquals(null, r.getProcessorStaff());
				assertEquals(appreciation, r.getAppreciation());
				assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
				assertEquals(date, r.getDate());
				assertEquals(reported, r.getReported());
				assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
				assertEquals(reporters, r.getReporters());
				assertEquals(reporters.size(), r.getReportersAmount());
				assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
				assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
				assertEquals(reason, r.getReason(false));
				assertEquals(archived, r.isArchived());
				assertEquals(reporters.size() > 1, r.isStackedReport());
				assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());

				boolean shouldHaveAdvancedData = saveAdvancedData && !archived;
				assertEquals(shouldHaveAdvancedData, r.hasAdvancedData());
				if (shouldHaveAdvancedData) {
					assertArrayEquals(reportedMessages, r.getMessagesHistory(ParticipantType.REPORTED));
					assertArrayEquals(reporterMessages, r.getMessagesHistory(ParticipantType.REPORTER));
					assertEquals(reportedLocation, r.getOldLocation(ParticipantType.REPORTED));
					assertEquals(reporterLocation, r.getOldLocation(ParticipantType.REPORTER));
					assertEquals(Report.AdvancedData.fromMap(reportAdvancedData), r.getAdvancedData());
				} else {
					assertArrayEquals(new SavedMessage[0], r.getMessagesHistory(ParticipantType.REPORTED));
					assertArrayEquals(new SavedMessage[0], r.getMessagesHistory(ParticipantType.REPORTER));
					assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
					assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
					assertEquals(null, r.getAdvancedData());
				}
			});
			assertTrue(resultCallbackCalled.get());
		}

	}

	/**
	 * Test methods for
	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#Report(int, fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails, java.lang.String, java.lang.String, fr.mrtigreroux.tigerreports.objects.users.User, java.util.List, java.lang.String, boolean)}.
	 */
	@Nested
	class MainConstructor {

		@Test
		void testReportWaiting() {
			int reportId = 5;

			Status status = Status.WAITING;
			User staff = null;
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(null, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportInProgress() {
			int reportId = 18;

			Status status = Status.IN_PROGRESS;
			Player staffPlayer = mock(Player.class);
			User staff = new User(UUID.randomUUID(), null, new OnlineUserData(staffPlayer));
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(staff, r.getProcessingStaff());
			assertEquals(null, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportImportant() {
			int reportId = 18;

			Status status = Status.IMPORTANT;
			User staff = null;
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(null, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportDone() {
			int reportId = 45;

			Status status = Status.DONE;
			Player staffPlayer = mock(Player.class);
			User staff = new User(UUID.randomUUID(), null, new OnlineUserData(staffPlayer));
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.TRUE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(staff, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportDoneSeveralReporters() {
			int reportId = 45;

			Status status = Status.DONE;
			Player staffPlayer = mock(Player.class);
			User staff = new User(UUID.randomUUID(), null, new OnlineUserData(staffPlayer));
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.TRUE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer1 = mock(Player.class);
			Player reporterPlayer2 = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer1)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer2)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(staff, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportDoneSeveralReportersPunishment() {
			int reportId = 45;

			Status status = Status.DONE;
			Player staffPlayer = mock(Player.class);
			User staff = new User(UUID.randomUUID(), null, new OnlineUserData(staffPlayer));
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.TRUE;
			String punishment = "punishment name";
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, punishment);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer1 = mock(Player.class);
			Player reporterPlayer2 = mock(Player.class);
			Player reporterPlayer3 = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer1)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer2)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer3)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(staff, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(punishment, r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportDoneUncertainSeveralReporters() {
			int reportId = 45;

			Status status = Status.DONE;
			Player staffPlayer = mock(Player.class);
			User staff = new User(UUID.randomUUID(), null, new OnlineUserData(staffPlayer));
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.UNCERTAIN;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer1 = mock(Player.class);
			Player reporterPlayer2 = mock(Player.class);
			Player reporterPlayer3 = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer1)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer2)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer3)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(staff, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportDoneFalseSeveralReporters() {
			int reportId = 45;

			Status status = Status.DONE;
			Player staffPlayer = mock(Player.class);
			User staff = new User(UUID.randomUUID(), null, new OnlineUserData(staffPlayer));
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.FALSE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			Player reporterPlayer1 = mock(Player.class);
			Player reporterPlayer2 = mock(Player.class);
			Player reporterPlayer3 = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer1)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer2)),
			        new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer3)));

			String reason = "report reason";
			boolean archived = false;

			Report r = new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason,
			        archived);

			assertEquals(reportId, r.getId());
			assertEquals(status, r.getStatus());
			assertEquals(statusDetails.toString(), r.getStatusDetails());
			assertEquals(null, r.getProcessingStaff());
			assertEquals(staff, r.getProcessorStaff());
			assertEquals(appreciation, r.getAppreciation());
			assertEquals(appreciationDetails.toString(), r.getAppreciationDetails());
			assertEquals(date, r.getDate());
			assertEquals(reported, r.getReported());
			assertEquals(reported.getUniqueId(), r.getReportedUniqueId());
			assertEquals(reporters, r.getReporters());
			assertEquals(reporters.size(), r.getReportersAmount());
			assertEquals(reporters.get(0).getUniqueId(), r.getReporterUniqueId());
			assertEquals(reporters.get(reporters.size() - 1), r.getLastReporter());
			assertEquals(reason, r.getReason(false));
			assertEquals(archived, r.isArchived());

			assertEquals(reporters.size() > 1, r.isStackedReport());
			assertEquals(Message.NONE_FEMALE.get(), r.getPunishment());
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTED).length);
			assertEquals(0, r.getMessagesHistory(ParticipantType.REPORTER).length);
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTED));
			assertEquals(null, r.getOldLocation(ParticipantType.REPORTER));
		}

		@Test
		void testReportReportedNull() {
			int reportId = 45;

			Status status = Status.WAITING;
			User staff = null;
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			User reported = null;

			Player reporterPlayer = mock(Player.class);
			List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));

			String reason = "report reason";
			boolean archived = false;

			assertThrows(NullPointerException.class, () -> {
				new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason, archived);
			});
		}

		@Test
		void testReportReporterNull() {
			int reportId = 45;

			Status status = Status.WAITING;
			User staff = null;
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			List<User> reporters = null;

			String reason = "report reason";
			boolean archived = false;

			assertThrows(NullPointerException.class, () -> {
				new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason, archived);
			});
		}

		@Test
		void testReportReporterEmpty() {
			int reportId = 45;

			Status status = Status.WAITING;
			User staff = null;
			StatusDetails statusDetails = StatusDetails.from(status, staff);

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			List<User> reporters = new ArrayList<>();

			String reason = "report reason";
			boolean archived = false;

			assertThrows(IllegalArgumentException.class, () -> {
				new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason, archived);
			});
		}

		@Test
		void testReportStatusDetailsNull() {
			int reportId = 45;

			StatusDetails statusDetails = null;

			Appreciation appreciation = Appreciation.NONE;
			AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
			String date = DatetimeUtils.getNowDatetime();

			Player reportedPlayer = mock(Player.class);
			User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

			List<User> reporters = new ArrayList<>();

			String reason = "report reason";
			boolean archived = false;

			assertThrows(NullPointerException.class, () -> {
				new Report(reportId, statusDetails, appreciationDetails, date, reported, reporters, reason, archived);
			});
		}

	}

//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#getPlayerName(fr.mrtigreroux.tigerreports.objects.reports.Report.ParticipantType, boolean, boolean, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.managers.BungeeManager)}.
//	 */
//	@Test
//	void testGetPlayerNameParticipantTypeBooleanBooleanVaultManagerBungeeManager() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#getPlayerName(fr.mrtigreroux.tigerreports.objects.users.User, fr.mrtigreroux.tigerreports.objects.reports.Report.ParticipantType, boolean, boolean, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.managers.BungeeManager)}.
//	 */
//	@Test
//	void testGetPlayerNameUserParticipantTypeBooleanBooleanVaultManagerBungeeManager() {
//		fail("Not yet implemented");
//	}

	/**
	 * Test method for
	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#setStatus(fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.managers.ReportsManager, fr.mrtigreroux.tigerreports.bungee.BungeeManager)}.
	 */
	@Nested
	class SetStatus {

		@Test
		void test1() {
			testSetStatus(StatusDetails.from(Status.WAITING, null), true, false);
		}

		@Test
		void test2() {
			User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
			testSetStatus(StatusDetails.from(Status.IN_PROGRESS, staff), true, false);
		}

		@Test
		void test3() {
			User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
			testSetStatus(StatusDetails.from(Status.IMPORTANT, staff), true, false);
		}

		@Test
		void test4() {
			User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
			testSetStatus(StatusDetails.from(Status.DONE, staff), true, false);
		}

		@Test
		void test5() {
			User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
			testSetStatus(StatusDetails.from(Status.DONE, staff), false, false);
		}

		@Test
		void test6() {
			User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
			testSetStatus(StatusDetails.from(Status.DONE, staff), false, false);
		}

		@Test
		void test7() {
			testSetStatus(StatusDetails.from(Status.WAITING, null), false, false);
		}

		@Test
		void test8() {
			testSetStatus(StatusDetails.from(Status.WAITING, null), false, true);
		}

		@Test
		void test9() {
			User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
			testSetStatus(StatusDetails.from(Status.DONE, staff), false, true);
		}

		@Test
		void testNull() {
			testSetStatus(null, true, false);
		}

		void testSetStatus(StatusDetails statusDetails, boolean newDifferentStatusDetails, boolean bungee) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			ReportsManager rm = spy(ReportsManager.class);
			UsersManager um = mock(UsersManager.class);
			VaultManager vm = mock(VaultManager.class);
			BungeeManager bm = mock(BungeeManager.class);

			Holder<Event> calledEvent = new Holder<>();

			testReportAction(newDifferentStatusDetails ? (report) -> {
				if (statusDetails != null && statusDetails.toString().equals(report.getStatusDetails())) {
					Status differentStatus = report.getStatus() == Status.WAITING ? Status.IMPORTANT : Status.WAITING;
					StatusDetails differentStatusDetails = StatusDetails.from(differentStatus, null);

					TestsReport.mockPluginManagerCallEventAndRunReportAction(report, new Holder<>(),
					        (r) -> r.setStatus(differentStatusDetails, false, db, rm, bm));
					clearInvocations(bm, rm);
				}
			} : (report) -> {
				if (statusDetails != null && !statusDetails.toString().equals(report.getStatusDetails())) {
					TestsReport.mockPluginManagerCallEventAndRunReportAction(report, new Holder<>(),
					        (r) -> r.setStatus(statusDetails, false, db, rm, bm));
					clearInvocations(bm, rm);
				}
			}, (report) -> {
				TestsReport.mockPluginManagerCallEventAndRunReportAction(report, calledEvent,
				        statusDetails == null
				                ? (r) -> assertThrows(NullPointerException.class,
				                        () -> r.setStatus(statusDetails, bungee, db, rm, bm))
				                : (r) -> r.setStatus(statusDetails, bungee, db, rm, bm));
			}, taskScheduler, db, um, vm,
			        (Report beforeReport, Report afterReport, Report dbReport, Object sentStaffMessage) -> {
				        assertNotNull(beforeReport);
				        if (statusDetails != null) {
					        assertNotNull(afterReport);
					        assertNotNull(dbReport);

					        if (newDifferentStatusDetails) {
						        boolean dbReportShouldBeModified = !bungee;

						        assertNotEquals(statusDetails.toString(), beforeReport.getStatusDetails());
						        assertEquals(statusDetails.toString(), afterReport.getStatusDetails());
						        if (dbReportShouldBeModified) {
							        assertEquals(afterReport.getStatusDetails(), dbReport.getStatusDetails());
							        AssertionUtils.assertFieldEquals(afterReport, dbReport, "statusDetails");

							        assertEquals(statusDetails.toString(), dbReport.getStatusDetails());
						        } else {
							        assertEquals(beforeReport.getStatusDetails(), dbReport.getStatusDetails());
							        AssertionUtils.assertFieldEquals(beforeReport, dbReport, "statusDetails");

							        assertNotEquals(statusDetails.toString(), dbReport.getStatusDetails());
						        }
					        } else {
						        assertEquals(statusDetails.toString(), beforeReport.getStatusDetails());
						        assertEquals(statusDetails.toString(), afterReport.getStatusDetails());
						        assertEquals(statusDetails.toString(), dbReport.getStatusDetails());

						        AssertionUtils.assertFieldEquals(beforeReport, afterReport, "statusDetails");
						        AssertionUtils.assertFieldEquals(beforeReport, dbReport, "statusDetails");
					        }

					        AssertionUtils.assertDeepEquals(beforeReport, afterReport, "statusDetails");
					        AssertionUtils.assertDeepEquals(beforeReport, dbReport, "statusDetails");

					        if (newDifferentStatusDetails) {
						        ArgumentCaptor<Report> broadcastReportDataChangedReport = ArgumentCaptor
						                .forClass(Report.class);
						        verify(rm, times(1))
						                .broadcastReportDataChanged(broadcastReportDataChangedReport.capture());
						        assertTrue(broadcastReportDataChangedReport.getValue() == afterReport,
						                () -> "the same (unique) report instance should be used for broadcast");
					        } else {
						        verify(rm, times(0)).broadcastReportDataChanged(any(Report.class));
					        }

					        assertNull(sentStaffMessage);

					        if (!bungee) {
						        StatusBungeeNotification statusBungeeNotif = TestsBungeeManager
						                .assertCalledOnceSendPluginNotificationToAll(bm, StatusBungeeNotification.class);
						        assertEquals(statusDetails.toString(), statusBungeeNotif.statusDetails);
						        assertEquals(beforeReport.getId(), statusBungeeNotif.reportId);
					        } else {
								TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm, StatusBungeeNotification.class);
					        }

					        assertNotNull(calledEvent.get());
					        assertTrue(calledEvent.get() instanceof ReportStatusChangeEvent);
				        }
			        });
		}

	}

	/**
	 * Test methods for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#processing()}.
	 */
	@Nested
	class Processing {

		/**
		 * Test method for
		 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#process(fr.mrtigreroux.tigerreports.objects.users.User, java.lang.String, boolean, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database)}.
		 */
		@Nested
		class Process {

			@Test
			void test1() {
				testProcessWithValidStaff(Appreciation.TRUE, true, true, true, false);
			}

			@Test
			void test2() {
				testProcessWithValidStaff(Appreciation.TRUE, false, true, true, false);
			}

			@Test
			void test3() {
				testProcessWithValidStaff(Appreciation.TRUE, true, false, true, false);
			}

			@Test
			void test4() {
				testProcessWithValidStaff(Appreciation.TRUE, false, true, false, false);
			}

			@Test
			void test5() {
				testProcessWithValidStaff(Appreciation.TRUE, false, true, true, true);
			}

			@Test
			void test6() {
				testProcessWithValidStaff(Appreciation.TRUE, true, true, true, true);
			}

			@Test
			void test7() {
				testProcessWithValidStaff(Appreciation.TRUE, false, false, false, false);
			}

			@Test
			void test8() {
				testProcessWithValidStaff(Appreciation.TRUE, false, false, true, false);
			}

			@Test
			void test9() {
				testProcessWithValidStaff(Appreciation.UNCERTAIN, false, false, true, false);
			}

			@Test
			void test10() {
				testProcessWithValidStaff(Appreciation.FALSE, false, false, true, false);
			}

			@Test
			void testStaffNull() {
				testProcess(null, Appreciation.TRUE, true, true, true, false);
			}

			@Test
			void testAppreciationNull() {
				testProcessWithValidStaff(null, true, true, true, false);
			}

			@Test
			void testStaffAndAppreciationNull() {
				testProcess(null, null, true, true, true, false);
			}

			void testProcessWithValidStaff(Appreciation appreciation, boolean bungee, boolean autoArchive,
			        boolean notifyStaff, boolean alreadyProcessed) {
				User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
				testProcess(staff, appreciation, bungee, autoArchive, notifyStaff, alreadyProcessed);
			}

			void testProcess(User staff, Appreciation appreciation, boolean bungee, boolean autoArchive,
			        boolean notifyStaff, boolean alreadyProcessed) {
				TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
				Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

				ReportsManager rm = spy(ReportsManager.class);
				UsersManager um = mock(UsersManager.class);
				VaultManager vm = mock(VaultManager.class);
				BungeeManager bm = mock(BungeeManager.class);

				Holder<Event> calledEvent = new Holder<>();
				testProcessing(staff, appreciation, bungee, autoArchive, null, notifyStaff, null, alreadyProcessed,
				        (report) -> {
					        TestsReport.mockPluginManagerCallEventAndRunReportAction(report, calledEvent,
					                staff == null || appreciation == null
					                        ? (r) -> assertThrows(NullPointerException.class,
					                                () -> r.process(staff, appreciation, bungee, autoArchive,
					                                        notifyStaff, db, rm, vm, bm, taskScheduler))
					                        : (r) -> r.process(staff, appreciation, bungee, autoArchive, notifyStaff,
					                                db, rm, vm, bm, taskScheduler));
				        }, taskScheduler, db, rm, um, vm, bm, calledEvent);
			}

		}

		/**
		 * Test method for
		 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#processWithPunishment(fr.mrtigreroux.tigerreports.objects.users.User, boolean, boolean, java.lang.String, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.bungee.BungeeManager)}.
		 */
		@Nested
		class ProcessWithPunishment {

			@Test
			void test1() {
				testProcessWithPunishmentWithValidStaff(true, true, "punishment name", true, false);
			}

			@Test
			void test2() {
				testProcessWithPunishmentWithValidStaff(false, true, "punishment name", true, false);
			}

			@Test
			void test3() {
				testProcessWithPunishmentWithValidStaff(true, false, "punishment name", true, false);
			}

			@Test
			void test4() {
				testProcessWithPunishmentWithValidStaff(false, true, "punishment name", false, false);
			}

			@Test
			void test5() {
				testProcessWithPunishmentWithValidStaff(false, true, "punishment name", true, true);
			}

			@Test
			void test6() {
				testProcessWithPunishmentWithValidStaff(true, true, "punishment name", true, true);
			}

			@Test
			void test7() {
				testProcessWithPunishmentWithValidStaff(false, false, "punishment name", false, false);
			}

			@Test
			void test8() {
				testProcessWithPunishmentWithValidStaff(false, false, "punishment name", true, false);
			}

			@Test
			void testPunishmentNull() {
				testProcessWithPunishmentWithValidStaff(false, false, null, true, false);
			}

			@Test
			void testStaffNull() {
				testProcessWithPunishment(null, true, true, "punishment name", true, false);
			}

			@Test
			void testStaffAndPunishmentNull() {
				testProcessWithPunishment(null, true, true, null, true, false);
			}

			void testProcessWithPunishmentWithValidStaff(boolean bungee, boolean autoArchive, String punishment,
			        boolean notifyStaff, boolean alreadyProcessed) {
				User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
				testProcessWithPunishment(staff, bungee, autoArchive, punishment, notifyStaff, alreadyProcessed);
			}

			void testProcessWithPunishment(User staff, boolean bungee, boolean autoArchive, String punishment,
			        boolean notifyStaff, boolean alreadyProcessed) {
				TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
				Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

				ReportsManager rm = spy(ReportsManager.class);
				UsersManager um = mock(UsersManager.class);
				VaultManager vm = mock(VaultManager.class);
				BungeeManager bm = mock(BungeeManager.class);

				Holder<Event> calledEvent = new Holder<>();

				testProcessing(staff, Appreciation.TRUE, bungee, autoArchive, punishment, notifyStaff, null,
				        alreadyProcessed, (report) -> {
					        TestsReport.mockPluginManagerCallEventAndRunReportAction(report, calledEvent,
					                staff == null
					                        ? (r) -> assertThrows(NullPointerException.class,
					                                () -> r.processWithPunishment(staff, bungee, autoArchive,
					                                        punishment, notifyStaff, db, rm, vm, bm, taskScheduler))
					                        : (r) -> r.processWithPunishment(staff, bungee, autoArchive, punishment,
					                                notifyStaff, db, rm, vm, bm, taskScheduler));
				        }, taskScheduler, db, rm, um, vm, bm, calledEvent);
			}

		}

		/**
		 * Test method for
		 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#processAbusive(fr.mrtigreroux.tigerreports.objects.users.User, boolean, boolean, long, boolean, fr.mrtigreroux.tigerreports.data.database.Database)}.
		 */
		@Nested
		class ProcessAbusive {

			@Test
			void test1() {
				testProcessAbusiveWithValidStaff(true, true, 5, true, false);
			}

			@Test
			void test2() {
				testProcessAbusiveWithValidStaff(false, true, 5, true, false);
			}

			@Test
			void test3() {
				testProcessAbusiveWithValidStaff(true, false, 5, true, false);
			}

			@Test
			void test4() {
				testProcessAbusiveWithValidStaff(false, true, 5, false, false);
			}

			@Test
			void test5() {
				testProcessAbusiveWithValidStaff(false, true, 5, true, true);
			}

			@Test
			void test6() {
				testProcessAbusiveWithValidStaff(true, true, 5, true, true);
			}

			@Test
			void test7() {
				testProcessAbusiveWithValidStaff(false, false, 5, false, false);
			}

			@Test
			void test8() {
				testProcessAbusiveWithValidStaff(false, false, 5, true, false);
			}

			@Test
			void testStaffNull() {
				testProcessAbusive(null, false, true, 5, false, false);
			}

			void testProcessAbusiveWithValidStaff(boolean bungee, boolean autoArchive, long punishSeconds,
			        boolean notifyStaff, boolean alreadyProcessed) {
				User staff = new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"));
				testProcessAbusive(staff, bungee, autoArchive, punishSeconds, notifyStaff, alreadyProcessed);
			}

			void testProcessAbusive(User staff, boolean bungee, boolean autoArchive, long punishSeconds,
			        boolean notifyStaff, boolean alreadyProcessed) {
				TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
				Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

				ReportsManager rm = spy(ReportsManager.class);
				UsersManager um = mock(UsersManager.class);
				VaultManager vm = mock(VaultManager.class);
				BungeeManager bm = mock(BungeeManager.class);

				Holder<Event> calledEvent = new Holder<>();
				testProcessing(staff, Appreciation.FALSE, bungee, autoArchive, null, notifyStaff, punishSeconds,
				        alreadyProcessed, (report) -> {
					        TestsReport.mockPluginManagerCallEventAndRunReportAction(report, calledEvent,
					                staff == null
					                        ? (r) -> assertThrows(NullPointerException.class,
					                                () -> r.processAbusive(staff, bungee, autoArchive, punishSeconds,
					                                        notifyStaff, db, rm, um, bm, vm, taskScheduler))
					                        : (r) -> r.processAbusive(staff, bungee, autoArchive, punishSeconds,
					                                notifyStaff, db, rm, um, bm, vm, taskScheduler));
				        }, taskScheduler, db, rm, um, vm, bm, calledEvent);
			}

		}

		void testProcessing(User staff, Appreciation appreciation, boolean bungee, boolean autoArchive,
		        String punishment, boolean notifyStaff, Long punishSeconds, boolean alreadyProcessed,
		        Consumer<Report> reportAction, TestsTaskScheduler taskScheduler, Database db, ReportsManager rm,
		        UsersManager um, VaultManager vm, BungeeManager bm, Holder<Event> calledEvent) {
			testReportAction(alreadyProcessed ? (report) -> {
				if (report.getStatus() != Status.DONE) {
					TestsReport.mockPluginManagerCallEventAndRunReportAction(report, new Holder<>(), (r) -> r
					        .process(staff, appreciation, false, autoArchive, false, db, rm, vm, bm, taskScheduler));
					clearInvocations(bm, rm);
				}
			} : (report) -> {
				if (report.getStatus() == Status.DONE) {
					TestsReport.mockPluginManagerCallEventAndRunReportAction(report, new Holder<>(),
					        (r) -> r.setStatus(StatusDetails.from(Status.WAITING, null), false, db, rm, bm));
					clearInvocations(bm, rm);
				}
			}, reportAction, taskScheduler, db, um, vm,
			        (Report beforeReport, Report afterReport, Report dbReport, Object sentStaffMessage) -> {
				        assertNotNull(beforeReport);

				        if (staff != null && appreciation != null) {
					        assertNotNull(afterReport);
					        assertNotNull(dbReport);

					        boolean dbReportShouldBeModified = !bungee;

					        if (!alreadyProcessed) {
						        assertNotEquals(Status.DONE, beforeReport.getStatus());
					        } else {
						        assertEquals(Status.DONE, beforeReport.getStatus());
					        }
					        assertEquals(Status.DONE, afterReport.getStatus());
					        assertEquals(StatusDetails.from(Status.DONE, staff).toString(),
					                afterReport.getStatusDetails());
					        assertEquals(staff, afterReport.getProcessorStaff());

					        if (!alreadyProcessed) {
						        assertEquals(Appreciation.NONE, beforeReport.getAppreciation());
					        } else {
						        assertNotEquals(Appreciation.NONE, beforeReport.getAppreciation());
					        }
					        assertEquals(appreciation, afterReport.getAppreciation());
					        assertEquals(AppreciationDetails.from(appreciation, punishment).toString(),
					                afterReport.getAppreciationDetails());
					        assertEquals(Message.NONE_FEMALE.get(), beforeReport.getPunishment());
					        if (punishment == null) {
						        assertEquals(Message.NONE_FEMALE.get(), afterReport.getPunishment());
					        } else {
						        assertEquals(punishment, afterReport.getPunishment());
					        }

					        if (!alreadyProcessed) {
						        assertFalse(beforeReport.isArchived());
					        } else {
						        assertEquals(autoArchive, beforeReport.isArchived());
					        }

					        boolean afterReportShouldBeArchived = autoArchive;
					        boolean dbReportShouldBeArchived = autoArchive && !bungee;
					        assertEquals(afterReportShouldBeArchived, afterReport.isArchived());
					        if (!afterReportShouldBeArchived) {
						        AssertionUtils.assertFieldEquals(beforeReport, afterReport, "advancedData");
					        }

					        if (dbReportShouldBeModified) {
						        assertEquals(Status.DONE, dbReport.getStatus());
						        assertEquals(StatusDetails.from(Status.DONE, staff).toString(),
						                dbReport.getStatusDetails());
						        assertEquals(staff, dbReport.getProcessorStaff());

						        assertEquals(appreciation, dbReport.getAppreciation());
						        assertEquals(AppreciationDetails.from(appreciation, punishment).toString(),
						                dbReport.getAppreciationDetails());
						        if (punishment == null) {
							        assertEquals(Message.NONE_FEMALE.get(), dbReport.getPunishment());
						        } else {
							        assertEquals(punishment, dbReport.getPunishment());
						        }

						        assertEquals(dbReportShouldBeArchived, dbReport.isArchived());

						        AssertionUtils.assertDeepEquals(afterReport, dbReport);
					        } else {
						        assertEquals(beforeReport.getStatus(), dbReport.getStatus());
						        assertEquals(beforeReport.getStatusDetails(), dbReport.getStatusDetails());
						        assertEquals(beforeReport.getProcessorStaff(), dbReport.getProcessorStaff());
						        assertEquals(beforeReport.getProcessingStaff(), dbReport.getProcessingStaff());

						        assertEquals(beforeReport.getAppreciation(), dbReport.getAppreciation());
						        assertEquals(beforeReport.getAppreciationDetails(), dbReport.getAppreciationDetails());
						        assertEquals(beforeReport.getPunishment(), dbReport.getPunishment());

						        assertEquals(beforeReport.isArchived(), dbReport.isArchived());

						        AssertionUtils.assertFieldEquals(beforeReport, dbReport, "statusDetails");
						        AssertionUtils.assertFieldEquals(beforeReport, dbReport, "appreciationDetails");
						        AssertionUtils.assertFieldEquals(beforeReport, dbReport, "archived");
						        AssertionUtils.assertFieldEquals(beforeReport, dbReport, "advancedData");
					        }

					        AssertionUtils.assertDeepEquals(beforeReport, afterReport, "statusDetails",
					                "appreciationDetails", "archived", "advancedData");
					        AssertionUtils.assertDeepEquals(beforeReport, dbReport, "statusDetails",
					                "appreciationDetails", "archived", "advancedData");

					        if (!alreadyProcessed || punishment != null) {
						        ArgumentCaptor<Report> broadcastReportDataChangedReport = ArgumentCaptor
						                .forClass(Report.class);
						        verify(rm, times(1))
						                .broadcastReportDataChanged(broadcastReportDataChangedReport.capture());
						        assertTrue(broadcastReportDataChangedReport.getValue() == afterReport,
						                () -> "the same (unique) report instance should be used for broadcast");
					        } else { // was already processed with same appreciation, by same staff, with same archive action
						        verify(rm, times(0)).broadcastReportDataChanged(any(Report.class));
					        }

					        if (notifyStaff && staff != null) {
						        assertNotNull(sentStaffMessage);
					        } else {
						        assertNull(sentStaffMessage);
					        }

					        if (!bungee) {
						        if (punishSeconds == null) {
									if (punishment != null) {
										ProcessPunishBungeeNotification bungeeNotif = TestsBungeeManager
												.assertCalledOnceSendPluginNotificationToAll(bm, ProcessPunishBungeeNotification.class);
										assertEquals(staff.getUniqueId().toString(),
							                bungeeNotif.staffUniqueId);
										assertEquals(beforeReport.getId(), bungeeNotif.reportId);
										assertEquals(autoArchive, bungeeNotif.archive);
										assertEquals(punishment, bungeeNotif.punishment);
									} else {
										ProcessBungeeNotification bungeeNotif = TestsBungeeManager
												.assertCalledOnceSendPluginNotificationToAll(bm, ProcessBungeeNotification.class);
										assertEquals(staff.getUniqueId().toString(),
							                bungeeNotif.staffUniqueId);
										assertEquals(beforeReport.getId(), bungeeNotif.reportId);
										assertEquals(autoArchive, bungeeNotif.archive);
										assertEquals(appreciation, bungeeNotif.getAppreciation());
									}
						        } else {
									ProcessAbusiveBungeeNotification bungeeNotif = TestsBungeeManager
											.assertCalledOnceSendPluginNotificationToAll(bm, ProcessAbusiveBungeeNotification.class);
									assertEquals(staff.getUniqueId().toString(),
										bungeeNotif.staffUniqueId);
									assertEquals(beforeReport.getId(), bungeeNotif.reportId);
									assertEquals(autoArchive, bungeeNotif.archive);
									assertEquals(punishSeconds, bungeeNotif.punishSeconds);
						        }
						        // TODO: check statistics modification
						        // TODO: check notifications correctly sent
					        } else {
								TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm,ProcessBungeeNotification.class);
								TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm,ProcessPunishBungeeNotification.class);
								TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm,ProcessAbusiveBungeeNotification.class);
					        }

					        assertNotNull(calledEvent.get());
					        assertTrue(calledEvent.get() instanceof ProcessReportEvent);
					        ProcessReportEvent event = (ProcessReportEvent) calledEvent.get();
					        assertEquals(afterReport, event.getReport());
					        if (staff != null) {
						        assertEquals(staff.getName(), event.getStaff());
					        } else {
						        assertNull(event.getStaff());
					        }
					        assertEquals(bungee, event.isFromBungeeCord());
				        } else {
					        AssertionUtils.assertDeepEquals(beforeReport, afterReport);
					        AssertionUtils.assertDeepEquals(afterReport, dbReport);
				        }
			        });
		}

	}

//	/**
//	 * Test method for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#extractAndSaveAdvancedData(java.util.Map)}.
//	 */
//	@Test
//	void testExtractAndSaveAdvancedData() {
//		fail("Not yet implemented");
//	}

//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#addComment(fr.mrtigreroux.tigerreports.objects.users.User, java.lang.String, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testAddCommentUserStringDatabaseTaskSchedulerResultCallbackOfInteger() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#addComment(java.lang.String, java.lang.String, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testAddCommentStringStringDatabaseTaskSchedulerResultCallbackOfInteger() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#getCommentByIdAsynchronously(int, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testGetCommentByIdAsynchronously() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#getCommentAsynchronously(int, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testGetCommentAsynchronously() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#getCommentAsynchronouslyFrom(java.util.Map, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testGetCommentAsynchronouslyFrom() {
//		fail("Not yet implemented");
//	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#archive(fr.mrtigreroux.tigerreports.objects.users.User, boolean, fr.mrtigreroux.tigerreports.data.database.Database)}.
	 */
	@Nested
	class Archive {

		@Test
		void test1() {
			testArchive(true, true, false);
		}

		@Test
		void test2() {
			testArchive(true, false, false);
		}

		@Test
		void test3() {
			testArchive(true, true, true);
		}

		@Test
		void test4() {
			testArchive(true, false, true);
		}

		@Test
		void test5() {
			testArchive(false, true, false);
		}

		@Test
		void test6() {
			testArchive(false, false, false);
		}

		@Test
		void test7() {
			testArchive(false, true, true);
		}

		@Test
		void test8() {
			testArchive(false, false, true);
		}

		void testArchive(boolean onArchivedReport, boolean validStaff, boolean bungee) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			ReportsManager rm = spy(ReportsManager.class);
			User staff = validStaff
			        ? new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"))
			        : null;

			UsersManager um = mock(UsersManager.class);
			VaultManager vm = mock(VaultManager.class);
			BungeeManager bm = mock(BungeeManager.class);

			testReportAction(onArchivedReport ? TestsReport.getSuccessfulArchiveReportAction(db, rm, vm, bm)
			        : TestsReport.NO_REPORT_ACTION, (r) -> {
				        r.archive(staff, bungee, db, rm, vm, bm);
			        }, taskScheduler, db, um, vm,
			        (Report beforeReport, Report afterReport, Report dbReport, Object sentStaffMessage) -> {
				        assertNotNull(beforeReport);
				        assertNotNull(afterReport);
				        assertNotNull(dbReport);

				        if (!onArchivedReport) {
					        boolean dbReportShouldBeModified = staff != null && !bungee;

					        assertFalse(beforeReport.isArchived());
					        assertTrue(afterReport.isArchived());
					        assertEquals(dbReportShouldBeModified, dbReport.isArchived());

					        // advancedData should be cleared when report is archived
					        assertTrue(beforeReport.hasAdvancedData());
					        assertFalse(afterReport.hasAdvancedData());
					        assertEquals(dbReportShouldBeModified, !dbReport.hasAdvancedData());
				        } else {
					        assertTrue(beforeReport.isArchived());
					        assertTrue(afterReport.isArchived());
					        assertTrue(dbReport.isArchived());

					        assertFalse(beforeReport.hasAdvancedData());
					        assertFalse(afterReport.hasAdvancedData());
					        assertFalse(dbReport.hasAdvancedData());
				        }

				        AssertionUtils.assertDeepEquals(beforeReport, afterReport, "archived", "advancedData");
				        AssertionUtils.assertDeepEquals(beforeReport, dbReport, "archived", "advancedData");

				        if (!onArchivedReport) {
					        ArgumentCaptor<Report> broadcastReportDataChangedReport = ArgumentCaptor
					                .forClass(Report.class);
					        verify(rm, times(1)).broadcastReportDataChanged(broadcastReportDataChangedReport.capture());
					        assertTrue(broadcastReportDataChangedReport.getValue() == afterReport,
					                () -> "the same (unique) report instance should be used for broadcast");
				        } else {
					        verify(rm, times(0)).broadcastReportDataChanged(any(Report.class));
				        }

				        if (staff != null) {
					        assertNotNull(sentStaffMessage);
				        } else {
					        assertNull(sentStaffMessage);
				        }

				        if (staff != null && !bungee) {
					        ArchiveBungeeNotification bungeeArchiveNotif = TestsBungeeManager
					                .assertCalledOnceSendPluginNotificationToAll(bm, ArchiveBungeeNotification.class);
					        assertEquals(staff.getUniqueId().toString(), bungeeArchiveNotif.staffUniqueId);
					        assertEquals(beforeReport.getId(), bungeeArchiveNotif.reportId);
				        } else {
							TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm, ArchiveBungeeNotification.class);
				        }
			        });
		}

	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#unarchive(fr.mrtigreroux.tigerreports.objects.users.User, boolean, fr.mrtigreroux.tigerreports.data.database.Database)}.
	 */
	@Nested
	class Unarchive {

		@Test
		void test1() {
			testUnarchive(true, true, false);
		}

		@Test
		void test2() {
			testUnarchive(true, false, false);
		}

		@Test
		void test3() {
			testUnarchive(true, true, true);
		}

		@Test
		void test4() {
			testUnarchive(true, false, true);
		}

		@Test
		void test5() {
			testUnarchive(false, true, false);
		}

		@Test
		void test6() {
			testUnarchive(false, false, false);
		}

		@Test
		void test7() {
			testUnarchive(false, true, true);
		}

		@Test
		void test8() {
			testUnarchive(false, false, true);
		}

		void testUnarchive(boolean onArchivedReport, boolean validStaff, boolean bungee) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			ReportsManager rm = spy(ReportsManager.class);
			User staff = validStaff
			        ? new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"))
			        : null;

			UsersManager um = mock(UsersManager.class);
			VaultManager vm = mock(VaultManager.class);
			BungeeManager bm = mock(BungeeManager.class);

			testReportAction(onArchivedReport ? TestsReport.getSuccessfulArchiveReportAction(db, rm, vm, bm)
			        : TestsReport.NO_REPORT_ACTION, (r) -> {
				        r.unarchive(staff, bungee, db, rm, vm, bm);
			        }, taskScheduler, db, um, vm,
			        (Report beforeReport, Report afterReport, Report dbReport, Object sentStaffMessage) -> {
				        assertNotNull(beforeReport);
				        assertNotNull(afterReport);
				        assertNotNull(dbReport);

				        boolean dbReportShouldBeModified = staff != null && !bungee;

				        if (onArchivedReport) {
					        assertTrue(beforeReport.isArchived());
					        assertFalse(afterReport.isArchived());
					        assertEquals(dbReportShouldBeModified, !dbReport.isArchived());

					        // advancedData should still be missing locally, but present in db after unarchive
					        assertFalse(beforeReport.hasAdvancedData());
					        assertFalse(afterReport.hasAdvancedData());
					        assertEquals(dbReportShouldBeModified, dbReport.hasAdvancedData());
				        } else {
					        assertFalse(beforeReport.isArchived());
					        assertFalse(afterReport.isArchived());
					        assertFalse(dbReport.isArchived());

					        // advancedData should still be missing locally, but present in db after unarchive
					        assertTrue(beforeReport.hasAdvancedData());
					        assertTrue(afterReport.hasAdvancedData());
					        assertTrue(dbReport.hasAdvancedData());
				        }

				        AssertionUtils.assertDeepEquals(beforeReport, afterReport, "archived", "advancedData");
				        AssertionUtils.assertDeepEquals(beforeReport, dbReport, "archived", "advancedData");

				        if (onArchivedReport) {
					        ArgumentCaptor<Report> broadcastReportDataChangedReport = ArgumentCaptor
					                .forClass(Report.class);
					        verify(rm, times(1)).broadcastReportDataChanged(broadcastReportDataChangedReport.capture());
					        assertTrue(broadcastReportDataChangedReport.getValue() == afterReport,
					                () -> "the same (unique) report instance should be used for broadcast");
				        } else {
					        verify(rm, times(0)).broadcastReportDataChanged(any(Report.class));
				        }

				        if (staff != null) {
					        assertNotNull(sentStaffMessage);
				        } else {
					        assertNull(sentStaffMessage);
				        }

				        if (staff != null && !bungee) {
					        UnarchiveBungeeNotification unarchiveBungeeNotif = TestsBungeeManager
					                .assertCalledOnceSendPluginNotificationToAll(bm, UnarchiveBungeeNotification.class);
					        assertEquals(staff.getUniqueId().toString(), unarchiveBungeeNotif.staffUniqueId);
					        assertEquals(beforeReport.getId(), unarchiveBungeeNotif.reportId);
				        } else {
					        verify(bm, times(0)).sendPluginNotificationToAll(any(UnarchiveBungeeNotification.class));
				        }
			        });
		}

	}

	/**
	 * Test method for
	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#deleteFromArchives(fr.mrtigreroux.tigerreports.objects.users.User, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.ReportsManager, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.bungee.BungeeManager)}.
	 */
	@Nested
	class DeleteFromArchives {

		@Test
		void test1() {
			testDeleteFromArchives(true, true, false);
		}

		@Test
		void test2() {
			testDeleteFromArchives(true, false, false);
		}

		@Test
		void test3() {
			testDeleteFromArchives(true, true, true);
		}

		@Test
		void test4() {
			testDeleteFromArchives(true, false, true);
		}

		@Test
		void test5() {
			testDeleteFromArchives(false, true, false);
		}

		@Test
		void test6() {
			testDeleteFromArchives(false, false, false);
		}

		@Test
		void test7() {
			testDeleteFromArchives(false, true, true);
		}

		@Test
		void test8() {
			testDeleteFromArchives(false, false, true);
		}

		void testDeleteFromArchives(boolean onArchivedReport, boolean validStaff, boolean bungee) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			ReportsManager rm = spy(ReportsManager.class);
			User staff = validStaff
			        ? new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"))
			        : null;

			UsersManager um = mock(UsersManager.class);
			VaultManager vm = mock(VaultManager.class);
			BungeeManager bm = mock(BungeeManager.class);

			testReportAction(onArchivedReport ? TestsReport.getSuccessfulArchiveReportAction(db, rm, vm, bm)
			        : TestsReport.NO_REPORT_ACTION, (r) -> {
				        r.deleteFromArchives(staff, bungee, db, taskScheduler, rm, vm, bm);
			        }, taskScheduler, db, um, vm,
			        (Report beforeReport, Report afterReport, Report dbReport, Object sentStaffMessage) -> {
				        assertNotNull(beforeReport);
				        assertNotNull(afterReport);

				        if (onArchivedReport) {
					        boolean dbReportShouldBeModified = staff != null && !bungee;
					        if (dbReportShouldBeModified) {
						        assertNull(dbReport);
					        } else {
						        assertNotNull(dbReport);
					        }

					        if (staff != null) {
						        assertNotNull(sentStaffMessage);
					        } else {
						        assertNull(sentStaffMessage);
					        }

					        if (staff != null && !bungee) {
								DeleteBungeeNotification bungeeNotif = TestsBungeeManager
					                .assertCalledOnceSendPluginNotificationToAll(bm, DeleteBungeeNotification.class);
						        assertEquals(staff.getUniqueId().toString(), bungeeNotif.staffUniqueId);
						        assertEquals(beforeReport.getBasicDataAsString(),
						                bungeeNotif.reportBasicDataString);
					        } else {
								TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm, DeleteBungeeNotification.class);
					        }

					        verify(rm, times(1)).reportIsDeleted(beforeReport.getId());
				        } else {
					        assertNotNull(dbReport);
					        assertNull(sentStaffMessage);
							TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm, DeleteBungeeNotification.class);
					        verify(rm, times(0)).reportIsDeleted(beforeReport.getId());
				        }
			        });
		}

	}

	/**
	 * Test method for
	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#delete(fr.mrtigreroux.tigerreports.objects.users.User, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.ReportsManager, fr.mrtigreroux.tigerreports.managers.VaultManager, fr.mrtigreroux.tigerreports.bungee.BungeeManager)}.
	 */
	@Nested
	class Delete {

		@Test
		void test1() {
			testDelete(true, false);
		}

		@Test
		void test2() {
			testDelete(false, false);
		}

		@Test
		void test3() {
			testDelete(true, true);
		}

		@Test
		void test4() {
			testDelete(false, true);
		}

		void testDelete(boolean validStaff, boolean bungee) {
			TestsTaskScheduler taskScheduler = TestsTaskScheduler.getCleanMainTaskScheduler();
			Database db = TestsDatabaseManager.getCleanMainDatabase(taskScheduler);

			ReportsManager rm = spy(ReportsManager.class);
			User staff = validStaff
			        ? new User(UUID.randomUUID(), "staff display name", new OfflineUserData("staff name"))
			        : null;

			UsersManager um = mock(UsersManager.class);
			VaultManager vm = mock(VaultManager.class);
			BungeeManager bm = mock(BungeeManager.class);

			testReportAction((r) -> {
				r.delete(staff, bungee, db, taskScheduler, rm, vm, bm);
			}, taskScheduler, db, um, vm,
			        (Report beforeReport, Report afterReport, Report dbReport, Object sentStaffMessage) -> {
				        assertNotNull(beforeReport);
				        assertNotNull(afterReport);

				        boolean dbReportShouldBeModified = staff != null && !bungee;
				        if (dbReportShouldBeModified) {
					        assertNull(dbReport);
				        } else {
					        assertNotNull(dbReport);
				        }

				        if (staff != null) {
					        assertNotNull(sentStaffMessage);
				        } else {
					        assertNull(sentStaffMessage);
				        }

				        if (staff != null && !bungee) {
							DeleteBungeeNotification bungeeNotif = TestsBungeeManager
					                .assertCalledOnceSendPluginNotificationToAll(bm, DeleteBungeeNotification.class);
							assertEquals(staff.getUniqueId().toString(), bungeeNotif.staffUniqueId);
							assertEquals(beforeReport.getBasicDataAsString(),
						                bungeeNotif.reportBasicDataString);
				        } else {
							TestsBungeeManager.assertNeverCalledSendPluginNotificationToAll(bm, DeleteBungeeNotification.class);
				        }

				        verify(rm, times(1)).reportIsDeleted(beforeReport.getId());
			        });
		}

	}

//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#update(java.util.Map, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testUpdateMapOfStringObjectBooleanDatabaseTaskSchedulerUsersManagerResultCallbackOfBoolean() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for
//	 * {@link fr.mrtigreroux.tigerreports.objects.reports.Report#update(java.util.Map, boolean, boolean, fr.mrtigreroux.tigerreports.data.database.Database, fr.mrtigreroux.tigerreports.tasks.TaskScheduler, fr.mrtigreroux.tigerreports.managers.UsersManager, fr.mrtigreroux.tigerreports.tasks.ResultCallback)}.
//	 */
//	@Test
//	void testUpdateMapOfStringObjectBooleanBooleanDatabaseTaskSchedulerUsersManagerResultCallbackOfBoolean() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#getBasicDataAsString()}.
//	 */
//	@Test
//	void testGetBasicDataAsString() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#parseBasicDataFromString(java.lang.String)}.
//	 */
//	@Test
//	void testParseBasicDataFromString() {
//		fail("Not yet implemented");
//	}

	@FunctionalInterface
	interface ReportActionResultCallback {

		void afterReportActionExecuted(Report beforeReport, Report afterReport, Report dbReport,
		        Object sentStaffMessage);

	}

	void testReportAction(Consumer<Report> reportAction, TestsTaskScheduler taskScheduler, Database db, UsersManager um,
	        VaultManager vm, ReportActionResultCallback resultCallback) {
		testReportAction(TestsReport.NO_REPORT_ACTION, reportAction, taskScheduler, db, um, vm, resultCallback);
	}

	/**
	 * Test a report action. NB: um, rm and vm interactions are cleared before report action in order to be able to safely use {@link Mockito#verify(Object)}.
	 * 
	 * @param beforeReportAction
	 * @param reportAction
	 * @param taskScheduler
	 * @param db
	 * @param um
	 * @param rm
	 * @param vm
	 * @param resultCallback
	 */
	void testReportAction(Consumer<Report> beforeReportAction, Consumer<Report> reportAction,
	        TestsTaskScheduler taskScheduler, Database db, UsersManager um, VaultManager vm,
	        ReportActionResultCallback resultCallback) {
		Holder<Report> beforeReport = new Holder<>();
		Holder<Report> afterReport = new Holder<>();
		Holder<Report> dbReport = new Holder<>();
		Holder<Object> sentStaffMessage = new Holder<>();

		ReportsManager rm = new ReportsManager();

		assertTrue(taskScheduler.runTaskAndWait((tc) -> {
			TestsReportUtils.createRandomReportAsynchronously(true, true, taskScheduler, db, (r) -> {
				beforeReportAction.accept(r);
				((TestsSQLite) db).whenNoAsyncUpdate(() -> { // wait for report action done before continuing
					LOGGER.debug(() -> "testReportAction(): report actions executed");
					beforeReport.set(r.deepClone());
					clearInvocations(um, vm);

					LOGGER.debug(() -> "testReportAction(): before report action");
					TestsReport.mockSendStaffMessageAndRunReportAction(r, vm, sentStaffMessage, reportAction);
					((TestsSQLite) db).whenNoAsyncUpdate(() -> { // wait for report action done before continuing
						LOGGER.debug(() -> "testReportAction(): after report action");
						afterReport.set(r);
						TestsReportUtils.getReportByIdAsynchronously(r, taskScheduler, db, um, rm, (dbResult) -> {
							LOGGER.debug(() -> "testReportAction(): getReportByIdAsynchronously result received");
							dbReport.set(dbResult);
							tc.setDone();
						});
					});
				});
			});
		}, TestsTaskScheduler.DEFAULT_TIMEOUT));
		assertTrue(((TestsSQLite) db).noPendingAsyncUpdateAndNoCallback());
		assertNotNull(beforeReport.get());

		resultCallback.afterReportActionExecuted(beforeReport.get(), afterReport.get(), dbReport.get(),
		        sentStaffMessage.get());
	}

	/**
	 * Test method for {@link fr.mrtigreroux.tigerreports.objects.reports.Report#clone()}.
	 */
	@Test
	void testClone() {
		int reportId = 5;

		Status status = Status.WAITING;
		User staff = null;
		StatusDetails statusDetails = StatusDetails.from(status, staff);

		Appreciation appreciation = Appreciation.NONE;
		AppreciationDetails appreciationDetails = AppreciationDetails.from(appreciation, null);
		String date = DatetimeUtils.getNowDatetime();

		Player reportedPlayer = mock(Player.class);
		User reported = new User(UUID.randomUUID(), null, new OnlineUserData(reportedPlayer));

		Player reporterPlayer = mock(Player.class);
		List<User> reporters = Arrays.asList(new User(UUID.randomUUID(), null, new OnlineUserData(reporterPlayer)));

		String reason = "report reason";
		boolean archived = false;

		String reportedUUID = reported.getUniqueId().toString();
		String reportersUUID = TestsReport.getReportersStr(reporters);

		String reporterIP = "/127.0.0.145";
		String reporterLocation = "localhost/world/12.5/15.6/17.5/17.2/14.258";
		SavedMessage[] reporterMessages = new SavedMessage[] { new SavedMessage("Hello"), new SavedMessage("Hello2") };

		String reportedIP = "/127.0.0.985";
		String reportedLocation = "server2/world2/12.5/18.6/14.5/17.2/14.258";
		SavedMessage[] reportedMessages = new SavedMessage[0];
		String reportedGamemode = GameMode.SURVIVAL.toString().toLowerCase();
		int reportedOnGround = 1;
		int reportedSneaking = 1;
		int reportedSprinting = 1;
		String reportedHealth = "18/20";
		int reportedFood = 14;
		String reportedEffects = "HEAL:2/32";

		Map<String, Object> reportData = new HashMap<>();
		reportData.put(Report.REPORT_ID, reportId);
		reportData.put(Report.STATUS, statusDetails.toString());
		reportData.put(Report.APPRECIATION, appreciationDetails.toString());
		reportData.put(Report.DATE, date);
		reportData.put(Report.REPORTED_UUID, reportedUUID);
		reportData.put(Report.REPORTER_UUID, reportersUUID);
		reportData.put(Report.REASON, reason);

		Map<String, Object> reportAdvancedData = new HashMap<>();
		reportAdvancedData.put(Report.AdvancedData.REPORTER_IP, reporterIP);
		reportAdvancedData.put(Report.AdvancedData.REPORTER_LOCATION, reporterLocation);
		reportAdvancedData.put(Report.AdvancedData.REPORTER_MESSAGES,
		        Report.AdvancedData.serializeMessages(Arrays.asList(reporterMessages)));

		reportAdvancedData.put(Report.AdvancedData.REPORTED_IP, reportedIP);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_LOCATION, reportedLocation);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_MESSAGES,
		        Report.AdvancedData.serializeMessages(Arrays.asList(reportedMessages)));
		reportAdvancedData.put(Report.AdvancedData.REPORTED_GAMEMODE, reportedGamemode);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_ON_GROUND, reportedOnGround);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_SNEAK, reportedSneaking);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_SPRINT, reportedSprinting);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_HEALTH, reportedHealth);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_FOOD, reportedFood);
		reportAdvancedData.put(Report.AdvancedData.REPORTED_EFFECTS, reportedEffects);

		reportData.putAll(reportAdvancedData);

		Database db = mock(Database.class);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		UsersManager um = mock(UsersManager.class);

		TestsReport.setupUMForAsynchronouslyFrom(um, reported, reporters);

		Holder<Boolean> resultCallbackCalled = new Holder<>(false);
		// Use asynchronouslyFrom to be able to put advancedData in order to be able to check the right copy of advancedData.
		Report.asynchronouslyFrom(reportData, archived, true, db, taskScheduler, um, (r) -> {
			resultCallbackCalled.set(true);
			AssertionUtils.assertDeeplyCloneable(r, "reported", "advancedData"); // advancedData is immutable, reported (users) is unique in the program
		});
		assertTrue(resultCallbackCalled.get());
	}

}
