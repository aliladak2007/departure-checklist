package com.example.departurechecklist.ui.events;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.departurechecklist.R;
import com.example.departurechecklist.data.db.AppDatabase;
import com.example.departurechecklist.data.model.CalendarEvent;
import com.example.departurechecklist.data.repository.CalendarRepository;
import com.example.departurechecklist.data.repository.ChecklistRepository;
import com.example.departurechecklist.databinding.FragmentEventListBinding;
import com.example.departurechecklist.notification.DepartureScheduler;
import com.example.departurechecklist.ui.common.ViewModelFactory;
import com.example.departurechecklist.util.CalendarPermissionHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Fragment displaying upcoming calendar events and checklist badge counts.
 */
public class EventListFragment extends Fragment {
    private static final String TAG = "EventListFragment";

    private FragmentEventListBinding binding;
    private EventListViewModel viewModel;
    private EventAdapter eventAdapter;
    private ActivityResultLauncher<String> calendarPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    /**
     * Registers runtime permission launchers.
     *
     * @param savedInstanceState saved state bundle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        calendarPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::onCalendarPermissionResult);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Log.e(TAG, "POST_NOTIFICATIONS permission denied");
                    }
                });
    }

    /**
     * Inflates the fragment layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved state
     * @return fragment root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentEventListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes UI, dependencies, and observers.
     *
     * @param view root view
     * @param savedInstanceState saved state bundle
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            Log.e(TAG, "Binding is null in onViewCreated");
            return;
        }

        eventAdapter = new EventAdapter(this::openChecklistForEvent);
        binding.eventRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventRecyclerView.setAdapter(eventAdapter);

        AppDatabase appDatabase = AppDatabase.getInstance(requireContext().getApplicationContext());
        DepartureScheduler scheduler = new DepartureScheduler(requireContext().getApplicationContext());
        ChecklistRepository checklistRepository = new ChecklistRepository(
                appDatabase.checklistDao(),
                appDatabase.checklistItemDao(),
                scheduler);
        CalendarRepository calendarRepository = new CalendarRepository(requireContext().getApplicationContext());
        ViewModelFactory factory = ViewModelFactory.forEventList(calendarRepository, checklistRepository);
        viewModel = new ViewModelProvider(this, factory).get(EventListViewModel.class);

        observeViewModel();
        binding.swipeRefresh.setOnRefreshListener(() -> {
            if (viewModel != null) {
                viewModel.refreshEvents();
            }
        });

        ensureCalendarPermissionThenLoad();
        requestNotificationPermissionIfNeeded();
    }

    /**
     * Refreshes events each time the fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && CalendarPermissionHelper.hasReadCalendarPermission(requireContext())) {
            viewModel.refreshEvents();
        }
    }

    /**
     * Clears references tied to the fragment view lifecycle.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.eventRecyclerView.setAdapter(null);
        }
        binding = null;
        viewModel = null;
    }

    private void observeViewModel() {
        if (viewModel == null || binding == null) {
            Log.e(TAG, "Unable to observe ViewModel because dependencies are null");
            return;
        }
        viewModel.getEventItems().observe(getViewLifecycleOwner(), this::renderEvents);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                binding.swipeRefresh.setRefreshing(Boolean.TRUE.equals(loading));
            }
        });
    }

    private void renderEvents(@Nullable List<EventListViewModel.EventRowItem> events) {
        if (binding == null || eventAdapter == null) {
            Log.e(TAG, "Unable to render events because the view was destroyed");
            return;
        }
        List<EventListViewModel.EventRowItem> safeEvents = events == null ? Collections.emptyList() : events;
        eventAdapter.submitItems(safeEvents);
        if (CalendarPermissionHelper.hasReadCalendarPermission(requireContext()) && safeEvents.isEmpty()) {
            showEmptyState(
                    getString(R.string.empty_state_title),
                    getString(R.string.empty_state_subtitle));
            return;
        }
        if (CalendarPermissionHelper.hasReadCalendarPermission(requireContext())) {
            hideEmptyState();
        }
    }

    private void ensureCalendarPermissionThenLoad() {
        if (CalendarPermissionHelper.hasReadCalendarPermission(requireContext())) {
            hideEmptyState();
            if (viewModel != null) {
                viewModel.refreshEvents();
            }
            return;
        }
        if (CalendarPermissionHelper.shouldShowCalendarPermissionRationale(this)) {
            showCalendarPermissionRationale();
        } else {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR);
        }
    }

    private void showCalendarPermissionRationale() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.calendar_permission_rationale_title)
                .setMessage(R.string.calendar_permission_rationale_message)
                .setPositiveButton(R.string.permission_positive,
                        (dialogInterface, which) -> calendarPermissionLauncher.launch(
                                Manifest.permission.READ_CALENDAR))
                .setNegativeButton(R.string.permission_negative, null)
                .show();
    }

    private void onCalendarPermissionResult(boolean granted) {
        if (granted) {
            if (viewModel != null) {
                viewModel.refreshEvents();
            }
            hideEmptyState();
        } else {
            showEmptyState(getString(R.string.empty_permission_title),
                    getString(R.string.empty_permission_subtitle));
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (CalendarPermissionHelper.shouldShowNotificationPermissionRationale(this)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.notification_permission_rationale_title)
                    .setMessage(R.string.notification_permission_rationale_message)
                    .setPositiveButton(R.string.permission_positive,
                            (dialogInterface, which) -> notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.permission_negative, null)
                    .show();
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void openChecklistForEvent(@NonNull CalendarEvent event) {
        try {
            Bundle args = new Bundle();
            args.putLong("eventId", event.getId());
            args.putString("eventTitle", event.getTitle());
            args.putLong("eventStartMillis", event.getStartTimeMillis());
            args.putLong("checklistId", -1L);
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                    R.id.action_eventListFragment_to_checklistFragment,
                    args);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to navigate to checklist", exception);
        }
    }

    private void showEmptyState(@NonNull String title, @NonNull String subtitle) {
        if (binding == null) {
            Log.e(TAG, "Cannot show empty state because binding is null");
            return;
        }
        binding.emptyState.emptyStateTitle.setText(title);
        binding.emptyState.emptyStateSubtitle.setText(subtitle);
        binding.emptyState.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (binding == null) {
            Log.e(TAG, "Cannot hide empty state because binding is null");
            return;
        }
        binding.emptyState.getRoot().setVisibility(View.GONE);
    }
}
