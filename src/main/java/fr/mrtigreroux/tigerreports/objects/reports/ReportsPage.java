package fr.mrtigreroux.tigerreports.objects.reports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;

/**
 * @author MrTigreroux
 */
public class ReportsPage implements Report.ReportListener {

	private static final Logger LOGGER = Logger.fromClass(ReportsPage.class);

	public static final int PAGE_MAX_REPORTS_AMOUNT = 27;

	public final ReportsPageCharacteristics characteristics;
	private final ReportsManager rm;
	private final Database db;
	private final TaskScheduler taskScheduler;
	private final UsersManager um;
	private final List<Integer> reportsId = new ArrayList<>();
	private final Set<ReportsPageListener> listeners = new HashSet<>();
	private boolean changed = false;

	public ReportsPage(ReportsPageCharacteristics characteristics, ReportsManager rm, Database db,
	        TaskScheduler taskScheduler, UsersManager um) {
		this.characteristics = Objects.requireNonNull(characteristics);
		this.rm = rm;
		this.db = db;
		this.taskScheduler = taskScheduler;
		this.um = um;
	}

	public interface ReportsPageListener {
		void onReportsPageChange(int page);
	}

	public boolean addListener(ReportsPageListener listener, Database db, TaskScheduler taskScheduler,
	        ReportsManager rm, UsersManager um) {
		LOGGER.info(() -> characteristics + ": addListener(" + listener + ")");

		boolean wasEmpty = listeners.isEmpty();
		boolean success = listeners.add(listener);

		if (wasEmpty) { // Data is potentially expired or has never been collected
			LOGGER.info(() -> characteristics + ": addListener(" + listener
			        + "): updateDataWhenPossible() and start MenuUpdater");
			rm.updateDataWhenPossible(db, taskScheduler, um);
			MenuUpdater.startIfNeeded(rm, db, taskScheduler, um);
		}
		return success;
	}

	public boolean removeListener(ReportsPageListener listener, ReportsManager rm) {
		LOGGER.info(() -> characteristics + ": removeListener(" + listener + ")");
		boolean success = listeners.remove(listener);
//		if (listeners.isEmpty()) {
//			destroy(rm);
//		}
		// destroyed at the end of rm.updateData

		return success;
	}

	public void updateReportAtIndex(int index, int reportId, ReportsManager rm) {
		checkReportIndex(index, true);

		Integer previousReportId = index < reportsId.size() ? reportsId.get(index) : null;
		if (previousReportId == null || reportId != previousReportId) {
			if (previousReportId != null) {
				LOGGER.info(() -> characteristics + ": updateReportAtIndex(): remove report listener of "
				        + previousReportId);
				rm.removeReportListener(previousReportId, this);
			}

			if (index < reportsId.size()) {
				reportsId.set(index, reportId);
			} else if (index == reportsId.size()) {
				reportsId.add(reportId);
			} else {
				throw new IllegalStateException("index > reportsIds size");
			}
			changed = true;

			rm.addReportListener(reportId, this, false, null, null, null); // Collection will be done by ReportsManager#updateData
		}
	}

	private void checkReportIndex(int index, boolean allowIndexForNextPage) throws IndexOutOfBoundsException {
		int maxAllowed = PAGE_MAX_REPORTS_AMOUNT;
		if (!allowIndexForNextPage) {
			maxAllowed--;
		}
		if (index < 0 || index > maxAllowed) {
			throw new IndexOutOfBoundsException("Index " + index + " is out of [0; " + maxAllowed + "]");
		}
	}

	public void removeOldReports(int maxReportIndex, ReportsManager rm) {
		LOGGER.info(() -> characteristics + ": removeOldReports(" + maxReportIndex + ")");
		if (!reportsId.isEmpty()) {
			int oldLastIndex = reportsId.size() - 1;

			// Remove previous remaining reports starting from the end of the list.
			for (int oldIndex = oldLastIndex; oldIndex > maxReportIndex; oldIndex--) {
				removeReportAtIndex(oldIndex, rm);
			}
		}
	}

	public void removeReportAtIndex(int index, ReportsManager rm) {
		if (index < 0 || index >= reportsId.size()) {
			return;
		}

		Integer previousReportId = reportsId.get(index);
		reportsId.remove(index);
		if (rm != null && previousReportId != null && !reportsId.contains(previousReportId)) { // previousReportId can still be in reportsId at other index
			LOGGER.info(
			        () -> characteristics + ": removeReportAtIndex(): remove report listener of " + previousReportId);
			rm.removeReportListener(previousReportId, this);
		}
		changed = true;
	}

