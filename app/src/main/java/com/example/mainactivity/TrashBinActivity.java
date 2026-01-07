package com.example.mainactivity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class TrashBinActivity extends AppCompatActivity {

    private LinearLayout trashItemsContainer;
    private LinearLayout emptyState;
    private ExtendedFloatingActionButton fabEmptyTrash;
    private TabLayout categoryTabs;
    
    private TaskDao taskDao;
    private NoteDao noteDao;
    
    private List<Task> deletedTasks = new ArrayList<>();
    private List<Note> deletedNotes = new ArrayList<>();
    
    private int currentTab = 0; // 0=All, 1=Tasks, 2=Focus, 3=Notes
    
    // For long press popup
    private PopupWindow quickInfoPopup;
    private boolean isPopupTapped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_bin);
        
        initViews();
        setupToolbar();
        setupTabs();
        
        TaskDatabase db = TaskDatabase.getInstance(this);
        taskDao = db.taskDao();
        noteDao = db.noteDao();
        
        loadTrashItems();
    }

    private void initViews() {
        trashItemsContainer = findViewById(R.id.trashItemsContainer);
        emptyState = findViewById(R.id.emptyState);
        fabEmptyTrash = findViewById(R.id.fabEmptyTrash);
        categoryTabs = findViewById(R.id.categoryTabs);
        
        fabEmptyTrash.setOnClickListener(v -> showEmptyTrashConfirmation());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        categoryTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                displayTrashItems();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadTrashItems() {
        Executors.newSingleThreadExecutor().execute(() -> {
            deletedTasks = taskDao.getDeletedTasks();
            deletedNotes = noteDao.getDeletedNotes();
            
            runOnUiThread(this::displayTrashItems);
        });
    }

    private void displayTrashItems() {
        trashItemsContainer.removeAllViews();
        
        List<Object> itemsToShow = new ArrayList<>();
        
        switch (currentTab) {
            case 0: // All
                itemsToShow.addAll(deletedTasks);
                itemsToShow.addAll(deletedNotes);
                break;
            case 1: // Tasks (reminders only)
                for (Task task : deletedTasks) {
                    if (!"focus".equals(task.taskType)) {
                        itemsToShow.add(task);
                    }
                }
                break;
            case 2: // Focus Tasks
                for (Task task : deletedTasks) {
                    if ("focus".equals(task.taskType)) {
                        itemsToShow.add(task);
                    }
                }
                break;
            case 3: // Notes
                itemsToShow.addAll(deletedNotes);
                break;
        }
        
        if (itemsToShow.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            fabEmptyTrash.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            fabEmptyTrash.setVisibility(View.VISIBLE);
            
            for (Object item : itemsToShow) {
                if (item instanceof Task) {
                    addTaskTrashItem((Task) item);
                } else if (item instanceof Note) {
                    addNoteTrashItem((Note) item);
                }
            }
        }
    }

    private void addTaskTrashItem(Task task) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_trash, trashItemsContainer, false);
        
        ImageView typeIcon = itemView.findViewById(R.id.itemTypeIcon);
        TextView titleView = itemView.findViewById(R.id.itemTitle);
        TextView subtitleView = itemView.findViewById(R.id.itemSubtitle);
        TextView typeView = itemView.findViewById(R.id.itemType);
        
        titleView.setText(task.name);
        subtitleView.setText(getTimeAgo(task.deletedAt));
        
        boolean isFocus = "focus".equals(task.taskType);
        if (isFocus) {
            typeIcon.setImageResource(R.drawable.ic_focus);
            typeIcon.setBackgroundResource(R.drawable.icon_background_blue);
            typeView.setText("Focus Session");
        } else {
            typeIcon.setImageResource(R.drawable.ic_reminder);
            typeIcon.setBackgroundResource(R.drawable.icon_background_green);
            typeView.setText("Task");
        }
        
        // Long press shows quick info popup
        itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start long press detection
                    v.postDelayed(() -> {
                        if (v.isPressed()) {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            showTaskQuickInfoPopup(v, task, isFocus);
                        }
                    }, 500);
                    return false; // Allow normal click handling too
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dismissQuickInfoPopup();
                    return false;
            }
            return false;
        });
        
        // Regular click shows options
        itemView.setOnClickListener(v -> showTaskOptionsDialog(task));
        
        trashItemsContainer.addView(itemView);
    }

    private void addNoteTrashItem(Note note) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_trash, trashItemsContainer, false);
        
        ImageView typeIcon = itemView.findViewById(R.id.itemTypeIcon);
        TextView titleView = itemView.findViewById(R.id.itemTitle);
        TextView subtitleView = itemView.findViewById(R.id.itemSubtitle);
        TextView typeView = itemView.findViewById(R.id.itemType);
        
        titleView.setText(note.title);
        subtitleView.setText(getTimeAgo(note.deletedAt));
        
        typeIcon.setImageResource(R.drawable.ic_notepad);
        typeIcon.setBackgroundResource(R.drawable.icon_background_orange);
        typeView.setText("Note");
        
        // Long press shows quick info popup
        itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start long press detection
                    v.postDelayed(() -> {
                        if (v.isPressed()) {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            showNoteQuickInfoPopup(v, note);
                        }
                    }, 500);
                    return false; // Allow normal click handling too
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dismissQuickInfoPopup();
                    return false;
            }
            return false;
        });
        
        // Regular click shows options
        itemView.setOnClickListener(v -> showNoteOptionsDialog(note));
        
        trashItemsContainer.addView(itemView);
    }

    private void showRestoreTaskDialog(Task task) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore " + ("focus".equals(task.taskType) ? "Focus Session" : "Task"))
                .setMessage("Would you like to restore \"" + task.name + "\"?")
                .setPositiveButton("Restore", (dialog, which) -> restoreTask(task))
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete Forever", (dialog, which) -> permanentlyDeleteTask(task))
                .show();
    }

    private void showRestoreNoteDialog(Note note) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Note")
                .setMessage("Would you like to restore \"" + note.title + "\"?")
                .setPositiveButton("Restore", (dialog, which) -> restoreNote(note))
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete Forever", (dialog, which) -> permanentlyDeleteNote(note))
                .show();
    }

    private void showTaskOptionsDialog(Task task) {
        String[] options = {"Restore", "Delete Forever"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(task.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        restoreTask(task);
                    } else {
                        permanentlyDeleteTask(task);
                    }
                })
                .show();
    }

    private void showNoteOptionsDialog(Note note) {
        String[] options = {"Restore", "Delete Forever"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(note.title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        restoreNote(note);
                    } else {
                        permanentlyDeleteNote(note);
                    }
                })
                .show();
    }

    private void restoreTask(Task task) {
        Executors.newSingleThreadExecutor().execute(() -> {
            taskDao.restore(task.id);
            runOnUiThread(() -> {
                Toast.makeText(this, "\"" + task.name + "\" restored", Toast.LENGTH_SHORT).show();
                loadTrashItems();
                sendRefreshBroadcast();
            });
        });
    }

    private void restoreNote(Note note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            noteDao.restore(note.id);
            runOnUiThread(() -> {
                Toast.makeText(this, "\"" + note.title + "\" restored", Toast.LENGTH_SHORT).show();
                loadTrashItems();
                sendRefreshBroadcast();
            });
        });
    }

    private void permanentlyDeleteTask(Task task) {
        Executors.newSingleThreadExecutor().execute(() -> {
            taskDao.deleteById(task.id);
            runOnUiThread(() -> {
                Toast.makeText(this, "Permanently deleted", Toast.LENGTH_SHORT).show();
                loadTrashItems();
            });
        });
    }

    private void permanentlyDeleteNote(Note note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            noteDao.deleteById(note.id);
            runOnUiThread(() -> {
                Toast.makeText(this, "Permanently deleted", Toast.LENGTH_SHORT).show();
                loadTrashItems();
            });
        });
    }

    private void showEmptyTrashConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Empty Trash")
                .setMessage("Are you sure you want to permanently delete all items in the trash? This action cannot be undone.")
                .setPositiveButton("Empty Trash", (dialog, which) -> emptyTrash())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void emptyTrash() {
        Executors.newSingleThreadExecutor().execute(() -> {
            taskDao.emptyTrash();
            noteDao.permanentlyDeleteAllTrashed();
            runOnUiThread(() -> {
                Toast.makeText(this, "Trash emptied", Toast.LENGTH_SHORT).show();
                loadTrashItems();
            });
        });
    }

    private void sendRefreshBroadcast() {
        Intent refreshIntent = new Intent("com.example.mainactivity.REFRESH_TASKS");
        refreshIntent.setPackage(getPackageName());
        sendBroadcast(refreshIntent);
    }
    
    private void showTaskQuickInfoPopup(View anchorView, Task task, boolean isFocus) {
        dismissQuickInfoPopup();
        isPopupTapped = false;
        
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_trash_quick_info, null);
        
        // Set up the popup content
        ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        MaterialCardView iconContainer = popupView.findViewById(R.id.iconContainer);
        TextView itemTitle = popupView.findViewById(R.id.itemTitle);
        TextView itemType = popupView.findViewById(R.id.itemType);
        TextView deletedTime = popupView.findViewById(R.id.deletedTime);
        
        itemTitle.setText(task.name);
        deletedTime.setText(getTimeAgo(task.deletedAt));
        
        if (isFocus) {
            typeIcon.setImageResource(R.drawable.ic_focus);
            itemType.setText("Focus Session");
            iconContainer.setCardBackgroundColor(getColor(R.color.blue_primary));
        } else {
            typeIcon.setImageResource(R.drawable.ic_reminder);
            itemType.setText("Task");
            iconContainer.setCardBackgroundColor(getColor(R.color.green_primary));
        }
        
        quickInfoPopup = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false);
        quickInfoPopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        quickInfoPopup.setElevation(16);
        
        // Tap on popup to restore
        popupView.setOnClickListener(v -> {
            isPopupTapped = true;
            dismissQuickInfoPopup();
            restoreTask(task);
        });
        
        // Show popup centered on screen
        quickInfoPopup.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }
    
    private void showNoteQuickInfoPopup(View anchorView, Note note) {
        dismissQuickInfoPopup();
        isPopupTapped = false;
        
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_trash_quick_info, null);
        
        // Set up the popup content
        ImageView typeIcon = popupView.findViewById(R.id.typeIcon);
        MaterialCardView iconContainer = popupView.findViewById(R.id.iconContainer);
        TextView itemTitle = popupView.findViewById(R.id.itemTitle);
        TextView itemType = popupView.findViewById(R.id.itemType);
        TextView deletedTime = popupView.findViewById(R.id.deletedTime);
        
        itemTitle.setText(note.title);
        itemType.setText("Note");
        deletedTime.setText(getTimeAgo(note.deletedAt));
        
        typeIcon.setImageResource(R.drawable.ic_notepad);
        iconContainer.setCardBackgroundColor(getColor(R.color.orange_primary));
        
        quickInfoPopup = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false);
        quickInfoPopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        quickInfoPopup.setElevation(16);
        
        // Tap on popup to restore
        popupView.setOnClickListener(v -> {
            isPopupTapped = true;
            dismissQuickInfoPopup();
            restoreNote(note);
        });
        
        // Show popup centered on screen
        quickInfoPopup.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }
    
    private void dismissQuickInfoPopup() {
        if (quickInfoPopup != null && quickInfoPopup.isShowing()) {
            quickInfoPopup.dismiss();
            quickInfoPopup = null;
        }
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp == 0) return "Unknown";
        
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            if (days == 1) return "Deleted yesterday";
            if (days < 7) return "Deleted " + days + " days ago";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return "Deleted on " + sdf.format(new Date(timestamp));
        } else if (hours > 0) {
            return "Deleted " + hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (minutes > 0) {
            return "Deleted " + minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else {
            return "Deleted just now";
        }
    }
}