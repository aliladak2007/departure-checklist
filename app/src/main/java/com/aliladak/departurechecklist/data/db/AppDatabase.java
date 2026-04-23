package com.aliladak.departurechecklist.data.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.aliladak.departurechecklist.data.db.dao.ChecklistDao;
import com.aliladak.departurechecklist.data.db.dao.ChecklistItemDao;
import com.aliladak.departurechecklist.data.db.entity.Checklist;
import com.aliladak.departurechecklist.data.db.entity.ChecklistItem;

/**
 * Application Room database singleton.
 */
@Database(entities = {Checklist.class, ChecklistItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "departure_checklist.db";
    private static volatile AppDatabase instance;

    /**
     * Returns the shared database instance.
     *
     * @param context application context
     * @return database singleton
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    Log.e(TAG, "Creating database instance");
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    /** @return checklist DAO */
    public abstract ChecklistDao checklistDao();

    /** @return checklist item DAO */
    public abstract ChecklistItemDao checklistItemDao();
}
