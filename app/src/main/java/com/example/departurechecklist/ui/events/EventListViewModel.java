package com.example.departurechecklist.ui.events;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.departurechecklist.data.model.CalendarEvent;
import com.example.departurechecklist.data.repository.CalendarRepository;
import com.example.departurechecklist.data.repository.ChecklistRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for loading upcoming events and checklist badge counts.
 */
public class EventListViewModel extends ViewModel {
    private static final String TAG = "EventListViewModel";

    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;
    private final ExecutorService ioExecutor;
    private final MutableLiveData<List<EventRowItem>> eventItems;
    private final MutableLiveData<Boolean> loading;

    /**
     * Immutable UI row model for the event list.
     */
    public static class EventRowItem {
        public static final String TAG = "EventRowItem";

        private final CalendarEvent event;
        private final int checklistItemCount;
        private final boolean hasChecklist;

        /**
         * Creates a row model.
         *
         * @param event calendar event
         * @param checklistItemCount attached checklist item count
         */
        public EventRowItem(@NonNull CalendarEvent event, int checklistItemCount, boolean hasChecklist) {
            this.event = event;
            this.checklistItemCount = checklistItemCount;
            this.hasChecklist = hasChecklist;
        }

        /**
         * Returns the event payload.
         *
         * @return calendar event
         */
        @NonNull
        public CalendarEvent getEvent() {
            return event;
        }

        /**
         * Returns checklist item count for this event.
         *
         * @return checklist item count
         */
        public int getChecklistItemCount() {
            return checklistItemCount;
        }

        /**
         * Returns true when an event already has an attached checklist.
         *
         * @return true when a checklist exists
         */
        public boolean hasChecklist() {
            return hasChecklist;
        }
    }

    /**
     * Creates the ViewModel.
     *
     * @param calendarRepository calendar repository
     * @param checklistRepository checklist repository
     */
    public EventListViewModel(@NonNull CalendarRepository calendarRepository,
            @NonNull ChecklistRepository checklistRepository) {
        this.calendarRepository = calendarRepository;
        this.checklistRepository = checklistRepository;
        this.ioExecutor = Executors.newSingleThreadExecutor();
        this.eventItems = new MutableLiveData<>(Collections.emptyList());
        this.loading = new MutableLiveData<>(false);
    }

    /**
     * Observes event rows.
     *
     * @return live list of event rows
     */
    @NonNull
    public LiveData<List<EventRowItem>> getEventItems() {
        return eventItems;
    }

    /**
     * Observes loading state.
     *
     * @return true while refreshing events
     */
    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * Refreshes upcoming events and associated checklist counts.
     */
    public void refreshEvents() {
        loading.setValue(true);
        ioExecutor.execute(() -> {
            try {
                List<CalendarEvent> events = calendarRepository.getUpcomingEvents();
                Map<Long, Integer> counts = checklistRepository.getChecklistCountsByEventIdSync();
                List<EventRowItem> rows = new ArrayList<>();
                for (CalendarEvent event : events) {
                    boolean hasChecklist = counts.containsKey(event.getId());
                    int count = hasChecklist ? counts.get(event.getId()) : 0;
                    rows.add(new EventRowItem(event, count, hasChecklist));
                }
                eventItems.postValue(rows);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Failed to refresh event list", exception);
                eventItems.postValue(Collections.emptyList());
            } finally {
                loading.postValue(false);
            }
        });
    }

    /**
     * Clears ViewModel resources.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }
}
