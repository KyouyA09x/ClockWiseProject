package com.example.mainactivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

public class TrashBinActivity extends BaseThemedActivity {
    
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private NoteDao noteDao;
    private List<Note> deletedNotes;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_bin);
        
        // Initialize toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Trash Bin");
        }
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewTrash);
        emptyTextView = findViewById(R.id.emptyTextView);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize DAO
        noteDao = TaskDatabase.getInstance(this).noteDao();
        
        // Load deleted notes
        loadDeletedNotes();
    }
    
    private void loadDeletedNotes() {
        // For now, show empty state since delete functionality isn't implemented yet
        // In the future, filter notes with isDeleted = true
        runOnUiThread(() -> {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setText("No deleted notes\n\nDeleted notes will appear here");
            emptyTextView.setVisibility(View.VISIBLE);
        });
        
        // TODO: Implement delete functionality
        // When Note class has isDeleted field, uncomment this:
        /*
        new Thread(() -> {
            List<Note> allNotes = noteDao.getAllNotes();
            deletedNotes = new ArrayList<>();
            
            for (Note note : allNotes) {
                if (note.isDeleted) {
                    deletedNotes.add(note);
                }
            }
            
            runOnUiThread(() -> {
                if (deletedNotes.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.GONE);
                    // Set adapter here
                }
            });
        }).start();
        */
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
