package com.example.departurechecklist.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.departurechecklist.data.db.ChecklistWithItems;
import com.example.departurechecklist.data.db.entity.Checklist;

import java.util.List;

/**
 * DAO for checklist records.
 */
@Dao
public interface ChecklistDao {
    String TAG = "ChecklistDao";

    /** @return generated identifier */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Checklist checklist);

    /** Updates a checklist row. */
    @Update
    void update(Checklist checklist);

    /** @return live checklist with items */
    @Transaction
    @Query("SELECT * FROM checklists WHERE eventId = :eventId LIMIT 1")
    LiveData<ChecklistWithItems> observeChecklistWithItemsByEventId(long eventId);

    /** @return live checklist with items */
    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :checklistId LIMIT 1")
    LiveData<ChecklistWithItems> observeChecklistWithItemsById(long checklistId);

    /** @return matching checklist or null */
    @Query("SELECT * FROM checklists WHERE eventId = :eventId LIMIT 1")
    Checklist findByEventId(long eventId);

    /** @return matching checklist or null */
    @Query("SELECT * FROM checklists WHERE id = :checklistId LIMIT 1")
    Checklist findById(long checklistId);

    /** @return scheduled checklists */
    @Query("SELECT * FROM checklists WHERE reminderMinutesBefore > 0")
    List<Checklist> getScheduledChecklists();

    /** @return lightweight count projection */
    @Query("SELECT eventId, COUNT(checklist_items.id) AS itemCount "
            + "FROM checklists LEFT JOIN checklist_items ON checklists.id = checklist_items.checklistId "
            + "GROUP BY checklists.eventId")
    List<EventChecklistCount> getChecklistCounts();

    /** Deletes a checklist row. */
    @Query("DELETE FROM checklists WHERE id = :checklistId")
    void deleteChecklist(long checklistId);

    /**
     * Projection for event item counts.
     */
    class EventChecklistCount {
        public long eventId;
        public int itemCount;
    }
}
