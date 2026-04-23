package com.aliladak.departurechecklist.ui.common;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aliladak.departurechecklist.data.repository.CalendarRepository;
import com.aliladak.departurechecklist.data.repository.ChecklistRepository;
import com.aliladak.departurechecklist.ui.checklist.ChecklistViewModel;
import com.aliladak.departurechecklist.ui.events.EventListViewModel;

/**
 * Manual dependency injection factory for all ViewModels.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {
    private static final String TAG = "ViewModelFactory";

    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;

    /**
     * Creates a factory for the event list screen.
     *
     * @param calendarRepository calendar repository
     * @param checklistRepository checklist repository
     * @return factory instance
     */
    @NonNull
    public static ViewModelFactory forEventList(@NonNull CalendarRepository calendarRepository,
            @NonNull ChecklistRepository checklistRepository) {
        return new ViewModelFactory(calendarRepository, checklistRepository);
    }

    /**
     * Creates a factory for the checklist screen.
     *
     * @param checklistRepository checklist repository
     * @return factory instance
     */
    @NonNull
    public static ViewModelFactory forChecklist(@NonNull ChecklistRepository checklistRepository) {
        return new ViewModelFactory(null, checklistRepository);
    }

    private ViewModelFactory(CalendarRepository calendarRepository,
            @NonNull ChecklistRepository checklistRepository) {
        this.calendarRepository = calendarRepository;
        this.checklistRepository = checklistRepository;
    }

    /**
     * Creates the requested ViewModel type.
     *
     * @param modelClass requested class
     * @param <T> viewmodel type
     * @return created ViewModel
     */
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EventListViewModel.class)) {
            if (calendarRepository == null) {
                Log.e(TAG, "CalendarRepository is required for EventListViewModel");
                throw new IllegalStateException("CalendarRepository missing");
            }
            return (T) new EventListViewModel(calendarRepository, checklistRepository);
        }
        if (modelClass.isAssignableFrom(ChecklistViewModel.class)) {
            return (T) new ChecklistViewModel(checklistRepository);
        }
        Log.e(TAG, "Unknown ViewModel class: " + modelClass.getName());
        throw new IllegalArgumentException("Unknown ViewModel class " + modelClass.getName());
    }
}

