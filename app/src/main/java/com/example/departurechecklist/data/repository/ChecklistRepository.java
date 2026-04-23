package com.example.departurechecklist.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.departurechecklist.data.db.ChecklistWithItems;
import com.example.departurechecklist.data.db.dao.ChecklistDao;
import com.example.departurechecklist.data.db.dao.ChecklistItemDao;
import com.example.departurechecklist.data.db.entity.Checklist;
import com.example.departurechecklist.data.db.entity.ChecklistItem;
import com.example.departurechecklist.notification.DepartureScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository exposing checklist CRUD and reminder scheduling operations.
 */
public class ChecklistRepository {
    private static final String TAG = "ChecklistRepository";

    private final ChecklistDao checklistDao;
    private final ChecklistItemDao checklistItemDao;
    private final DepartureScheduler departureScheduler;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;

    /**
     * Result state for reminder scheduling.
     */
    public enum ReminderScheduleResult {
        SUCCESS,
        EXACT_ALARM_PERMISSION_REQUIRED,
        ERROR,
        LEAD_TIME_TOO_LONG
    }

    /**
     * Listener for asynchronous reminder scheduling completion.
     */
    public interface ReminderScheduleCallback {
        /**
         * Called when reminder scheduling finishes.
         *
         * @param result operation result
         */
        void onComplete(ReminderScheduleResult result);
    }

