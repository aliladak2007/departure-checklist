package com.example.departurechecklist.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.departurechecklist.data.db.entity.Checklist;

/**
 * Schedules and cancels exact departure reminder alarms.
 */
public class DepartureScheduler {
    private static final String TAG = "DepartureScheduler";

    public static final String ACTION_DEPARTURE_REMINDER =
            "com.example.departurechecklist.ACTION_DEPARTURE_REMINDER";
    public static final String EXTRA_CHECKLIST_ID = "extra_checklist_id";

    private final Context appContext;

    /**
     * Creates a scheduler for alarm operations.
     *
     * @param context any context
     */
    public DepartureScheduler(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Schedules an exact alarm for a checklist reminder.
     *
     * @param checklist checklist metadata
     * @return true when scheduling succeeded
     */
    public boolean scheduleChecklistReminder(@NonNull Checklist checklist) {
        AlarmManager alarmManager = appContext.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is unavailable");
            return false;
        }
        if (!canScheduleExactAlarms()) {
            Log.e(TAG, "Exact alarm permission is not granted");
            return false;
        }
        long triggerAtMillis = checklist.eventStartMillis
                - (checklist.reminderMinutesBefore * 60L * 1000L);
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.e(TAG, "Alarm trigger time is in the past for checklistId=" + checklist.id);
            return false;
        }

        PendingIntent pendingIntent = buildPendingIntent(checklist.id);
        alarmManager.cancel(pendingIntent);
        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent);
            return true;
        } catch (SecurityException securityException) {
            Log.e(TAG, "Security exception while scheduling alarm", securityException);
            return false;
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unexpected failure while scheduling alarm", exception);
            return false;
        }
    }

    /**
     * Cancels an existing reminder for a checklist.
     *
     * @param checklistId checklist identifier
     */
    public void cancelReminder(long checklistId) {
        AlarmManager alarmManager = appContext.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is unavailable while cancelling alarm");
            return;
        }
        alarmManager.cancel(buildPendingIntent(checklistId));
    }

    /**
     * Returns true when exact alarms can be scheduled on this device.
     *
     * @return true when exact alarms are available
     */
    public boolean canScheduleExactAlarms() {
        AlarmManager alarmManager = appContext.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is unavailable for exact alarm permission check");
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }

    private PendingIntent buildPendingIntent(long checklistId) {
        Intent intent = new Intent(appContext, DepartureReceiver.class);
        intent.setAction(ACTION_DEPARTURE_REMINDER);
        intent.putExtra(EXTRA_CHECKLIST_ID, checklistId);
        return PendingIntent.getBroadcast(
                appContext,
                getRequestCode(checklistId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private int getRequestCode(long checklistId) {
        return (int) (checklistId ^ (checklistId >>> 32));
    }
}

