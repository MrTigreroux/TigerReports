package fr.mrtigreroux.tigerreports.objects.reports;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import fr.mrtigreroux.tigerreports.TestsBukkit;
import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.Holder;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report.AdvancedData;
import fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails;
import fr.mrtigreroux.tigerreports.objects.users.OfflineUserData;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.objects.users.User.SavedMessage;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.RandomUtils;
import fr.mrtigreroux.tigerreports.utils.SerializationUtils;
import fr.mrtigreroux.tigerreports.utils.TestsMessageUtils;
import fr.mrtigreroux.tigerreports.utils.TestsUserUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */
public class TestsReport {

	private static final Logger LOGGER = Logger.fromClass(TestsReport.class);

	public static final List<World> worlds = new ArrayList<>();
	public static final Consumer<Report> NO_REPORT_ACTION = (r) -> { /* nothing */ };

	public static String getReportersStr(List<User> reporters) {
		return reporters.stream()
		        .map((u) -> u.getUniqueId().toString())
		        .collect(Collectors.joining(Report.REPORTERS_SEPARATOR));
	}

	public static String[] getReportersUUID(List<User> reporters) {
		return reporters.stream().map((u) -> u.getUniqueId().toString()).toArray(String[]::new);
	}

	/**
	 * Does not use {@link AdvancedData#serializeConfigEffects(java.util.Collection)} but a constant string.
	 * 
	 * @param reportData
	 * @param bm
	 */
	public static void fillMissingWithRandomReportedAdvancedData(Map<String, Object> reportData, BungeeManager bm) {
		Random r = new Random();
		putIfMissing(reportData, Report.AdvancedData.REPORTED_IP, TestsReport.getRandomIPAddress(r));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_LOCATION,
		        SerializationUtils.serializeLocation(TestsReport.mockRandomLocation(), bm));

