package com.aliladak.departurechecklist.ui.checklist;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aliladak.departurechecklist.data.db.ChecklistWithItems;
import com.aliladak.departurechecklist.data.db.entity.Checklist;
import com.aliladak.departurechecklist.data.db.entity.ChecklistItem;
import com.aliladak.departurechecklist.data.repository.ChecklistRepository;

/**
 * ViewModel for checklist CRUD and reminder scheduling.
 */
public class ChecklistViewModel extends ViewModel {
    private static final String TAG = "ChecklistViewModel";

    /**
     * One-off reminder scheduling UI event.
     */
    public static class ReminderUiEvent {
        public static final String TAG = "ReminderUiEvent";

        public final ChecklistRepository.ReminderScheduleResult result;
        public final int minutesBefore;

        /**
         * Creates a reminder UI event.
         *
         * @param result schedule result
         * @param minutesBefore selected minutes before event
         */
        public ReminderUiEvent(@NonNull ChecklistRepository.ReminderScheduleResult result,
                int minutesBefore) {
            this.result = result;
            this.minutesBefore = minutesBefore;
        }
    }

    private final ChecklistRepository checklistRepository;
    private final MediatorLiveData<ChecklistWithItems> checklist;
    private final MutableLiveData<ReminderUiEvent> reminderEvent;
    private final MutableLiveData<Boolean> addItemValidationError;

    private LiveData<ChecklistWithItems> activeSource;
    private boolean initialized;

    /**
     * Creates the ViewModel.
     *
     * @param checklistRepository checklist repository
     */
    public ChecklistViewModel(@NonNull ChecklistRepository checklistRepository) {
        this.checklistRepository = checklistRepository;
        this.checklist = new MediatorLiveData<>();
        this.reminderEvent = new MutableLiveData<>();
        this.addItemValidationError = new MutableLiveData<>(false);
        this.initialized = false;
    }

    /**
     * Initializes checklist observation for the provided args.
     *
     * @param eventId calendar event id
     * @param eventTitle event title
     * @param eventStartMillis event start time
     * @param checklistId optional checklist id for deep links
     */
    public void initialize(long eventId, @NonNull String eventTitle, long eventStartMillis,
            long checklistId) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (activeSource != null) {
            checklist.removeSource(activeSource);
        }
        if (checklistId > 0) {
            activeSource = checklistRepository.observeChecklistById(checklistId);
        } else {
            activeSource = checklistRepository.observeChecklistByEventId(eventId);
            checklistRepository.ensureChecklistExists(eventId, eventTitle, eventStartMillis);
        }
        checklist.addSource(activeSource, checklist::setValue);
    }

    /**
     * Observes checklist relation.
     *
     * @return live checklist relation
     */
    @NonNull
    public LiveData<ChecklistWithItems> getChecklist() {
        return checklist;
    }

    /**
     * Observes reminder scheduling UI events.
     *
     * @return one-off reminder event stream
     */
    @NonNull
    public LiveData<ReminderUiEvent> getReminderEvent() {
        return reminderEvent;
    }

    /**
     * Observes add-item validation errors.
     *
     * @return true when latest add attempt failed validation
     */
    @NonNull
    public LiveData<Boolean> getAddItemValidationError() {
        return addItemValidationError;
    }

    /**
     * Adds a new checklist item.
     *
     * @param label item label
     */
    public void addItem(@NonNull String label) {
        Checklist currentChecklist = getCurrentChecklist();
        if (currentChecklist == null) {
            Log.e(TAG, "Cannot add checklist item because checklist is unavailable");
            return;
        }
        String normalized = label.trim();
        if (normalized.isEmpty()) {
            addItemValidationError.setValue(true);
            return;
        }
        addItemValidationError.setValue(false);
        checklistRepository.addChecklistItem(currentChecklist.id, normalized);
    }

    /**
     * Persists checked state for an item.
     *
     * @param item item to update
     * @param checked checked value
     */
    public void updateItemChecked(@NonNull ChecklistItem item, boolean checked) {
        item.isChecked = checked;
        checklistRepository.updateChecklistItem(item);
    }

    /**
     * Deletes an item row.
     *
     * @param itemId item identifier
     */
    public void deleteItem(long itemId) {
        checklistRepository.deleteChecklistItem(itemId);
    }

    /**
     * Clears all checklist items.
     */
    public void clearChecklist() {
        Checklist currentChecklist = getCurrentChecklist();
        if (currentChecklist == null) {
            Log.e(TAG, "Cannot clear checklist because checklist is unavailable");
            return;
        }
        checklistRepository.clearChecklistItems(currentChecklist.id);
    }

    /**
     * Updates reminder lead time and schedules the notification alarm.
     *
     * @param minutesBefore minutes before the event
     */
    public void scheduleReminder(int minutesBefore) {
        Checklist currentChecklist = getCurrentChecklist();
        if (currentChecklist == null) {
            Log.e(TAG, "Cannot schedule reminder because checklist is unavailable");
            reminderEvent.setValue(new ReminderUiEvent(ChecklistRepository.ReminderScheduleResult.ERROR,
                    minutesBefore));
            return;
        }

        // Add this block:
        long triggerAtMillis = currentChecklist.eventStartMillis - (minutesBefore * 60L * 1000L);
        if (triggerAtMillis <= System.currentTimeMillis()) {
            reminderEvent.setValue(new ReminderUiEvent(
                    ChecklistRepository.ReminderScheduleResult.LEAD_TIME_TOO_LONG,
                    minutesBefore));
            return;
        }

        checklistRepository.scheduleReminder(currentChecklist.id, minutesBefore,
                result -> reminderEvent.setValue(new ReminderUiEvent(result, minutesBefore)));
    }
    /**
     * Clears the add-item validation error state.
     */
    public void clearAddItemValidationError() {
        addItemValidationError.setValue(false);
    }

    /**
     * Clears the last reminder event so it is not consumed again.
     */
    public void clearReminderEvent() {
        reminderEvent.setValue(null);
    }

    private Checklist getCurrentChecklist() {
        ChecklistWithItems value = checklist.getValue();
        return value == null ? null : value.checklist;
    }
}

