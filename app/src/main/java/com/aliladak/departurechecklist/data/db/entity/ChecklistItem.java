package com.aliladak.departurechecklist.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing an item belonging to a checklist.
 */
@Entity(tableName = "checklist_items",
        foreignKeys = @ForeignKey(entity = Checklist.class,
                parentColumns = "id",
                childColumns = "checklistId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("checklistId")})
public class ChecklistItem {
    public static final String TAG = "ChecklistItem";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long checklistId;
    public String label;
    public boolean isChecked;
    public int sortOrder;
}
