package com.example.departurechecklist.ui.events;

import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.departurechecklist.R;
import com.example.departurechecklist.data.model.CalendarEvent;
import com.example.departurechecklist.databinding.ItemEventRowBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for calendar events.
 */
final class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private static final String TAG = "EventAdapter";

    interface OnEventClickListener {
        void onEventClicked(@NonNull CalendarEvent event);
    }

    private final List<EventListViewModel.EventRowItem> items;
    private final OnEventClickListener onEventClickListener;

    EventAdapter(@NonNull OnEventClickListener onEventClickListener) {
        this.items = new ArrayList<>();
        this.onEventClickListener = onEventClickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventRowBinding binding = ItemEventRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventListViewModel.EventRowItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void submitItems(@NonNull List<EventListViewModel.EventRowItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    final class EventViewHolder extends RecyclerView.ViewHolder {
        private final ItemEventRowBinding binding;

        EventViewHolder(@NonNull ItemEventRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull EventListViewModel.EventRowItem item) {
            CalendarEvent event = item.getEvent();
            binding.eventTitle.setText(event.getTitle());
            binding.eventDateTime.setText(formatEventDateTime(event));
            if (TextUtils.isEmpty(event.getLocation())) {
                binding.eventLocation.setVisibility(View.GONE);
            } else {
                binding.eventLocation.setVisibility(View.VISIBLE);
                binding.eventLocation.setText(binding.getRoot().getContext().getString(
                        R.string.event_location_format, event.getLocation()));
            }

            int count = item.getChecklistItemCount();
            if (item.hasChecklist()) {
                binding.checklistBadgeText.setText(binding.getRoot().getContext().getString(
                        R.string.checklist_badge_count, count));
                binding.checklistBadgeText.setContentDescription(binding.getRoot().getContext().getString(
                        R.string.checklist_badge_count, count));
            } else {
                binding.checklistBadgeText.setText(R.string.checklist_badge_plus);
                binding.checklistBadgeText.setContentDescription(binding.getRoot().getContext().getString(
                        R.string.checklist_add_icon));
            }

            binding.getRoot().setOnClickListener(view -> {
                try {
                    onEventClickListener.onEventClicked(event);
                } catch (RuntimeException exception) {
                    Log.e(TAG, "Failed to handle event click", exception);
                }
            });
        }

        private String formatEventDateTime(@NonNull CalendarEvent event) {
            if (event.isAllDay()) {
                return DateUtils.formatDateTime(binding.getRoot().getContext(),
                        event.getStartTimeMillis(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
            }
            boolean is24Hour = DateFormat.is24HourFormat(binding.getRoot().getContext());
            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                    | DateUtils.FORMAT_SHOW_YEAR
                    | (is24Hour ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_12HOUR);
            return DateUtils.formatDateTime(binding.getRoot().getContext(),
                    event.getStartTimeMillis(), flags);
        }
    }
}
