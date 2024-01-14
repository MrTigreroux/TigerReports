package fr.mrtigreroux.tigerreports.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.Inventory;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.QueryResult;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.Report.ReportListener;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsCharacteristics;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPage;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPage.ReportsPageListener;
import fr.mrtigreroux.tigerreports.objects.reports.ReportsPageCharacteristics;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.SeveralTasksHandler;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * This class doesn't support multithreading, it must only be accessed by the main thread.
 * 
 * @author MrTigreroux
 */

public class ReportsManager {
    
    private static final Logger LOGGER = Logger.fromClass(ReportsManager.class);
    
    private static final String PAGE_CHARACTERISTICS_FAKE_COLUMN = "page_characteristics";
    public static final int PAGE_MAX_COMMENTS_AMOUNT = 27;
    
    public static final long DATA_UPDATE_COOLDOWN = 1000L; // in ms
    public static final long DATA_UPDATE_MAX_TIME = 5 * 60 * 1000L; // in ms
    
    private long lastDataUpdateTime = 0;
    private boolean pendingDataUpdate = false;
    private boolean pendingDataUpdateWhenPossible = false;
    private boolean dataUpdateRequested = false;
    
    private final Map<Integer, Report> reports = new ConcurrentHashMap<>();
    private final Map<Integer, List<Comment>> reportsComments = new ConcurrentHashMap<>();
    private final Map<ReportsPageCharacteristics, ReportsPage> reportsPages =
            new ConcurrentHashMap<>();
    
    private final Map<Integer, Set<ReportListener>> reportsListeners = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Set<ReportCommentsPageListener>>> reportsCommentsPagesListeners =
            new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Boolean>> reportsCommentsPagesChanged =
            new ConcurrentHashMap<>(); // since last broadcast
    
    public interface ReportCommentsPageListener {
        
        void onReportCommentsPageChanged(int page);
        
    }
    
    public ReportsManager() {}
    
    public void freeUnlistenedReportsFromCache() {
        List<Object> cachedReportsIdBefore;
        if (LOGGER.isInfoLoggable()) {
            cachedReportsIdBefore = new ArrayList<>(reports.keySet());
        } else {
            cachedReportsIdBefore = null;
        }
        
        Set<Integer> reportsId = new HashSet<>(reports.keySet());
        for (int reportId : reportsId) {
            if (!hasReportListener(reportId)) {
                removeReportFromCache(reportId);
            }
        }
        
        LOGGER.info(() -> {
            List<Object> cachedReportsIdAfter = new ArrayList<>(reports.keySet());
            cachedReportsIdBefore.removeAll(cachedReportsIdAfter);
            return "freeUnlistenedReportsFromCache(): removed cached reports: "
                    + CollectionUtils.toString(cachedReportsIdBefore);
        });
    }
    
    public void freeUnlistenedReportsPagesFromCache() {
        for (ReportsPage page : new ArrayList<>(reportsPages.values())) {
            if (!page.hasListener()) {
                page.destroy(this);
            }
        }
    }
    
    public boolean addReportListener(int reportId, ReportListener listener,
            boolean updateDataIfNoListener, Database db, TaskScheduler taskScheduler,
            UsersManager um) {
        return addListener(
                reportId,
                reportsListeners,
                listener,
                updateDataIfNoListener,
                db,
                taskScheduler,
                um
        );
    }
    
    public boolean hasReportListener(int reportId) {
        Set<ReportListener> listeners = reportsListeners.get(reportId);
        return listeners != null && !listeners.isEmpty();
    }
    
    public boolean isReportListenerListeningToReport(int reportId, ReportListener listener) {
        Set<ReportListener> listeners = reportsListeners.get(reportId);
        return listeners != null && listeners.contains(listener);
    }
    
    public boolean removeReportListener(int reportId, ReportListener listener) {
        return removeListener(reportId, reportsListeners, listener);
    }
    
    public ReportsPage getAndListenReportsPage(ReportsCharacteristics reportsCharacteristics,
            int page, ReportsPageListener listener, Database db, TaskScheduler taskScheduler,
            UsersManager um) {
        ReportsPageCharacteristics pageCharac =
                new ReportsPageCharacteristics(reportsCharacteristics, page);
        ReportsPage reportsPage = getReportsPage(pageCharac, true, db, taskScheduler, um);
        // reportsPage could be removed from cache between that 2 lines of code because there would be no listener
        reportsPage.addListener(listener, db, taskScheduler, this, um);
        if (reportsPages.get(pageCharac) != reportsPage) { // Ensures that the returned reports page is kept in cache and is unique
            throw new ConcurrentModificationException(
                    "reportsPages was concurrently modified while getting and listening to "
                            + pageCharac
            );
            // LOGGER.info(
            //			        () -> "getAndListenReportsPage(): cached reports page != local reports page, calls getAndListenReportsPage()");
            //			return getAndListenReportsPage(reportsCharacteristics, page, initialize, listener, db, taskScheduler, um);
        } else {
            return reportsPage;
        }
    }
    
    public ReportsPage getReportsPage(UUID reporterUUID, UUID reportedUUID, boolean archived,
            int page, boolean initialize, Database db, TaskScheduler taskScheduler,
            UsersManager um) {
        ReportsCharacteristics charac =
                new ReportsCharacteristics(reporterUUID, reportedUUID, archived);
        return getReportsPage(charac, page, initialize, db, taskScheduler, um);
    }
    
    public ReportsPage getReportsPage(ReportsCharacteristics reportsCharacteristics, int page,
            boolean initialize, Database db, TaskScheduler taskScheduler, UsersManager um) {
        ReportsPageCharacteristics pageCharac =
                new ReportsPageCharacteristics(reportsCharacteristics, page);
        return getReportsPage(pageCharac, initialize, db, taskScheduler, um);
    }
    
