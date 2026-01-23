package com.example.mainactivity;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY modifiedTimestamp DESC")
    List<Note> getAllNotes();
    
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY modifiedTimestamp DESC")
    List<Note> getActiveNotes();
    
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY modifiedTimestamp DESC")
    List<Note> getDeletedNotes();

    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(int noteId);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteById(int noteId);
    
    // Soft delete - move to trash
    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    void softDelete(int id, long deletedAt);
    
    // Restore from trash
    @Query("UPDATE notes SET isDeleted = 0, deletedAt = 0 WHERE id = :id")
    void restore(int id);
    
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    void permanentlyDeleteAllTrashed();
    
    // Get notes linked to a specific task
    @Query("SELECT * FROM notes WHERE linkedTaskId = :taskId AND isDeleted = 0 ORDER BY createdTimestamp ASC")
    List<Note> getNotesForTask(int taskId);
}
