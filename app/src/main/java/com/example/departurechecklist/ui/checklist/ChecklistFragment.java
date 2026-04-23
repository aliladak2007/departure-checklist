package com.example.departurechecklist.ui.checklist;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.departurechecklist.R;
import com.example.departurechecklist.data.db.AppDatabase;
import com.example.departurechecklist.data.db.ChecklistWithItems;
import com.example.departurechecklist.data.db.entity.Checklist;
import com.example.departurechecklist.data.db.entity.ChecklistItem;
import com.example.departurechecklist.data.repository.ChecklistRepository;
import com.example.departurechecklist.databinding.FragmentChecklistBinding;
import com.example.departurechecklist.notification.DepartureScheduler;
import com.example.departurechecklist.ui.common.ViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment that manages checklist items for a selected calendar event.
 */
public class ChecklistFragment extends Fragment {
    private static final String TAG = "ChecklistFragment";

    private FragmentChecklistBinding binding;
    private ChecklistViewModel viewModel;
    private ChecklistItemAdapter itemAdapter;
    private ChecklistFragmentArgs args;
    private int currentReminderMinutes;

    /**
     * Inflates the fragment layout.
     *
     * @param inflater layout inflater
     * @param container parent container
     * @param savedInstanceState saved state bundle
     * @return root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChecklistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Sets up dependencies, adapters, and observers.
     *
     * @param view root view
     * @param savedInstanceState saved instance state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            Log.e(TAG, "Binding is null in onViewCreated");
            return;
        }
        args = ChecklistFragmentArgs.fromBundle(getArguments() == null ? new Bundle() : getArguments());

        itemAdapter = new ChecklistItemAdapter(new ChecklistItemAdapter.ItemActions() {
            @Override
            public void onCheckedChanged(@NonNull ChecklistItem item, boolean checked) {
                if (viewModel != null) {
                    viewModel.updateItemChecked(item, checked);
                }
            }

            @Override
            public void onDeleteClicked(@NonNull ChecklistItem item) {
                showDeleteItemConfirmation(item);
            }
        });
        binding.checklistRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.checklistRecyclerView.setAdapter(itemAdapter);

        AppDatabase appDatabase = AppDatabase.getInstance(requireContext().getApplicationContext());
        DepartureScheduler scheduler = new DepartureScheduler(requireContext().getApplicationContext());
        ChecklistRepository checklistRepository = new ChecklistRepository(
                appDatabase.checklistDao(),
                appDatabase.checklistItemDao(),
                scheduler);
        ViewModelFactory factory = ViewModelFactory.forChecklist(checklistRepository);
        viewModel = new ViewModelProvider(this, factory).get(ChecklistViewModel.class);
        viewModel.initialize(args.getEventId(), args.getEventTitle(), args.getEventStartMillis(),
                args.getChecklistId());

        binding.checklistTitle.setText(args.getEventTitle());
        binding.checklistSubtitle.setText(getString(R.string.checklist_items_count, 0));
        binding.checklistEmptyState.emptyStateTitle.setText(R.string.empty_checklist_title);
        binding.checklistEmptyState.emptyStateSubtitle.setText(R.string.empty_checklist_subtitle);

        binding.addItemButton.setOnClickListener(ignored -> addItemFromInput());
        binding.scheduleReminderButton.setOnClickListener(ignored -> showReminderMinutesPicker());
        binding.clearChecklistButton.setOnClickListener(ignored -> showClearChecklistConfirmation());

        observeViewModel();
    }

    /**
     * Clears references tied to the view lifecycle.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.checklistRecyclerView.setAdapter(null);
        }
        binding = null;
        viewModel = null;
    }

    private void observeViewModel() {
        if (viewModel == null || binding == null) {
            Log.e(TAG, "Cannot observe ViewModel because required references are null");
            return;
        }
        viewModel.getChecklist().observe(getViewLifecycleOwner(), this::renderChecklist);
        viewModel.getAddItemValidationError().observe(getViewLifecycleOwner(), hasError -> {
            if (binding == null) {
                return;
            }
            boolean showError = Boolean.TRUE.equals(hasError);
            binding.addItemInputLayout.setError(showError
                    ? getString(R.string.checklist_item_empty_error)
                    : null);
            if (showError && viewModel != null) {
                Toast.makeText(requireContext(), R.string.checklist_item_empty_error, Toast.LENGTH_SHORT).show();
                viewModel.clearAddItemValidationError();
            }
        });
        viewModel.getReminderEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null || viewModel == null) {
                return;
            }
            if (event.result == ChecklistRepository.ReminderScheduleResult.SUCCESS) {
                Toast.makeText(requireContext(),
                        getString(R.string.reminder_scheduled, event.minutesBefore),
                        Toast.LENGTH_SHORT).show();
            } else if (event.result == ChecklistRepository.ReminderScheduleResult.EXACT_ALARM_PERMISSION_REQUIRED) {
                showExactAlarmPermissionDialog();
            } else if (event.result == ChecklistRepository.ReminderScheduleResult.LEAD_TIME_TOO_LONG) {
                Toast.makeText(requireContext(),
                        "Reminder lead time exceeds time until event. Please choose a shorter lead time.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), R.string.reminder_schedule_failed, Toast.LENGTH_SHORT).show();
            }
            viewModel.clearReminderEvent();
        });
    }

    private void renderChecklist(@Nullable ChecklistWithItems checklistWithItems) {
        if (binding == null || itemAdapter == null) {
            Log.e(TAG, "Cannot render checklist because the view has been destroyed");
            return;
        }
        if (checklistWithItems == null || checklistWithItems.checklist == null) {
            itemAdapter.submitItems(new ArrayList<>());
            binding.checklistEmptyState.getRoot().setVisibility(View.VISIBLE);
            binding.checklistRecyclerView.setVisibility(View.GONE);
            return;
        }

        Checklist checklist = checklistWithItems.checklist;
        currentReminderMinutes = checklist.reminderMinutesBefore;
        if (checklist.eventTitle != null && !checklist.eventTitle.trim().isEmpty()) {
            binding.checklistTitle.setText(checklist.eventTitle);
        }
        List<ChecklistItem> sortedItems = new ArrayList<>();
        if (checklistWithItems.items != null) {
            sortedItems.addAll(checklistWithItems.items);
            sortedItems.sort(Comparator.comparingInt(item -> item.sortOrder));
        }
        itemAdapter.submitItems(sortedItems);
        binding.checklistSubtitle.setText(getString(R.string.checklist_items_count, sortedItems.size()));
        boolean isEmpty = sortedItems.isEmpty();
        binding.checklistEmptyState.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.checklistRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void addItemFromInput() {
        if (binding == null || viewModel == null) {
            Log.e(TAG, "Cannot add item because dependencies are null");
            return;
        }
        String label = binding.addItemEditText.getText() == null
                ? ""
                : binding.addItemEditText.getText().toString();
        viewModel.addItem(label);
        if (!label.trim().isEmpty()) {
            binding.addItemEditText.setText("");
        }
    }

    private void showDeleteItemConfirmation(@NonNull ChecklistItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_item_title)
                .setMessage(R.string.delete_item_message)
                .setPositiveButton(R.string.delete_positive, (dialogInterface, which) -> {
                    if (viewModel != null) {
                        viewModel.deleteItem(item.id);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showClearChecklistConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_checklist_title)
                .setMessage(R.string.clear_checklist_message)
                .setPositiveButton(R.string.delete_positive, (dialogInterface, which) -> {
                    if (viewModel != null) {
                        viewModel.clearChecklist();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showReminderMinutesPicker() {
        int defaultMinutes = currentReminderMinutes > 0 ? currentReminderMinutes : 30;
        int initialHour = defaultMinutes / 60;
        int initialMinute = defaultMinutes % 60;
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (timePicker, selectedHour, selectedMinute) -> {
                    int minutes = (selectedHour * 60) + selectedMinute;
                    if (minutes <= 0) {
                        Toast.makeText(requireContext(), R.string.minutes_picker_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long timeUntilEventMs = args.getEventStartMillis() - System.currentTimeMillis();
                    long reminderMs = minutes * 60L * 1000L;
                    if (reminderMs >= timeUntilEventMs) {
                        Toast.makeText(requireContext(),
                                getString(R.string.reminder_lead_time_too_long),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (viewModel != null) {
                        viewModel.scheduleReminder(minutes);
                    }
                },
                initialHour,
                initialMinute,
                true);
        dialog.setTitle(R.string.minutes_picker_title);
        dialog.show();
    }

    private void showExactAlarmPermissionDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.schedule_reminder)
                .setMessage(R.string.exact_alarm_permission_message)
                .setPositiveButton(R.string.exact_alarm_permission_open_settings,
                        (dialogInterface, which) -> openExactAlarmSettings())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        try {
            startActivity(intent);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to open exact alarm settings", exception);
        }
    }
}