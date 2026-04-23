package com.example.departurechecklist.data.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.departurechecklist.data.db.entity.Checklist;
import com.example.departurechecklist.data.db.entity.ChecklistItem;

import java.util.List;

/**
 * Room relation wrapper exposing a checklist with its child items.
 */
public class ChecklistWithItems {
    public static final String TAG = "ChecklistWithItems";

    @Embedded
    public Checklist checklist;

    @Relation(parentColumn = "id", entityColumn = "checklistId")
    public List<ChecklistItem> items;
}