    /**
     * Creates the repository.
     *
     * @param checklistDao checklist DAO
     * @param checklistItemDao checklist item DAO
     * @param departureScheduler alarm scheduler
     */
    public ChecklistRepository(@NonNull ChecklistDao checklistDao,
            @NonNull ChecklistItemDao checklistItemDao,
            @NonNull DepartureScheduler departureScheduler) {
        this.checklistDao = checklistDao;
        this.checklistItemDao = checklistItemDao;
        this.departureScheduler = departureScheduler;
        this.ioExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Observes a checklist relation by event id.
     *
     * @param eventId calendar event id
     * @return live checklist relation
     */
    @NonNull
    public LiveData<ChecklistWithItems> observeChecklistByEventId(long eventId) {
        return checklistDao.observeChecklistWithItemsByEventId(eventId);
    }

    /**
     * Observes a checklist relation by checklist id.
     *
     * @param checklistId checklist id
     * @return live checklist relation
     */
    @NonNull
    public LiveData<ChecklistWithItems> observeChecklistById(long checklistId) {
        return checklistDao.observeChecklistWithItemsById(checklistId);
    }

    /**
     * Ensures a checklist exists for an event, creating one when absent.
     *
     * @param eventId event identifier
     * @param eventTitle event title
     * @param eventStartMillis event start time in milliseconds
     */
    public void ensureChecklistExists(long eventId, @NonNull String eventTitle, long eventStartMillis) {
        ioExecutor.execute(() -> {
            try {
                Checklist existing = checklistDao.findByEventId(eventId);
                if (existing == null) {
                    Checklist checklist = new Checklist();
                    checklist.eventId = eventId;
                    checklist.eventTitle = eventTitle;
                    checklist.eventStartMillis = eventStartMillis;
                    checklist.reminderMinutesBefore = 0;
                    checklistDao.insert(checklist);
                } else {
                    boolean changed = false;
                    if (!eventTitle.equals(existing.eventTitle)) {
                        existing.eventTitle = eventTitle;
                        changed = true;
                    }
                    if (eventStartMillis > 0 && existing.eventStartMillis != eventStartMillis) {
                        existing.eventStartMillis = eventStartMillis;
                        changed = true;
                    }
                    if (changed) {
                        checklistDao.update(existing);
                    }
                }
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to ensure checklist exists", exception);
            }
        });
    }

    /**
     * Adds a new checklist item.
     *
     * @param checklistId target checklist id
     * @param label item label
     */
    public void addChecklistItem(long checklistId, @NonNull String label) {
        ioExecutor.execute(() -> {
            try {
                ChecklistItem item = new ChecklistItem();
                item.checklistId = checklistId;
                item.label = label;
                item.isChecked = false;
                item.sortOrder = checklistItemDao.getNextSortOrder(checklistId);
                checklistItemDao.insert(item);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to add checklist item", exception);
            }
        });
    }

    /**
     * Updates a checklist item row.
     *
     * @param item checklist item to persist
     */
    public void updateChecklistItem(@NonNull ChecklistItem item) {
        ioExecutor.execute(() -> {
            try {
                checklistItemDao.update(item);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to update checklist item", exception);
            }
        });
    }

    /**
     * Deletes a single checklist item.
     *
     * @param itemId checklist item id
     */
    public void deleteChecklistItem(long itemId) {
        ioExecutor.execute(() -> {
            try {
                checklistItemDao.deleteById(itemId);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to delete checklist item", exception);
            }
        });
    }

    /**
     * Deletes all checklist items for a checklist.
     *
     * @param checklistId checklist id
     */
    public void clearChecklistItems(long checklistId) {
        ioExecutor.execute(() -> {
            try {
                checklistItemDao.deleteForChecklist(checklistId);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to clear checklist items", exception);
            }
        });
    }

    /**
     * Updates reminder settings and schedules an exact alarm.
     *
     * @param checklistId checklist id
     * @param minutesBefore lead time in minutes
     * @param callback completion callback
     */
    public void scheduleReminder(long checklistId, int minutesBefore,
            @NonNull ReminderScheduleCallback callback) {
        ioExecutor.execute(() -> {
            ReminderScheduleResult result = ReminderScheduleResult.ERROR;
            try {
                Checklist checklist = checklistDao.findById(checklistId);
                if (checklist == null) {
                    Log.e(TAG, "Checklist not found for scheduling: " + checklistId);
                } else {
                    checklist.reminderMinutesBefore = Math.max(minutesBefore, 0);
                    checklistDao.update(checklist);
                    if (checklist.reminderMinutesBefore == 0) {
                        departureScheduler.cancelReminder(checklist.id);
                        result = ReminderScheduleResult.SUCCESS;
                    } else if (!departureScheduler.canScheduleExactAlarms()) {
                        result = ReminderScheduleResult.EXACT_ALARM_PERMISSION_REQUIRED;
                    } else if (departureScheduler.scheduleChecklistReminder(checklist)) {
                        result = ReminderScheduleResult.SUCCESS;
                    }
                }
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to schedule reminder", exception);
            }
            ReminderScheduleResult finalResult = result;
            mainHandler.post(() -> callback.onComplete(finalResult));
        });
    }

    /**
     * Returns checklist item counts by event id.
     * Must be called off the main thread.
     *
     * @return event id to item count map
     */
    @NonNull
    public Map<Long, Integer> getChecklistCountsByEventIdSync() {
        Map<Long, Integer> map = new HashMap<>();
        List<ChecklistDao.EventChecklistCount> counts = checklistDao.getChecklistCounts();
        for (ChecklistDao.EventChecklistCount count : counts) {
            map.put(count.eventId, count.itemCount);
        }
        return map;
    }

    /**
     * Returns a checklist by id.
     * Must be called off the main thread.
     *
     * @param checklistId checklist id
     * @return checklist or null
     */
    public Checklist getChecklistByIdSync(long checklistId) {
        return checklistDao.findById(checklistId);
    }

    /**
     * Returns total item count for a checklist.
     * Must be called off the main thread.
     *
     * @param checklistId checklist id
     * @return item count
     */
    public int getChecklistItemCountSync(long checklistId) {
        return checklistItemDao.countItems(checklistId);
    }

    /**
     * Returns all checklists with scheduled reminders.
     * Must be called off the main thread.
     *
     * @return scheduled checklists
     */
    @NonNull
    public List<Checklist> getScheduledChecklistsSync() {
        return checklistDao.getScheduledChecklists();
    }
}