    public ReportsPage getReportsPage(ReportsPageCharacteristics pageCharacteristics,
            boolean initialize, Database db, TaskScheduler taskScheduler, UsersManager um) {
        ReportsPage reportsPage = reportsPages.get(pageCharacteristics);
        if (reportsPage == null && initialize) {
            LOGGER.info(() -> "getReportsPage(): initialize page " + pageCharacteristics);
            reportsPage = new ReportsPage(pageCharacteristics, this, db, taskScheduler, um);
            reportsPages.put(pageCharacteristics, reportsPage);
        }
        return reportsPage;
    }
    
    public void removeReportsPageFromCache(ReportsPageCharacteristics pageCharacteristics) {
        LOGGER.info(() -> "removeReportsPageFromCache(" + pageCharacteristics + ")");
        reportsPages.remove(pageCharacteristics);
    }
    
    private <L> boolean addListener(int key, Map<Integer, Set<L>> keysListeners, L listener,
            boolean updateDataIfNoListener, Database db, TaskScheduler taskScheduler,
            UsersManager um) {
        
        Set<L> listeners = keysListeners.get(key);
        if (listeners == null) {
            listeners = new HashSet<>();
            keysListeners.put(key, listeners);
        }
        LOGGER.info(
                () -> "addListener(): add listener " + listener + " to " + key
                        + ", listened keys = " + CollectionUtils.toString(keysListeners.keySet())
        );
        boolean wasEmpty = listeners.isEmpty();
        boolean success = listeners.add(listener);
        if (!success) {
            LOGGER.info(
                    () -> "addListener(): FAILED add listener "
                            + listener + " to " + key + ", listened keys = "
                            + CollectionUtils.toString(keysListeners.keySet())
            );
        }
        
        if (
            wasEmpty && updateDataIfNoListener && db != null && taskScheduler != null && um != null
        ) { // Data is potentially expired or has never been collected
            updateDataWhenPossible(db, taskScheduler, um);
            MenuUpdater.startIfNeeded(this, db, taskScheduler, um);
        }
        return success;
    }
    
    private <L> boolean removeListener(int key, Map<Integer, Set<L>> keysListeners, L listener) {
        LOGGER.info(() -> "removeListener(): remove listener " + listener + " from " + key);
        Set<L> listeners = keysListeners.get(key);
        boolean success = false;
        if (listeners != null) {
            success = listeners.remove(listener);
            if (listeners.isEmpty()) {
                keysListeners.remove(key);
                //				if (listener instanceof ReportListener) {
                //					LOGGER.info(() -> "removeListener(): remove report " + key + " from cache");
                //					reports.remove(key);
                //				}
                // removed from cache at the end of #updateData
            }
        } else {
            success = true;
        }
        return success;
    }
    
    public boolean addReportCommentsPagesListener(int reportId, int page,
            ReportCommentsPageListener listener, Database db, TaskScheduler taskScheduler,
            UsersManager um) {
        Map<Integer, Set<ReportCommentsPageListener>> pagesListeners =
                reportsCommentsPagesListeners.get(reportId);
        if (pagesListeners == null) {
            pagesListeners = new ConcurrentHashMap<>();
            reportsCommentsPagesListeners.put(reportId, pagesListeners);
        }
        
        return addListener(page, pagesListeners, listener, true, db, taskScheduler, um);
    }
    
    public boolean removeReportCommentsPagesListener(int reportId, int page,
            ReportCommentsPageListener listener) {
        Map<Integer, Set<ReportCommentsPageListener>> pagesListeners =
                reportsCommentsPagesListeners.get(reportId);
        if (pagesListeners == null) {
            return true;
        } else {
            boolean success = removeListener(page, pagesListeners, listener);
            if (pagesListeners.isEmpty()) {
                reportsCommentsPagesListeners.remove(reportId);
            }
            return success;
        }
    }
    
    public void updateDataWhenPossible(Database db, TaskScheduler taskScheduler, UsersManager um) {
        long timeBeforeNextDataUpdate = getTimeBeforeNextDataUpdate();
        LOGGER.info(
                () -> "updateDataWhenPossible(): timeBeforeNextDataUpdate = "
                        + timeBeforeNextDataUpdate
        );
        if (timeBeforeNextDataUpdate == 0) {
            try {
                updateData(db, taskScheduler, um);
            } catch (IllegalStateException underCooldown) {
                LOGGER.info(() -> "updateDataWhenPossible(): calls updateDataWhenPossible()");
                updateDataWhenPossible(db, taskScheduler, um);
            }
        } else if (timeBeforeNextDataUpdate == -1) {
            dataUpdateRequested = true;
        } else {
            if (!pendingDataUpdateWhenPossible) {
                pendingDataUpdateWhenPossible = true;
                long now = System.currentTimeMillis();
                taskScheduler.runTaskDelayedly(timeBeforeNextDataUpdate, () -> {
                    pendingDataUpdateWhenPossible = false;
                    LOGGER.info(
                            () -> "updateDataWhenPossible(): calls updateData() (after having waited "
                                    + (System.currentTimeMillis() - now) + "ms)"
                    );
                    try {
                        updateData(db, taskScheduler, um);
                    } catch (IllegalStateException underCooldown) {
                        updateDataWhenPossible(db, taskScheduler, um);
                    }
                });
            }
        }
    }
    
    /**
     * Time in milliseconds.
     * 
     * @return -1 if undefined, 0 if no cooldown
     */
    public long getTimeBeforeNextDataUpdate() {
        long now = System.currentTimeMillis();
        
        long timeSinceLastUpdate = now - lastDataUpdateTime;
        if (isPendingDataUpdate()) {
            if (timeSinceLastUpdate <= DATA_UPDATE_MAX_TIME) {
                LOGGER.info(
                        () -> "getTimeBeforeNextDataUpdate(): pending update, undefined next data update time"
                );
                return -1;
            } else {
                LOGGER.warn(
                        () -> ConfigUtils.getInfoMessage(
                                "The last data update of reports took a lot of time, data updates are now allowed again.",
                                "La derniere mise a jour des donnees des signalements a pris beaucoup de temps, les mises a jour des donnees sont desormais a nouveau possibles."
                        )
                );
            }
        }
        
        if (timeSinceLastUpdate < DATA_UPDATE_COOLDOWN) {
            LOGGER.info(
                    () -> "getTimeBeforeNextDataUpdate(): under cooldown, timeSinceLastUpdate = "
                            + timeSinceLastUpdate
            );
            return DATA_UPDATE_COOLDOWN - timeSinceLastUpdate;
        }
        
        return 0;
    }
    
