package com.example.departurechecklist.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.departurechecklist.data.db.AppDatabase;
import com.example.departurechecklist.data.db.entity.Checklist;
import com.example.departurechecklist.data.repository.ChecklistRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Receiver that re-schedules pending reminders after reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Handles BOOT_COMPLETED and re-schedules reminders from the database.
     *
     * @param context receiver context
     * @param intent received intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e(TAG, "Context or intent was null in onReceive");
            return;
        }
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> rescheduleReminders(appContext, pendingResult));
    }

    private void rescheduleReminders(@NonNull Context context, @Nullable PendingResult pendingResult) {
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            DepartureScheduler scheduler = new DepartureScheduler(context);
            ChecklistRepository repository = new ChecklistRepository(
                    database.checklistDao(),
                    database.checklistItemDao(),
                    scheduler);
            List<Checklist> scheduled = repository.getScheduledChecklistsSync();
            for (Checklist checklist : scheduled) {
                scheduler.scheduleChecklistReminder(checklist);
            }
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to re-schedule reminders after reboot", exception);
        } finally {
            if (pendingResult != null) {
                pendingResult.finish();
            }
        }
    }
}

