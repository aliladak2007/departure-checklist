package com.aliladak.departurechecklist.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliladak.departurechecklist.data.db.AppDatabase;
import com.aliladak.departurechecklist.data.db.entity.Checklist;
import com.aliladak.departurechecklist.data.repository.ChecklistRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver that posts departure reminders when alarms fire.
 */
public class DepartureReceiver extends BroadcastReceiver {
    private static final String TAG = "DepartureReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Handles reminder alarm broadcasts.
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
        long checklistId = intent.getLongExtra(DepartureScheduler.EXTRA_CHECKLIST_ID, -1L);
        if (checklistId <= 0L) {
            Log.e(TAG, "Missing checklistId extra in reminder intent");
            return;
        }
        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> handleReminder(appContext, checklistId, pendingResult));
    }

    private void handleReminder(@NonNull Context context, long checklistId,
            @Nullable PendingResult pendingResult) {
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            ChecklistRepository repository = new ChecklistRepository(
                    database.checklistDao(),
                    database.checklistItemDao(),
                    new DepartureScheduler(context));
            Checklist checklist = repository.getChecklistByIdSync(checklistId);
            if (checklist == null) {
                Log.e(TAG, "No checklist found for reminder checklistId=" + checklistId);
                return;
            }
            int itemCount = repository.getChecklistItemCountSync(checklistId);
            new NotificationHelper(context).showDepartureReminder(checklist, itemCount);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed while handling reminder alarm", exception);
        } finally {
            if (pendingResult != null) {
                pendingResult.finish();
            }
        }
    }
}