    private static class ReportCommentsPagesQuery {
        
        int reportId, minPage, maxPage;
        QueryResult qr;
        
        private ReportCommentsPagesQuery(int reportId, int minPage, int maxPage) {
            this.reportId = reportId;
            this.minPage = minPage;
            this.maxPage = maxPage;
        }
        
    }
    
    /**
     * Update data only if there are listeners.
     * 
     * @param db
     * @param taskScheduler
     * @param um
     * 
     * @return true if update started because it was needed
     * 
     * @throws IllegalStateException if {@link #getTimeBeforeNextDataUpdate} != 0
     */
    public boolean updateData(Database db, TaskScheduler taskScheduler, UsersManager um)
            throws IllegalStateException {
        long timeBeforeNextUpdateData = getTimeBeforeNextDataUpdate();
        if (timeBeforeNextUpdateData != 0) {
            LOGGER.info(
                    () -> "updateData(): cancelled because under cooldown, timeBeforeNextUpdateData = "
                            + timeBeforeNextUpdateData
            );
            throw new IllegalStateException("data update is under cooldown");
        }
        pendingDataUpdate = true;
        lastDataUpdateTime = System.currentTimeMillis();
        
        Map<Integer, Set<Integer>> reportsCommentsPagesWithSubs =
                getReportsCommentsWithSubscribers();
        
        if (
            reportsListeners.isEmpty()
                    && reportsPages.isEmpty()
                    && reportsCommentsPagesWithSubs == null
        ) {
            pendingDataUpdate = false;
            LOGGER.info(() -> "updateData(): cancelled because useless");
            // TODO clear comments from cache ?
            freeUnlistenedReportsPagesFromCache(); // remove unlistened reports pages before unlistened reports because some reports could be only listened by an unlistened page
            freeUnlistenedReportsFromCache();
            return false;
        }
        
        Set<ReportsPageCharacteristics> cachedReportsPagesCharac =
                new HashSet<>(reportsPages.keySet());
        taskScheduler.runTaskAsynchronously(() -> {
            final QueryResult reportsPagesQR = collectReportsPages(cachedReportsPagesCharac, db);
            
            final List<ReportCommentsPagesQuery> reportCommentsPagesQueries;
            if (reportsCommentsPagesWithSubs != null) {
                reportCommentsPagesQueries = new ArrayList<>();
                for (Entry<Integer, Set<Integer>> entry : reportsCommentsPagesWithSubs.entrySet()) {
                    Set<Integer> pages = entry.getValue();
                    if (pages != null && !pages.isEmpty()) {
                        ReportCommentsPagesQuery query = new ReportCommentsPagesQuery(
                                entry.getKey(),
                                Collections.min(pages),
                                Collections.max(pages)
                        );
                        
                        query.qr = collectReportCommentsPages(
                                query.reportId,
                                query.minPage,
                                query.maxPage,
                                db
                        );
                        if (query.qr != null) {
                            reportCommentsPagesQueries.add(query);
                        }
                    }
                }
            } else {
                reportCommentsPagesQueries = null;
            }
            
            taskScheduler.runTask(() -> {
                if (reportsPagesQR != null) {
                    updateReportsPages(reportsPagesQR.getResultList(), db, taskScheduler);
                    LOGGER.info(
                            () -> "updateData(): updated reports pages (not yet their reports)"
                    );
                }
                
                Set<Integer> reportsToUpdate = getKeysWithSubscribers(reportsListeners);
                
                if (reportCommentsPagesQueries != null && !reportCommentsPagesQueries.isEmpty()) {
                    if (reportsToUpdate == null) {
                        reportsToUpdate = new HashSet<>();
                    }
                    for (ReportCommentsPagesQuery query : reportCommentsPagesQueries) {
                        reportsToUpdate.add(query.reportId);
                    }
                }
                
                final Set<Integer> freportsToUpdate = reportsToUpdate;
                LOGGER.info(
                        () -> "updateData(): start collecting and updating reports: "
                                + CollectionUtils.toString(freportsToUpdate)
                );
                
                getReportsByIdAsynchronously(
                        reportsToUpdate,
                        false,
                        db,
                        taskScheduler,
                        um,
                        (updatedReports) -> {
                            LOGGER.info(
                                    () -> "updateData(): start updating reports comments pages"
                            );
                            if (
                                reportCommentsPagesQueries != null
                                        && !reportCommentsPagesQueries.isEmpty()
                            ) {
                                for (ReportCommentsPagesQuery query : reportCommentsPagesQueries) {
                                    updateReportCommentsPages(
                                            query.reportId,
                                            query.minPage,
                                            query.qr.getResultList(),
                                            db,
                                            taskScheduler,
                                            um
                                    );
                                }
                            }
                            
                            LOGGER.info(
                                    () -> "updateData(): updated reports (overall time spent: "
                                            + (System.currentTimeMillis() - lastDataUpdateTime)
                                            + "ms), cached reports: "
                                            + CollectionUtils.toString(reports.keySet())
                            );
                            
                            LOGGER.info(
                                    () -> "updateData(): broadcastChangedReportsCommentsPages()"
                            );
                            broadcastChangedReportsCommentsPages();
                            LOGGER.info(() -> "updateData(): broadcastChangedReportsPages()");
                            broadcastChangedReportsPages();
                            
                            LOGGER.info(
                                    () -> "updateData(): freeUnlistenedReportsPagesFromCache()"
                            );
                            freeUnlistenedReportsPagesFromCache(); // remove unlistened reports pages before unlistened reports because some reports could be only listened by an unlistened page
                            LOGGER.info(() -> "updateData(): freeUnlistenedReportsFromCache()");
                            freeUnlistenedReportsFromCache();
                            LOGGER.info(() -> "updateData(): finished");
                            
                            pendingDataUpdate = false;
                            
                            if (isDataUpdateRequested()) {
                                dataUpdateRequested = false;
                                updateDataWhenPossible(db, taskScheduler, um);
                            }
                        }
                );
            });
            
        });
        
        return true;
    }
    
