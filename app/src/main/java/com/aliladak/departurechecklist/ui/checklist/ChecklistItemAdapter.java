package com.aliladak.departurechecklist.ui.checklist;

import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliladak.departurechecklist.data.db.entity.ChecklistItem;
import com.aliladak.departurechecklist.databinding.ItemChecklistItemBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for checklist item rows.
 */
final class ChecklistItemAdapter extends RecyclerView.Adapter<ChecklistItemAdapter.ChecklistItemViewHolder> {
    private static final String TAG = "ChecklistItemAdapter";

    interface ItemActions {
        void onCheckedChanged(@NonNull ChecklistItem item, boolean checked);

        void onDeleteClicked(@NonNull ChecklistItem item);
    }

    private final List<ChecklistItem> items;
    private final ItemActions itemActions;

    ChecklistItemAdapter(@NonNull ItemActions itemActions) {
        this.items = new ArrayList<>();
        this.itemActions = itemActions;
    }

    @NonNull
    @Override
    public ChecklistItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChecklistItemBinding binding = ItemChecklistItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ChecklistItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChecklistItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void submitItems(@NonNull List<ChecklistItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    final class ChecklistItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemChecklistItemBinding binding;

        ChecklistItemViewHolder(@NonNull ItemChecklistItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ChecklistItem item) {
            binding.itemCheckBox.setOnCheckedChangeListener(null);
            binding.itemCheckBox.setChecked(item.isChecked);
            binding.itemLabel.setText(item.label);
            applyCheckedStyle(item.isChecked);

            binding.itemCheckBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                try {
                    itemActions.onCheckedChanged(item, isChecked);
                    applyCheckedStyle(isChecked);
                } catch (RuntimeException exception) {
                    Log.e(TAG, "Failed to toggle checklist item", exception);
                }
            });
            binding.deleteItemButton.setOnClickListener(view -> {
                try {
                    itemActions.onDeleteClicked(item);
                } catch (RuntimeException exception) {
                    Log.e(TAG, "Failed to delete checklist item", exception);
                }
            });
        }

        private void applyCheckedStyle(boolean checked) {
            if (checked) {
                binding.itemLabel.setPaintFlags(binding.itemLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                binding.itemLabel.setPaintFlags(binding.itemLabel.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}

