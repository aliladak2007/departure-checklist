package com.example.departurechecklist.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.departurechecklist.R;
import com.example.departurechecklist.data.model.CalendarEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for read-only access to local CalendarContract events.
 */
public class CalendarRepository {
    private static final String TAG = "CalendarRepository";
    private static final long FOURTEEN_DAYS_IN_MILLIS = 14L * 24L * 60L * 60L * 1000L;

    private final Context appContext;

    /**
     * Creates a calendar repository.
     *
     * @param context any context
     */
    public CalendarRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Returns upcoming events for the next 14 days sorted by start time.
     *
     * @return list of upcoming events
     */
    @NonNull
    public List<CalendarEvent> getUpcomingEvents() {
        List<CalendarEvent> events = new ArrayList<>();
        long now = System.currentTimeMillis();
        long endRange = now + FOURTEEN_DAYS_IN_MILLIS;

        String[] projection = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.ALL_DAY
        };

        String selection = CalendarContract.Events.DTSTART + " >= ? AND "
                + CalendarContract.Events.DTSTART + " <= ? AND "
                + CalendarContract.Events.DELETED + " = 0";
        String[] selectionArgs = new String[]{String.valueOf(now), String.valueOf(endRange)};

        Cursor cursor = null;
        try {
            cursor = appContext.getContentResolver().query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    CalendarContract.Events.DTSTART + " ASC");
            if (cursor == null) {
                Log.e(TAG, "Calendar query returned a null cursor");
                return events;
            }
            int idColumn = cursor.getColumnIndex(CalendarContract.Events._ID);
            int titleColumn = cursor.getColumnIndex(CalendarContract.Events.TITLE);
            int startColumn = cursor.getColumnIndex(CalendarContract.Events.DTSTART);
            int endColumn = cursor.getColumnIndex(CalendarContract.Events.DTEND);
            int locationColumn = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION);
            int allDayColumn = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY);

            while (cursor.moveToNext()) {
                if (idColumn < 0 || startColumn < 0 || endColumn < 0 || allDayColumn < 0) {
                    Log.e(TAG, "Required calendar columns are missing");
                    break;
                }
                long id = cursor.getLong(idColumn);
                String title = titleColumn >= 0 ? cursor.getString(titleColumn) : null;
                if (title == null || title.trim().isEmpty()) {
                    title = appContext.getString(R.string.event_title_fallback);
                }
                long startMillis = cursor.getLong(startColumn);
                long endMillis = cursor.getLong(endColumn);
                String location = locationColumn >= 0 ? cursor.getString(locationColumn) : null;
                boolean allDay = cursor.getInt(allDayColumn) == 1;
                events.add(new CalendarEvent(id, title, startMillis, endMillis, location, allDay));
            }
        } catch (SecurityException securityException) {
            Log.e(TAG, "Calendar permission missing while querying events", securityException);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unexpected error loading calendar events", exception);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }
}