    public Map<Integer, Set<Integer>> getReportsCommentsWithSubscribers() {
        if (reportsCommentsPagesListeners != null && !reportsCommentsPagesListeners.isEmpty()) {
            Map<Integer, Set<Integer>> result = new HashMap<>();
            for (
                Entry<Integer, Map<Integer, Set<ReportCommentsPageListener>>> entry : reportsCommentsPagesListeners
                        .entrySet()
            ) {
                Set<Integer> keysWithSubscribers = getKeysWithSubscribers(entry.getValue());
                if (keysWithSubscribers != null) {
                    result.put(entry.getKey(), keysWithSubscribers);
                }
            }
            return result;
        } else {
            return null;
        }
    }
    
    private static <L> Set<Integer> getKeysWithSubscribers(Map<Integer, Set<L>> subscribers) {
        if (!areAnyKeyWithSubscriber(subscribers)) {
            return null;
        } else {
            return new HashSet<>(subscribers.keySet());
        }
    }
    
    private static <L> boolean areAnyKeyWithSubscriber(Map<Integer, Set<L>> subscribers) {
        return subscribers != null && !subscribers.isEmpty();
    }
    
    /**
     * Get reports by their id from the database, without using the cache, and then if
     * {@code saveInCache} is true, save or update the reports in cache.
     * 
     * @param reportsId
     * @param withAdvancedData
     * @param saveInCache
     * @param db
     * @param taskScheduler
     * @param um
     * @param resultCallback
     */
    public void getReportsByIdAsynchronously(Set<Integer> reportsId, boolean withAdvancedData,
            Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<List<Report>> resultCallback) {
        collectReportsByIdAsynchronously(reportsId, db, taskScheduler, (reportsQR) -> {
            List<Map<String, Object>> reportsData =
                    reportsQR != null ? reportsQR.getResultList() : null;
            LOGGER.info(
                    () -> "getReportsByIdAsynchronously(): reports data received, updateAndGetReports()"
            );
            
            updateAndGetReports(
                    reportsData,
                    withAdvancedData,
                    db,
                    taskScheduler,
                    um,
                    resultCallback
            );
        });
    }
    
    private static void collectReportsByIdAsynchronously(Set<Integer> reportsId, Database db,
            TaskScheduler taskScheduler, ResultCallback<QueryResult> resultCallback) {
        if (reportsId == null || reportsId.isEmpty()) {
            LOGGER.info(() -> "collectReportsByIdAsynchronously(): reportsId = null or empty");
            resultCallback.onResultReceived(null);
            return;
        }
        
        List<Object> queryParams = Arrays.asList(reportsId.toArray());
        StringBuilder query =
                new StringBuilder("SELECT * FROM tigerreports_reports WHERE report_id IN (?");
        for (int i = 1; i < queryParams.size(); i++) {
            query.append(",?");
        }
        query.append(") ORDER BY report_id ASC");
        
        LOGGER.info(
                () -> "collectReportsByIdAsynchronously(): " + CollectionUtils.toString(queryParams)
        );
        db.queryAsynchronously(query.toString(), queryParams, taskScheduler, resultCallback);
    }
    
    private void broadcastChangedReportsCommentsPages() {
        for (Entry<Integer, Map<Integer, Boolean>> entry : reportsCommentsPagesChanged.entrySet()) {
            Map<Integer, Boolean> reportCommentsPagesChanged = entry.getValue();
            if (reportCommentsPagesChanged != null) {
                for (int page : reportCommentsPagesChanged.keySet()) {
                    broadcastReportCommentsPageChanged(entry.getKey(), page);
                }
            }
        }
        reportsCommentsPagesChanged.clear();
    }
    
    private void broadcastChangedReportsPages() {
        LOGGER.info(() -> "broadcastChangedReportsPages()");
        for (ReportsPage page : new ArrayList<>(reportsPages.values())) {
            page.broadcastIfPageChanged();
        }
    }
    
    private void setReportCommentsPageAsChanged(int reportId, int page) {
        if (page <= 0) {
            return;
        }
        LOGGER.info(() -> "setReportCommentsPageAsChanged(" + reportId + ", " + page + ")");
        Map<Integer, Boolean> reportCommentsPagesChanged =
                reportsCommentsPagesChanged.get(reportId);
        if (reportCommentsPagesChanged == null) {
            reportCommentsPagesChanged = new ConcurrentHashMap<>();
            reportsCommentsPagesChanged.put(reportId, reportCommentsPagesChanged);
        }
        
        reportCommentsPagesChanged.put(page, true);
    }
    
    private int getCommentPageById(int reportId, int commentId) {
        int commentIndex = getCommentIndexById(reportId, commentId);
        return commentIndex != -1 ? pageOfCommentIndex(commentIndex) : -1;
    }
    
    private int getCommentIndexById(int reportId, int commentId) {
        List<Comment> reportComments = reportsComments.get(reportId);
        if (reportComments == null) {
            return -1;
        }
        for (int i = 0; i < reportComments.size(); i++) {
            Comment comment = reportComments.get(i);
            if (comment != null && comment.getId() == commentId) {
                return i;
            }
        }
        return -1;
    }
    
    private void broadcastReportCommentsPageChanged(int reportId, int page) {
        LOGGER.info(() -> "broadcastReportCommentsPageChanged(" + reportId + ", " + page + ")");
        Map<Integer, Set<ReportCommentsPageListener>> reportCommentsPagesSubs =
                reportsCommentsPagesListeners.get(reportId);
        if (reportCommentsPagesSubs != null) {
            Set<ReportCommentsPageListener> listeners = reportCommentsPagesSubs.get(page);
            if (listeners != null) {
                listeners = new HashSet<>(listeners);
                for (ReportCommentsPageListener l : listeners) {
                    l.onReportCommentsPageChanged(page);
                }
            }
        }
    }
    
