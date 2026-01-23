package com.example.mainactivity;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Task.class, Note.class}, version = 13, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class TaskDatabase extends RoomDatabase {
    private static volatile TaskDatabase INSTANCE;

    public abstract TaskDao taskDao();
    public abstract NoteDao noteDao();

    public static TaskDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TaskDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    TaskDatabase.class, "task_database")
                            .allowMainThreadQueries() // For simplicity; consider using async operations in production
                            .fallbackToDestructiveMigration() // Handle schema changes
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