		List<SavedMessage> messages = getRandomSavedMessages(r);
		putIfMissing(reportData, Report.AdvancedData.REPORTED_MESSAGES, AdvancedData.serializeMessages(messages));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_GAMEMODE, AdvancedData.serializeGamemode(GameMode.SURVIVAL));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_ON_GROUND, RandomUtils.getRandomInt(r, 0, 1));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_SNEAK, RandomUtils.getRandomInt(r, 0, 1));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_SPRINT, RandomUtils.getRandomInt(r, 0, 1));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_HEALTH, RandomUtils.getRandomInt(r, 0, 20) + "/20");
		putIfMissing(reportData, Report.AdvancedData.REPORTED_FOOD, RandomUtils.getRandomInt(r, 0, 20));
		putIfMissing(reportData, Report.AdvancedData.REPORTED_EFFECTS, r.nextBoolean() ? "HEAL:2/32" : null);
	}

	private static void putIfMissing(Map<String, Object> reportData, String key, Object value) {
		if (!reportData.containsKey(key)) {
			reportData.put(key, value);
		}
	}

	public static List<SavedMessage> getRandomSavedMessages(Random r) {
		List<SavedMessage> messages = new ArrayList<>();
		int messagesAmount = RandomUtils.getRandomInt(r, 0, 7);
		for (int i = 0; i < messagesAmount; i++) {
			messages.add(new SavedMessage("Msg " + i));
		}
		return messages;
	}

	public static String getRandomIPAddress(Random r) {
		return "/" + RandomUtils.getRandomInt(r, 0, 255) + "." + RandomUtils.getRandomInt(r, 0, 255) + "."
		        + RandomUtils.getRandomInt(r, 0, 255) + "." + RandomUtils.getRandomInt(r, 0, 255);
	}

	public static Location mockRandomLocation() {
		Random r = new Random();
		World world = TestsReport.mockWorld("world" + RandomUtils.getRandomInt(r, 0, 100));
		if (world == null) {
			LOGGER.error("mockRandomLocation(): world = null");
		}
		LOGGER.info(() -> "mockRandomLocation(): world = " + world);
		worlds.add(world); // save world outside because in Location it is saved in weakref, and this save of world should be cleared after test end
		return new Location(world, RandomUtils.getRandomDouble(r, -100, 100), RandomUtils.getRandomDouble(r, -100, 100),
		        RandomUtils.getRandomDouble(r, -100, 100), RandomUtils.getRandomFloat(r, 0, 100),
		        RandomUtils.getRandomFloat(r, 0, 100));
	}

	public static World mockWorld(String worldName) {
		World world = mock(World.class);
		when(world.getName()).thenReturn(worldName);
		return world;
	}

	public static void setupUMForAsynchronouslyFrom(UsersManager um, User reported, List<User> reporters) {
		setupUMForAsynchronouslyFrom(um, reported, reporters, null);
	}

	public static void setupUMForAsynchronouslyFrom(UsersManager um, User reported, List<User> reporters, User staff) {
		String reportedUUID = reported != null ? reported.getUniqueId().toString() : "";
		String reportersUUID = getReportersStr(reporters);

		UUID staffUUID = staff != null ? staff.getUniqueId() : null;
		LOGGER.debug(() -> "setupUMForAsynchronouslyFrom(): reportedUUID = " + reportedUUID + "\n reportersUUID = "
		        + CollectionUtils.toString(reporters) + "\n staffUUID = " + staffUUID);

		doAnswer((invocation) -> {
			String uuid = invocation.getArgument(0);
			ResultCallback<User> callback = invocation.getArgument(3);
			if (reportedUUID.equals(uuid)) {
				callback.onResultReceived(reported);
			} else {
				LOGGER.warn(
				        () -> "setupUMForAsynchronouslyFrom(): getUserAsynchronously(String) unexpected uuid: " + uuid);
				callback.onResultReceived(null);
			}
			return null;
		}).when(um)
		        .getUserByUniqueIdAsynchronously(anyString(), any(Database.class), any(TaskScheduler.class),
		                ArgumentMatchers.<ResultCallback<User>>any());

		doAnswer((invocation) -> {
			Object uuid = invocation.getArgument(0);
			ResultCallback<User> callback = invocation.getArgument(3);
			if (staffUUID != null && staffUUID.equals(uuid)) {
				LOGGER.debug(() -> "setupUMForAsynchronouslyFrom(): getUserAsynchronously(): uuid = staffUUID");
				callback.onResultReceived(staff);
			} else {
				LOGGER.warn(
				        () -> "setupUMForAsynchronouslyFrom(): getUserAsynchronously(UUID) unexpected uuid: " + uuid);
				callback.onResultReceived(null);
			}
			return null;
		}).when(um)
		        .getUserByUniqueIdAsynchronously(any(UUID.class), any(Database.class), any(TaskScheduler.class),
		                ArgumentMatchers.<ResultCallback<User>>any());

		doAnswer((invocation) -> {
			String[] uuids = invocation.getArgument(0);
			ResultCallback<List<User>> callback = invocation.getArgument(3);
			if (Arrays.equals(reportersUUID.split(Report.REPORTERS_SEPARATOR), uuids)) {
				LOGGER.debug(() -> "setupUMForAsynchronouslyFrom(): getUsersAsynchronously(): reportersUUID == uuids");
				callback.onResultReceived(reporters);
			} else {
				LOGGER.warn(() -> "setupUMForAsynchronouslyFrom(): getUsersAsynchronously(): reportersUUID != uuids");
				callback.onResultReceived(null);
			}
			return null;
		}).when(um)
		        .getUsersByUniqueIdAsynchronously(any(String[].class), any(Database.class), any(TaskScheduler.class),
		                ArgumentMatchers.<ResultCallback<List<User>>>any());
	}

	public static void mockSendStaffMessageAndRunReportAction(Report r, VaultManager vm,
	        Holder<Object> sentStaffMessage, Consumer<Report> reportAction) {
		when(r.getReporter().getDisplayName(any(VaultManager.class), anyBoolean())).thenReturn("reporter display name");
		when(r.getReported().getDisplayName(any(VaultManager.class), anyBoolean())).thenReturn("reported display name");

		try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class, Mockito.CALLS_REAL_METHODS);
		        MockedStatic<MessageUtils> messageUtilsMock = mockStatic(MessageUtils.class,
		                Mockito.CALLS_REAL_METHODS)) {
			TestsUserUtils.mockUseDisplayName(userUtilsMock, true);
			TestsMessageUtils.mockSendStaffMessage(messageUtilsMock, sentStaffMessage);
			reportAction.accept(r);
		}
	}

	public static void mockPluginManagerCallEventAndRunReportAction(Report r, Holder<Event> calledEvent,
	        Consumer<Report> reportAction) {
		try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
			TestsBukkit.mockPluginManagerCallEvent(bukkitMock, calledEvent);
			reportAction.accept(r);
		}
	}

	/**
	 * NB: Clear invocations of bm.
	 * 
	 * @param db
	 * @param rm
	 * @param vm
	 * @param bm
	 * @return
	 */
	public static Consumer<Report> getSuccessfulArchiveReportAction(Database db, ReportsManager rm, VaultManager vm,
	        BungeeManager bm) {
		return (report) -> {
			TestsReport.mockSendStaffMessageAndRunReportAction(report, vm, new Holder<Object>(), (r) -> {
				// Use a valid staff and !bungee in order to have a successful archive action
				r.archive(new User(UUID.randomUUID(), "archive staff display name",
				        new OfflineUserData("archive staff name")), false, db, rm, vm, bm);
				clearInvocations(bm, rm);
			});
		};
	}

	public static void changeReportStatus(Report report, Database db) {
		StatusDetails newSD;
		if (report.getStatus() != Status.WAITING) {
			newSD = StatusDetails.from(Status.WAITING, null);
		} else {
			newSD = StatusDetails.from(Status.IMPORTANT, null);
		}
		TestsReport.mockPluginManagerCallEventAndRunReportAction(report, new Holder<>(),
		        (r) -> r.setStatus(newSD, false, db, mock(ReportsManager.class), mock(BungeeManager.class)));
	}

	public static void deleteReport(Report report, TaskScheduler taskScheduler, Database db, ReportsManager rm,
	        BungeeManager bm, VaultManager vm) {
		TestsReport.mockSendStaffMessageAndRunReportAction(report, vm, new Holder<Object>(), (r) -> {
		    // Use a valid staff and !bungee in order to have a successful archive action
		    r.delete(new User(UUID.randomUUID(), "delete staff display name", new OfflineUserData("delete staff name")),
		            false, db, taskScheduler, rm, vm, bm);
		});
	}

	public static void archiveReport(Report report, TaskScheduler taskScheduler, Database db, ReportsManager rm,
	        BungeeManager bm, VaultManager vm) {
		TestsReport.mockSendStaffMessageAndRunReportAction(report, vm, new Holder<Object>(), (r) -> {
		    // Use a valid staff and !bungee in order to have a successful archive action
		    r.archive(new User(UUID.randomUUID(), "archive staff display name",
		            new OfflineUserData("archive staff name")), false, db, rm, vm, bm);
		});
	}

	public static void unarchiveReport(Report report, TaskScheduler taskScheduler, Database db, ReportsManager rm,
	        BungeeManager bm, VaultManager vm) {
		TestsReport.mockSendStaffMessageAndRunReportAction(report, vm, new Holder<Object>(), (r) -> {
		    // Use a valid staff and !bungee in order to have a successful archive action
		    r.unarchive(new User(UUID.randomUUID(), "archive staff display name",
		            new OfflineUserData("archive staff name")), false, db, rm, vm, bm);
		});
	}

}
