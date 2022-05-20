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
	private final List<Integer> reportsId = new ArrayList<>();
	private final Set<ReportsPageListener> listeners = new HashSet<>();
	private boolean changed = false;

	public ReportsPage(ReportsPageCharacteristics characteristics) {
		this.characteristics = Objects.requireNonNull(characteristics);
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
		if (listeners.isEmpty()) {
			destroy(rm);
		}

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
			throw new IndexOutOfBoundsException("Index " + index + " is out of [0, " + maxAllowed + "]");
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

	public boolean removeReportAtIndex(int index, ReportsManager rm) {
		if (index < 0 || index >= reportsId.size()) {
			return false;
		}

		Integer previousReportId = reportsId.get(index);
		reportsId.remove(index);
		if (previousReportId != null && !reportsId.contains(previousReportId)) { // previousReportId can still be in reportsIds at other index
			LOGGER.info(
			        () -> characteristics + ": removeReportAtIndex(): remove report listener of " + previousReportId);
			rm.removeReportListener(previousReportId, this);
		}
		changed = true;
		return true;
	}

	public int getReportIdAtIndex(int index) {
		checkReportIndex(index, false);
		Integer reportId = CollectionUtils.getElementAtIndex(reportsId, index);
		return reportId != null ? reportId : -1;
	}

	public List<Integer> getReportsId() {
		List<Integer> pageReportsIds = reportsId;
		if (pageReportsIds.size() > PAGE_MAX_REPORTS_AMOUNT) {
			pageReportsIds = reportsId.subList(0, PAGE_MAX_REPORTS_AMOUNT);
		}
		return new ArrayList<>(reportsId);
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
		changed = true;
	}

	@Override
	public void onReportDelete(int reportId) {
		changed = true;
	}

	public void broadcastIfPageChanged() {
		if (changed) {
			changed = false;

			LOGGER.info(() -> characteristics + ": broadcastIfPageChanged(): page changed, broadcast");
			int page = characteristics.page;
			if (listeners != null) {
				for (ReportsPageListener l : new HashSet<>(listeners)) {
					l.onReportsPageChange(page);
				}
			}
		} else {
			LOGGER.info(() -> characteristics + ": broadcastIfPageChanged(): page has not changed");
		}
		changed = false;
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

	public void destroy(ReportsManager rm) {
		for (Integer reportId : reportsId) {
			if (reportId != null) {
				rm.removeReportListener(reportId, this);
			}
		}
		rm.removeReportsPage(characteristics);
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

}