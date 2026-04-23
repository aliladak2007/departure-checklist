package com.example.departurechecklist.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.departurechecklist.data.db.entity.ChecklistItem;

/**
 * DAO for checklist item records.
 */
@Dao
public interface ChecklistItemDao {
    String TAG = "ChecklistItemDao";

    /** @return generated identifier */
    @Insert
    long insert(ChecklistItem item);

    /** Updates a checklist item. */
    @Update
    void update(ChecklistItem item);

    /** Deletes a checklist item by id. */
    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    void deleteById(long itemId);

    /** Deletes all items in a checklist. */
    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId")
    void deleteForChecklist(long checklistId);

    /** @return max sort order plus one */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM checklist_items WHERE checklistId = :checklistId")
    int getNextSortOrder(long checklistId);

    /** @return number of rows */
    @Query("SELECT COUNT(*) FROM checklist_items WHERE checklistId = :checklistId")
    int countItems(long checklistId);
}