    public void broadcastReportDataChanged(Report r) {
        if (r == null) {
            return;
        }
        LOGGER.info(() -> "broadcastReportDataChanged(" + r.getId() + ")");
        Set<ReportListener> listeners = reportsListeners.get(r.getId());
        if (listeners != null && !listeners.isEmpty()) {
            //			listeners = new HashSet<>(listeners);
            for (ReportListener l : listeners) {
                l.onReportDataChange(r);
            }
        }
    }
    
    public void reportIsDeleted(int reportId) {
        LOGGER.info(() -> "reportIsDeleted(" + reportId + ")");
        
        broadcastReportDeleted(reportId);
        removeReportFromCache(reportId);
    }
    
    public void removeReportFromCache(int reportId) {
        LOGGER.info(() -> "removeReportFromCache(" + reportId + ")");
        // TODO rm report comments data from cache ?
        reports.remove(reportId);
        reportsListeners.remove(reportId);
    }
    
    public void commentIsDeleted(Comment c) {
        int reportId = c.getReport().getId();
        List<Comment> reportComments = reportsComments.get(reportId);
        if (reportComments != null && !reportComments.isEmpty()) {
            int page = getCommentPageById(reportId, c.getId()); // Required because when comment is removed from list, the page can no longer be obtained.
            if (reportComments.remove(c)) {
                broadcastCommentDataChanged(c, page);
            }
        }
    }
    
    private void broadcastReportDeleted(int reportId) {
        LOGGER.info(() -> "broadcastReportDeleted(" + reportId + ")");
        Set<ReportListener> listeners = reportsListeners.get(reportId);
        if (listeners != null) {
            //			listeners = new HashSet<>(listeners);
            for (ReportListener l : listeners) {
                l.onReportDelete(reportId);
            }
        }
    }
    
    public void broadcastCommentDataChanged(Comment c) {
        broadcastCommentDataChanged(c, -1);
    }
    
    public void broadcastCommentDataChanged(Comment c, int page) {
        if (c == null) {
            return;
        }
        LOGGER.info(() -> "broadcastCommentDataChanged(" + c + ")");
        
        int reportId = c.getReport().getId();
        if (page == -1) {
            page = getCommentPageById(reportId, c.getId());
        }
        if (page != -1) {
            broadcastReportCommentsPageChanged(reportId, page);
        }
    }
    
    public void getReportByIdAsynchronously(int reportId, boolean withAdvancedData,
            boolean useCache, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Report> resultCallback) {
        LOGGER.debug(() -> "getReportByIdAsynchronously()");
        checkValidReportId(reportId);
        
        if (useCache) {
            LOGGER.debug(() -> "getReportByIdAsynchronously(): useCache");
            Report r = getCachedReportById(reportId);
            if (r != null && (!withAdvancedData || r.hasAdvancedData())) {
                LOGGER.info(() -> "getReportByIdAsynchronously(): return cached");
                resultCallback.onResultReceived(r);
                return;
            }
        }
        
        LOGGER.debug(() -> "getReportByIdAsynchronously(): query to db");
        db.queryAsynchronously(
                "SELECT * FROM tigerreports_reports WHERE report_id = ? LIMIT 1",
                Collections.singletonList(reportId),
                taskScheduler,
                (qr) -> {
                    LOGGER.debug(() -> "getReportByIdAsynchronously: onResultReceived()");
                    updateAndGetReport(
                            reportId,
                            qr,
                            withAdvancedData,
                            db,
                            taskScheduler,
                            um,
                            resultCallback
                    );
                }
        );
    }
    
    private void updateAndGetReports(final List<Map<String, Object>> reportsData,
            boolean saveAdvancedData, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<List<Report>> resultCallback) {
        if (reportsData == null || reportsData.isEmpty()) {
            LOGGER.info(() -> "updateAndGetReports(): reportsData = null or empty");
            resultCallback.onResultReceived(null);
            return;
        }
        
        SeveralTasksHandler<Report> reportsTaskHandler = new SeveralTasksHandler<>();
        
        for (Map<String, Object> reportData : reportsData) {
            updateAndGetReport(
                    reportData,
                    saveAdvancedData,
                    db,
                    taskScheduler,
                    um,
                    reportsTaskHandler.newTaskResultSlot()
            );
        }
        
        reportsTaskHandler.whenAllTasksDone(false, (reports) -> {
            if (LOGGER.isWarnLoggable()) {
                int failedUpdateReportsAmount = 0;
                StringBuilder failedUpdateReportsData = new StringBuilder();
                if (reports != null) {
                    for (int i = 0; i < reports.size(); i++) {
                        Report r = reports.get(i);
                        if (r == null) {
                            failedUpdateReportsAmount++;
                            failedUpdateReportsData
                                    .append(CollectionUtils.toString(reportsData.get(i)));
                        }
                    }
                }
                final int ffailedUpdateReportsAmount = failedUpdateReportsAmount;
                if (failedUpdateReportsAmount > 0) {
                    LOGGER.warn(
                            () -> "updateAndGetReports(): failed update of reports ("
                                    + ffailedUpdateReportsAmount + "): [" + failedUpdateReportsData
                                    + "]"
                    );
                }
            }
            
            resultCallback.onResultReceived(reports);
        });
    }
    
    public void updateAndGetReport(Map<String, Object> reportData, boolean saveAdvancedData,
            Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Report> resultCallback) {
        LOGGER.debug(() -> "updateAndGetReport(reportData)");
        if (reportData == null) {
            LOGGER.info(() -> "updateAndGetReport(): reportData = null");
            resultCallback.onResultReceived(null);
            return;
        }
        
        Integer reportId = (Integer) reportData.get(Report.REPORT_ID);
        if (reportId != null) {
            updateAndGetReport(
                    reportId,
                    reportData,
                    saveAdvancedData,
                    db,
                    taskScheduler,
                    um,
                    resultCallback
            );
        } else {
            LOGGER.info(() -> "updateAndGetReport(): reportId = null");
            resultCallback.onResultReceived(null);
        }
    }
    
