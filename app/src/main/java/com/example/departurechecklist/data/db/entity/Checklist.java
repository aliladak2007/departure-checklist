package com.example.departurechecklist.data.db.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity storing checklist metadata for a calendar event.
 */
@Entity(tableName = "checklists", indices = {@Index(value = {"eventId"}, unique = true)})
public class Checklist {
    public static final String TAG = "Checklist";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long eventId;
    public String eventTitle;
    public long eventStartMillis;
    public int reminderMinutesBefore;
}
