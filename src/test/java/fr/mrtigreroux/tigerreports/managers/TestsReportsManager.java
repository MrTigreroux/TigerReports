package fr.mrtigreroux.tigerreports.managers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import fr.mrtigreroux.tigerreports.data.Holder;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPage;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.runnables.MenuUpdater;

/**
 * @author MrTigreroux
 */
public class TestsReportsManager {

	private static final Logger LOGGER = Logger.fromClass(TestsReportsManager.class);
	public static final Map<ReportsManager, Integer> waitingUpdateDataEndTaskIdByReportsManager = new HashMap<>();

	public static void setupReportsManagerForAddListenerWithoutUpdateDataAndMenuUpdater(ReportsManager rm,
	        MockedStatic<MenuUpdater> menuUpdaterStaticMock, Holder<Integer> menuUpdaterStartIfNeededCalledTimes) {
		doNothing().when(rm)
		        .updateDataWhenPossible(any(Database.class), any(TaskScheduler.class), any(UsersManager.class));
		menuUpdaterStaticMock.when(() -> MenuUpdater.startIfNeeded(any(ReportsManager.class), any(Database.class),
		        any(TaskScheduler.class), any(UsersManager.class))).thenAnswer((Answer<Void>) answer -> {
			        if (menuUpdaterStartIfNeededCalledTimes != null) {
				        menuUpdaterStartIfNeededCalledTimes.set(menuUpdaterStartIfNeededCalledTimes.get() + 1);
			        }
			        return null;
		        });
	}

	public static class FakeReportListener {

		private final Holder<Report> reportH;
		private final Report report;

		public final AtomicInteger reportDataChangeNotifications = new AtomicInteger();
		public final AtomicInteger reportDeleteNotifications = new AtomicInteger();

		public final Report.ReportListener reportListener = new Report.ReportListener() {

			@Override
			public void onReportDelete(int reportId) {
				if (getReport() != null && getReport().getId() == reportId) {
					reportDeleteNotifications.incrementAndGet();
				}
			}

			@Override
			public void onReportDataChange(Report r) {
				if (r != null && r.equals(getReport())) {
					reportDataChangeNotifications.incrementAndGet();
				}
			}

		};

		public FakeReportListener(Holder<Report> reportH) {
			this.reportH = Objects.requireNonNull(reportH);
			report = null;
		}

		public FakeReportListener(Report report) {
			reportH = null;
			this.report = Objects.requireNonNull(report);
		}

		public Report getReport() {
			return reportH != null ? reportH.get() : report;
		}

	}

	public static class FakeReportsPageListener {

		private final int page;
		public final AtomicInteger pageChangeNotifications = new AtomicInteger();
		private boolean listening = true;

		public final ReportsPage.ReportsPageListener reportsPageListener = new ReportsPage.ReportsPageListener() {

			@Override
			public void onReportsPageChange(int page) {
				if (listening && FakeReportsPageListener.this.page == page) {
					pageChangeNotifications.incrementAndGet();
				}
			}

		};

		public FakeReportsPageListener(int page) {
			this.page = page;
		}

		public void setListening(boolean state) {
			listening = state;
		}

	}

	public static void whenReportsManagerIsNotUpdatingData(ReportsManager rm, TaskScheduler ts, long checkInterval, Runnable run) {
		if (!rm.isPendingDataUpdate()) {
			LOGGER.debug(() -> "whenReportsManagerIsNotUpdatingData(): rm is not pending data update, no periodic check needed");
			run.run();
			return;
		}
		LOGGER.debug(() -> "whenReportsManagerIsNotUpdatingData(): rm is pending data update, will periodically check it...");
		waitingUpdateDataEndTaskIdByReportsManager.put(rm, ts.runTaskRepeatedly(checkInterval, checkInterval, () -> {
			if (!rm.isPendingDataUpdate()) {
				LOGGER.debug(() -> "whenReportsManagerIsNotUpdatingData(): rm is no longer pending data update");
				Integer curTaskId = waitingUpdateDataEndTaskIdByReportsManager.get(rm);
				if (curTaskId != null) {
					ts.cancelTask(curTaskId);
				}
				run.run();
			} else {
				LOGGER.debug(() -> "whenReportsManagerIsNotUpdatingData(): rm is still pending data update...");
			}
		}));
	}

}