	public int getReportIdAtIndex(int index) {
		checkReportIndex(index, false);
		Integer reportId = CollectionUtils.getElementAtIndex(reportsId, index);
		return reportId != null ? reportId : -1;
	}

	public List<Integer> getReportsId() {
		List<Integer> pageReportsId = reportsId;
		if (pageReportsId.size() > PAGE_MAX_REPORTS_AMOUNT) {
			pageReportsId = reportsId.subList(0, PAGE_MAX_REPORTS_AMOUNT);
		}
		return new ArrayList<>(pageReportsId);
	}

	public boolean isEmpty() {
		return reportsId.isEmpty();
	}

	public boolean isNextPageNotEmpty() {
		return CollectionUtils.getElementAtIndex(reportsId, PAGE_MAX_REPORTS_AMOUNT) != null;
	}

	public int getPage() {
		return characteristics.page;
	}

	@Override
	public void onReportDataChange(Report r) {
		processReportChange(r);
	}

	@Override
	public void onReportDelete(int reportId) {
		processReportChange(null);
	}

	private void processReportChange(Report r) {
		if (rm.isPendingDataUpdate()) { // group all changes to avoid several page change notifications
			LOGGER.debug(() -> characteristics + ": processReportChange(): changed set to true");
			changed = true;
		} else {
			if (couldContainReport(r)) { // report is still in the page
				LOGGER.debug(() -> characteristics + ": processReportChange(): broadcastPageChanged()");
				broadcastPageChanged();
			} else { // need to collect the eventually new report coming from the old next page
				LOGGER.debug(() -> characteristics + ": processReportChange(): rm.updateDataWhenPossible()");
				rm.updateDataWhenPossible(db, taskScheduler, um);
			}
		}
	}

	boolean couldContainReport(Report r) {
		return r != null && characteristics.reportsCharacteristics.archived == r.isArchived()
		        && (characteristics.reportsCharacteristics.reportedUUID == null
		                || characteristics.reportsCharacteristics.reportedUUID.equals(r.getReportedUniqueId()))
		        && (characteristics.reportsCharacteristics.reporterUUID == null
		                || r.getReportersUUID().contains(characteristics.reportsCharacteristics.reporterUUID));
	}

	public void broadcastIfPageChanged() {
		if (changed) {
			changed = false;

			LOGGER.info(() -> characteristics + ": broadcastIfPageChanged(): page changed, broadcast");
			broadcastPageChanged();
		} else {
			LOGGER.info(() -> characteristics + ": broadcastIfPageChanged(): page has not changed");
		}
	}

	private void broadcastPageChanged() {
		if (listeners != null) {
			for (ReportsPageListener l : listeners) {
				l.onReportsPageChange(characteristics.page);
			}
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(characteristics);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ReportsPage)) {
			return false;
		}
		ReportsPage other = (ReportsPage) obj;
		return Objects.equals(characteristics, other.characteristics);
	}

	@Override
	public String toString() {
		return "ReportsPage [characteristics=" + characteristics + ", reportsIds=" + reportsId + "]";
	}

	public boolean hasListener() {
		return !listeners.isEmpty();
	}

	public void destroy(ReportsManager rm) {
		LOGGER.info(() -> characteristics + ": destroy()");
		for (Integer reportId : reportsId) {
			if (reportId != null) {
				rm.removeReportListener(reportId, this);
			}
		}
		rm.removeReportsPageFromCache(characteristics);
		listeners.clear();
		reportsId.clear();
	}

	/**
	 * First report index of the page in the database.
	 * 
	 * @param page starting with 1
	 * @return
	 */
	public static int firstGlobalIndexOfPage(int page) {
		return (page - 1) * PAGE_MAX_REPORTS_AMOUNT;
	}

	public static int lastGlobalIndexOfPage(int page) {
		return firstGlobalIndexOfPage(page) + ReportsPage.PAGE_MAX_REPORTS_AMOUNT - 1;
	}

	public static boolean isGlobalIndexInPage(int index, int page) {
		return firstGlobalIndexOfPage(page) <= index && index <= lastGlobalIndexOfPage(page);
	}

}