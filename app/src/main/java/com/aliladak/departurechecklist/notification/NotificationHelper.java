package com.aliladak.departurechecklist.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.aliladak.departurechecklist.R;
import com.aliladak.departurechecklist.data.db.entity.Checklist;
import com.aliladak.departurechecklist.ui.MainActivity;

/**
 * Builds and posts departure reminder notifications.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "departure_reminders";

    private final Context appContext;

    /**
     * Creates a helper for notification operations.
     *
     * @param context any context
     */
    public NotificationHelper(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Creates notification channel if needed and posts a reminder notification.
     *
     * @param checklist checklist payload
     * @param itemCount checklist item count
     */
    public void showDepartureReminder(@NonNull Checklist checklist, int itemCount) {
        createChannelIfNeeded();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "POST_NOTIFICATIONS not granted; skipping reminder notification");
            return;
        }

        PendingIntent openChecklistIntent = buildChecklistPendingIntent(checklist);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(appContext.getString(R.string.notification_title, checklist.eventTitle))
                .setContentText(appContext.getString(R.string.notification_body, itemCount))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openChecklistIntent)
                .addAction(0, appContext.getString(R.string.notification_action), openChecklistIntent);
        try {
            NotificationManagerCompat.from(appContext)
                    .notify((int) checklist.id, builder.build());
        } catch (SecurityException securityException) {
            Log.e(TAG, "Security exception while posting notification", securityException);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unexpected error while posting notification", exception);
        }
    }

    /**
     * Creates the reminder notification channel when running on API 26+.
     */
    public void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = appContext.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager unavailable while creating channel");
            return;
        }
        NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(appContext.getString(R.string.notification_channel_description));
        notificationManager.createNotificationChannel(channel);
    }

    private PendingIntent buildChecklistPendingIntent(@NonNull Checklist checklist) {
        Bundle arguments = new Bundle();
        arguments.putLong("eventId", checklist.eventId);
        arguments.putString("eventTitle", checklist.eventTitle);
        arguments.putLong("eventStartMillis", checklist.eventStartMillis);
        arguments.putLong("checklistId", checklist.id);
        return new NavDeepLinkBuilder(appContext)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.checklistFragment)
                .setArguments(arguments)
                .createPendingIntent();
    }
}
