package com.aliladak.departurechecklist.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Utility methods for runtime calendar and notification permission checks.
 */
public final class CalendarPermissionHelper {
    private static final String TAG = "CalendarPermissionHelper";

    private CalendarPermissionHelper() {
    }

    /**
     * Returns true when READ_CALENDAR has been granted.
     *
     * @param context any context
     * @return true when calendar permission is granted
     */
    public static boolean hasReadCalendarPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns true when the framework recommends showing a calendar rationale.
     *
     * @param fragment host fragment
     * @return true when rationale should be shown
     */
    public static boolean shouldShowCalendarPermissionRationale(Fragment fragment) {
        return fragment.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR);
    }

    /**
     * Returns true when POST_NOTIFICATIONS is granted or not required on this API level.
     *
     * @param context any context
     * @return true when notifications may be posted
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        boolean granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            Log.e(TAG, "Notification permission has not been granted");
        }
        return granted;
    }

    /**
     * Returns true when the framework recommends showing a notification rationale.
     *
     * @param fragment host fragment
     * @return true when rationale should be shown
     */
    public static boolean shouldShowNotificationPermissionRationale(Fragment fragment) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        return fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
    }
}