    public void updateAndGetReport(int reportId, QueryResult qr, boolean saveAdvancedData,
            Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Report> resultCallback) {
        LOGGER.debug(() -> "updateAndGetReport(reportId, qr)");
        updateAndGetReport(
                reportId,
                qr.getResult(0),
                saveAdvancedData,
                db,
                taskScheduler,
                um,
                resultCallback
        );
    }
    
    /**
     * 
     * @param reportId
     * @param reportData       = null if the report no longer exists
     * @param saveAdvancedData
     * @param saveInCache
     * @param db
     * @param taskScheduler
     * @param um
     * @param resultCallback
     */
    public void updateAndGetReport(int reportId, Map<String, Object> reportData,
            boolean saveAdvancedData, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Report> resultCallback) {
        LOGGER.debug(() -> "updateAndGetReport(reportId, reportData)");
        boolean archived = false;
        if (reportData != null) {
            archived = QueryResult.isTrue(reportData.get(Report.ARCHIVED));
            final boolean farchived = archived;
            LOGGER.debug(
                    () -> "updateAndGetReport(reportId, reportData): archived = " + farchived
                            + ", data = " + reportData.get(Report.ARCHIVED)
            );
        }
        updateAndGetReport(
                reportId,
                reportData,
                archived,
                saveAdvancedData,
                db,
                taskScheduler,
                um,
                resultCallback
        );
    }
    
    /**
     * 
     * @param reportId
     * @param reportData               used to update the report if it is cached, or create the
     *                                 report if it isn't cached.
     * @param archived
     * @param saveAdvancedData
     * @param saveInCacheIfListened    if true and the report has at least one listener, the report
     *                                 will be cached if it isn't yet
     * @param saveInCacheIfNotListened if true and the report has not any listener, the report will
     *                                 be cached if it isn't yet
     * @param db
     * @param taskScheduler
     * @param um
     * @param resultCallback
     */
    public void updateAndGetReport(int reportId, Map<String, Object> reportData, boolean archived,
            boolean saveAdvancedData, Database db, TaskScheduler taskScheduler, UsersManager um,
            ResultCallback<Report> resultCallback) {
        final Report r = getCachedReportById(reportId);
        if (r == null) {
            LOGGER.info(() -> "updateAndGetReport(): CREATE " + reportId + " (r = null)");
            
            Report.asynchronouslyFrom(
                    reportData,
                    archived,
                    saveAdvancedData,
                    db,
                    taskScheduler,
                    um,
                    (newR) -> {
                        Report newCachedR = getCachedReportById(reportId, newR);
                        
                        if (newCachedR != null) {
                            broadcastReportDataChanged(newCachedR);
                        }
                        resultCallback.onResultReceived(newCachedR);
                    }
            );
        } else if (reportData != null) {
            LOGGER.info(
                    () -> "updateAndGetReport(): UPDATE " + reportId
                            + " (r != null, reportData != null), report before update: " + r
            );
            r.update(reportData, archived, saveAdvancedData, db, taskScheduler, um, (changed) -> {
                if (changed) {
                    broadcastReportDataChanged(r);
                }
                resultCallback.onResultReceived(r);
            });
        } else {
            LOGGER.info(() -> "updateAndGetReport(): DELETED " + reportId + " (r = " + r + ")");
            
            r.delete(null, false, db, taskScheduler, this, null, null);
            resultCallback.onResultReceived(null);
        }
    }
    
    public Report getCachedReportById(int reportId) {
        return getCachedReportById(reportId, null);
    }
    
    private Report getCachedReportById(int reportId, Report valueForInitialization) {
        checkValidReportId(reportId);
        
        Report r = reports.get(reportId);
        if (valueForInitialization != null && r == null) {
            r = valueForInitialization;
            reports.put(reportId, r);
            LOGGER.info(
                    () -> "getCachedReportById(): " + reportId + " added to cache: "
                            + valueForInitialization
            );
        }
        
        return r;
    }
    
    private static void checkValidReportId(int reportId) throws IllegalArgumentException {
        if (reportId < 0) {
            throw new IllegalArgumentException("Report id cannot be negative.");
        }
    }
    
    public Comment getCommentAtIndexInPage(int reportId, int page, int commentIndexInPage) {
        return getCommentAtIndex(reportId, firstIndexOfCommentsPage(page) + commentIndexInPage);
    }
    
    public Comment getCommentAtIndex(int reportId, int commentIndex) {
        List<Comment> reportComments = reportsComments.get(reportId);
        if (reportComments == null) {
            return null;
        }
        return CollectionUtils.getElementAtIndex(reportComments, commentIndex);
    }
    
    public boolean isCachedReportCommentsPageNotEmpty(int reportId, int page) {
        List<Comment> reportComments = reportsComments.get(reportId);
        int cachedReportCommentsAmount = reportComments != null ? reportComments.size() : 0;
        return firstIndexOfCommentsPage(page) < cachedReportCommentsAmount;
    }
    
    public List<Report> getReportsPageCachedReports(ReportsPage reportsPage) {
        List<Report> pageReports = new ArrayList<>();
        List<Integer> pageReportsId = reportsPage.getReportsId();
        
        LOGGER.info(
                () -> "getReportsPageCachedReports(): pageReportsId = "
                        + CollectionUtils.toString(pageReportsId)
        );
        
        for (Integer reportId : pageReportsId) {
            if (reportId != null) {
                Report r = getCachedReportById(reportId);
                if (r != null) {
                    pageReports.add(r);
                } else {
                    LOGGER.info(
                            () -> "getReportsPageCachedReports(): report " + reportId
                                    + " is not (yet) cached"
                    );
                }
            }
        }
        return pageReports;
    }
    
