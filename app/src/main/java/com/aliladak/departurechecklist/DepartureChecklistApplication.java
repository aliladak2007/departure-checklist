package com.aliladak.departurechecklist;

import android.app.Application;
import android.util.Log;

import com.google.android.material.color.DynamicColors;

/**
 * Application entry point that enables Material dynamic colors when available.
 */
public class DepartureChecklistApplication extends Application {
    private static final String TAG = "DepartureChecklistApp";

    /**
     * Initializes global app configuration.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            DynamicColors.applyToActivitiesIfAvailable(this);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to apply dynamic colors", exception);
        }
    }
}