    public List<Comment> getCachedReportComments(int reportId, boolean initialize) {
        List<Comment> reportComments = reportsComments.get(reportId);
        if (reportComments == null && initialize) {
            reportComments = new ArrayList<>();
            reportsComments.put(reportId, reportComments);
        }
        return reportComments;
    }
    
    public List<Comment> getReportCommentsCachedPageComments(int reportId, int page) {
        List<Comment> reportComments = reportsComments.get(reportId);
        if (reportComments != null) {
            int includedMinIndex = firstIndexOfCommentsPage(page);
            int excludedMaxIndex =
                    Math.min(firstIndexOfCommentsPage(page + 1), reportComments.size());
            if (includedMinIndex < excludedMaxIndex) {
                return new ArrayList<>(reportComments.subList(includedMinIndex, excludedMaxIndex));
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 
     * @param page starting with 1
     * 
     * @return
     */
    public static int firstIndexOfCommentsPage(int page) {
        return (page - 1) * PAGE_MAX_COMMENTS_AMOUNT;
    }
    
    public static int lastIndexOfCommentsPage(int page) {
        return firstIndexOfCommentsPage(page + 1) - 1;
    }
    
    public static int pageOfCommentIndex(int index) {
        if (index < 0) {
            index = 0;
        }
        return (index / PAGE_MAX_COMMENTS_AMOUNT) + 1;
    }
    
    private QueryResult collectReportCommentsPages(int reportId, int minPage, int maxPage,
            Database db) {
        LOGGER.info(() -> "collectReportCommentsPages(" + reportId + ")");
        
        int minCommentIndex = firstIndexOfCommentsPage(minPage);
        int maxCommentsAmount = (maxPage - minPage + 1) * PAGE_MAX_COMMENTS_AMOUNT;
        
        return db.query(
                "SELECT * FROM tigerreports_comments WHERE report_id = ? LIMIT ? OFFSET ?",
                Arrays.asList(reportId, maxCommentsAmount, minCommentIndex)
        );
    }
    
    private QueryResult collectReportsPages(Set<ReportsPageCharacteristics> pagesCharacteristics,
            Database db) {
        if (pagesCharacteristics == null || pagesCharacteristics.isEmpty()) {
            LOGGER.info(
                    () -> "collectReportsPages(): null or empty "
                            + CollectionUtils.toString(pagesCharacteristics)
            );
            return null;
        }
        
        final StringBuilder query = new StringBuilder();
        final List<Object> params = new ArrayList<>();
        int index = 0;
        
        LOGGER.info(
                () -> "collectReportsPages(): " + CollectionUtils.toString(pagesCharacteristics)
        );
        for (ReportsPageCharacteristics pageCharacteristics : pagesCharacteristics) {
            ReportsCharacteristics reportsCharacteristics =
                    pageCharacteristics.reportsCharacteristics;
            
            final String whereClause;
            final String whereClauseParam;
            if (reportsCharacteristics.reporterUUID != null) {
                whereClause = " AND reporter_uuid LIKE ?";
                whereClauseParam = "%" + reportsCharacteristics.reporterUUID + "%";
            } else if (reportsCharacteristics.reportedUUID != null) {
                whereClause = " AND reported_uuid = ?";
                whereClauseParam = reportsCharacteristics.reportedUUID.toString();
            } else {
                whereClause = "";
                whereClauseParam = null;
            }
            final String orderClause =
                    " ORDER BY report_id " + (reportsCharacteristics.archived ? "DESC" : "ASC");
            
            params.add(reportsCharacteristics.archived ? 1 : 0);
            if (!whereClause.isEmpty()) {
                params.add(whereClauseParam);
            }
            params.add(ReportsPage.PAGE_MAX_REPORTS_AMOUNT + 1);
            params.add(ReportsPage.firstGlobalIndexOfPage(pageCharacteristics.page));
            
            if (query.length() != 0) {
                query.append(" UNION ALL ");
            }
            
            query.append(
                    "SELECT * FROM (SELECT report_id,'" + pageCharacteristics + "' AS "
                            + PAGE_CHARACTERISTICS_FAKE_COLUMN
                            + " FROM tigerreports_reports WHERE archived = ?" + whereClause
                            + orderClause + " LIMIT ? OFFSET ?) AS p" + index
            );
            index++;
        }
        
        return db.query(query.toString(), params);
    }
    
    private void updateReportCommentsPages(int reportId, int minPage,
            List<Map<String, Object>> results, Database db, TaskScheduler taskScheduler,
            UsersManager um) {
        List<Comment> reportComments = getCachedReportComments(reportId, true);
        
        int resultsSize = results.size();
        int firstCommentIndex = firstIndexOfCommentsPage(minPage);
        int maxCommentIndex = firstCommentIndex + resultsSize - 1;
        
        int page = minPage;
        boolean pageHasChanged = false;
        
        SeveralTasksHandler<Comment> commentsTaskHandler = new SeveralTasksHandler<>();
        List<Integer> newCommentsIndex = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        Report r = getCachedReportById(reportId);
        synchronized (reportComments) {
            for (int index = firstCommentIndex; index <= maxCommentIndex; index++) {
                Map<String, Object> commentDbResult = results.get(index - firstCommentIndex);
                if (commentDbResult != null) {
                    int commentId = (int) commentDbResult.get("comment_id");
                    Comment previousComment =
                            index < reportComments.size() ? reportComments.get(index) : null;
                    Integer previousCommentId =
                            previousComment != null ? previousComment.getId() : null;
                    if (previousCommentId == null || commentId != previousCommentId) {
                        newCommentsIndex.add(index);
                        r.getCommentAsynchronouslyFrom(
                                commentDbResult,
                                db,
                                taskScheduler,
                                um,
                                commentsTaskHandler.newTaskResultSlot()
                        );
                        pageHasChanged = true;
                    } else {
                        pageHasChanged |= previousComment.update(commentDbResult);
                    }
                }
                
                if (pageOfCommentIndex(index + 1) > page) {
                    if (pageHasChanged) {
                        setReportCommentsPageAsChanged(reportId, page);
                    }
                    pageHasChanged = false;
                    page++;
                }
            }
        }
        
        // After the for loop, the last page could have changed.
        if (pageHasChanged) {
            setReportCommentsPageAsChanged(reportId, page);
        }
        
        commentsTaskHandler.whenAllTasksDone(false, (newComments) -> {
            LOGGER.info(
                    () -> "updateReportCommentsPages(" + reportId + "): took "
                            + (System.currentTimeMillis() - now)
                            + "ms to process all comments data received from db"
            );
            
            int lastIndex;
            
            synchronized (reportComments) {
                int i = 0;
                int commentIndex = firstCommentIndex;
                for (Iterator<Comment> it = newComments.iterator(); it.hasNext(); i++) {
                    Comment c = it.next();
                    commentIndex = newCommentsIndex.get(i);
                    if (c == null) {
                        final int fcommentIndex = commentIndex;
                        LOGGER.warn(
                                () -> "updateReportCommentsPages(): comment at index "
                                        + fcommentIndex + " (first comment index = "
                                        + firstCommentIndex + ") = null"
                        );
                    }
                    
                    if (commentIndex < reportComments.size()) {
                        reportComments.set(commentIndex, c);
                    } else if (commentIndex == reportComments.size()) {
                        reportComments.add(c);
                    } else {
                        throw new IllegalStateException(
                                "index " + commentIndex + " (first comment index = "
                                        + firstCommentIndex + ") > reportComments size "
                                        + reportComments.size()
                        );
                    }
                }
                
                lastIndex = reportComments.size() - 1;
                
                // Remove previous remaining reports starting from the end of the last report of the max page.
                for (int oldIndex = lastIndex; oldIndex > maxCommentIndex; oldIndex--) {
                    reportComments.remove(oldIndex);
                }
            }
            
            // Broadcast changes after all remove done.
            for (
                    int pageWithOneRemovedComment = pageOfCommentIndex(maxCommentIndex + 1);
                    pageWithOneRemovedComment <= pageOfCommentIndex(lastIndex);
                    pageWithOneRemovedComment++
            ) {
                setReportCommentsPageAsChanged(reportId, pageWithOneRemovedComment);
            }
        });
    }
    
    private void updateReportsPages(List<Map<String, Object>> results, Database db,
            TaskScheduler taskScheduler) {
        if (results == null || results.isEmpty()) {
            LOGGER.info(() -> "updateReportsPages(): empty result");
            return;
        }
        
        ReportsPageCharacteristics pageCharac = null;
        ReportsPage reportsPage = null;
        int reportIndexInPage = 0;
        
        for (Map<String, Object> reportDbResult : results) {
            if (reportDbResult == null || reportDbResult.isEmpty()) {
                continue;
            }
            
            String reportsPageCharacteristics =
                    (String) reportDbResult.get(PAGE_CHARACTERISTICS_FAKE_COLUMN);
            if (
                reportsPage == null
                        || pageCharac == null
                        || !pageCharac.toString().equals(reportsPageCharacteristics)
            ) {
                // Beginning of a page result
                if (reportsPage != null) { // A previous page was processed
                    reportsPage.removeOldReports(reportIndexInPage, this);
                }
                
                pageCharac = ReportsPageCharacteristics.fromString(reportsPageCharacteristics);
                reportsPage = getReportsPage(pageCharac, false, db, taskScheduler, null); // The page could no longer be listened, there is no reason to initialize it (um is useless because no initialization).
                if (reportsPage == null) { // The page is no longer in cache (no longer listened), results are not saved.
                    continue;
                }
                reportIndexInPage = 0;
            }
            
            if (reportDbResult != null) {
                int reportId = (int) reportDbResult.get("report_id");
                if (LOGGER.isInfoLoggable()) {
                    ReportsPageCharacteristics fpageCharac = pageCharac;
                    LOGGER.info(
                            () -> "updateReportsPages(): page = " + fpageCharac + ", report = "
                                    + reportId
                    );
                }
                
                reportsPage.updateReportAtIndex(reportIndexInPage, reportId, this);
                reportIndexInPage++;
            }
        }
        if (reportsPage != null) { // A previous page was processed
            reportsPage.removeOldReports(reportIndexInPage - 1, this);
        }
    }
    
    public void fillInventoryWithReportsPage(Inventory inv, ReportsPage reportsPage,
            String actionsBefore, boolean archiveAction, String actionsAfter, VaultManager vm,
            BungeeManager bm) {
        List<Report> pageReports = getReportsPageCachedReports(reportsPage);
        int index = 0;
        LOGGER.info(
                () -> "fillInventoryWithReportsPage(): reports = "
                        + CollectionUtils.toString(pageReports)
        );
        LOGGER.info(() -> "fillInventoryWithReportsPage(): inv = " + inv);
        for (int slot = 18; slot < 45; slot++) {
            if (index >= pageReports.size()) {
                inv.setItem(slot, null);
            } else {
                Report r = pageReports.get(index);
                if (r == null) {
                    inv.setItem(slot, null);
                } else {
                    String archiveActionStr = archiveAction
                            && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives())
                                    ? Message.REPORT_ARCHIVE_ACTION.get()
                                    : "";
                    inv.setItem(
                            slot,
                            r.getItem(actionsBefore + archiveActionStr + actionsAfter, vm, bm)
                    );
                    index++;
                }
            }
        }
        
        int size = inv.getSize();
        inv.setItem(
                size - 7,
                reportsPage.getPage() >= 2
                        ? MenuItem.PAGE_SWITCH_PREVIOUS.get()
                        : MenuRawItem.GUI.create()
        );
        
        inv.setItem(
                size - 3,
                reportsPage.isNextPageNotEmpty()
                        ? MenuItem.PAGE_SWITCH_NEXT.get()
                        : MenuRawItem.GUI.create()
        );
        LOGGER.info(() -> "fillInventoryWithReportsPage(): end");
    }
    
    public boolean isDataUpdateRequested() {
        return dataUpdateRequested;
    }
    
    public boolean isPendingDataUpdate() {
        return pendingDataUpdate;
    }
    
}
