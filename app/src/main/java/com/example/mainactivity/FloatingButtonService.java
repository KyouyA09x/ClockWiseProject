package com.example.mainactivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class FloatingButtonService extends Service {

    private static final String CHANNEL_ID = "FloatingButtonChannel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View floatingView;
    private View menuView;
    private boolean isMenuVisible = false;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    private TaskDatabase taskDatabase;
    private TaskRepository taskRepository;

    private Calendar selectedTaskTime;
    private Calendar focusStartTime;
    private Calendar focusEndTime;

    private Context themedContext;
    private View timePickerView;

    // Interface for time selection callback
    private interface TimeSelectedListener {
        void onTimeSelected(int hourOfDay, int minute);
    }

    /**
     * Get a themed context for inflating Material3 layouts from a Service.
     * Service context doesn't have Material theme by default, so we need to wrap it.
     */
    private Context getThemedContext() {
        if (themedContext == null) {
            themedContext = new ContextThemeWrapper(this, R.style.Theme_MainActivity);
        }
        return themedContext;
    }

    /**
     * Show a custom time picker as an overlay window (since TimePickerDialog requires Activity context)
     */
    private void showTimePicker(String title, int initialHour, int initialMinute, TimeSelectedListener listener) {
        if (timePickerView != null) {
            try { windowManager.removeView(timePickerView); } catch (Exception ignored) {}
            timePickerView = null;
        }

        timePickerView = LayoutInflater.from(getThemedContext()).inflate(R.layout.floating_time_picker, null);

        int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);

        windowManager.addView(timePickerView, params);

        TextView titleView = timePickerView.findViewById(R.id.timePickerTitle);
        NumberPicker hourPicker = timePickerView.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = timePickerView.findViewById(R.id.minutePicker);
        NumberPicker amPmPicker = timePickerView.findViewById(R.id.amPmPicker);
        MaterialButton cancelButton = timePickerView.findViewById(R.id.cancelButton);
        MaterialButton okButton = timePickerView.findViewById(R.id.okButton);

        if (titleView != null) titleView.setText(title);

        // Setup hour picker (1-12)
        if (hourPicker != null) {
            hourPicker.setMinValue(1);
            hourPicker.setMaxValue(12);
            int displayHour = initialHour % 12;
            if (displayHour == 0) displayHour = 12;
            hourPicker.setValue(displayHour);
        }

        // Setup minute picker (0-59)
        if (minutePicker != null) {
            minutePicker.setMinValue(0);
            minutePicker.setMaxValue(59);
            minutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));
            minutePicker.setValue(initialMinute);
        }

        // Setup AM/PM picker
        if (amPmPicker != null) {
            amPmPicker.setMinValue(0);
            amPmPicker.setMaxValue(1);
            amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
            amPmPicker.setValue(initialHour >= 12 ? 1 : 0);
        }

        // Cancel button
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                if (timePickerView != null) {
                    try { windowManager.removeView(timePickerView); } catch (Exception ignored) {}
                    timePickerView = null;
                }
            });
        }

        // OK button
        if (okButton != null) {
            okButton.setOnClickListener(v -> {
                int hour = hourPicker != null ? hourPicker.getValue() : 12;
                int minute = minutePicker != null ? minutePicker.getValue() : 0;
                boolean isPm = amPmPicker != null && amPmPicker.getValue() == 1;

                // Convert to 24-hour format
                int hourOfDay;
                if (hour == 12) {
                    hourOfDay = isPm ? 12 : 0;
                } else {
                    hourOfDay = isPm ? hour + 12 : hour;
                }

                if (timePickerView != null) {
                    try { windowManager.removeView(timePickerView); } catch (Exception ignored) {}
                    timePickerView = null;
                }

                if (listener != null) {
                    listener.onTimeSelected(hourOfDay, minute);
                }
            });
        }

        // Click outside to dismiss
        timePickerView.setOnClickListener(v -> {
            if (timePickerView != null) {
                try { windowManager.removeView(timePickerView); } catch (Exception ignored) {}
                timePickerView = null;
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        taskDatabase = TaskDatabase.getInstance(this);
        taskRepository = TaskRepository.getInstance();
        taskRepository.initialize(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            if (floatingView == null) {
                createFloatingButton();
            }
            return START_STICKY;
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);
        View floatingButton = floatingView.findViewById(R.id.floatingActionButton);

        int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingView, params);

        floatingButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - initialTouchX);
                    int dy = (int) (event.getRawY() - initialTouchY);
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true;
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(floatingView, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    Point size = new Point();
                    windowManager.getDefaultDisplay().getSize(size);
                    params.x = (params.x < size.x / 2) ? 0 : size.x - floatingView.getWidth();
                    windowManager.updateViewLayout(floatingView, params);
                    if (!isDragging) {
                        showMainMenu();
                    }
                    return true;
            }
            return false;
        });
    }

    private void showMainMenu() {
        // Clean up any existing menu first (for back navigation)
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
            isMenuVisible = false;
        }
        try {
            menuView = LayoutInflater.from(getThemedContext()).inflate(R.layout.floating_menu_layout, null);
            int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            windowManager.addView(menuView, menuParams);
            isMenuVisible = true;
            View quickTaskCard = menuView.findViewById(R.id.quickTaskCard);
            View noteCard = menuView.findViewById(R.id.noteCard);
            if (quickTaskCard != null) quickTaskCard.setOnClickListener(v -> showTaskTypeMenu());
            if (noteCard != null) noteCard.setOnClickListener(v -> showNoteForm());
            menuView.setOnClickListener(v -> hideMenu());
        } catch (Exception e) {
            menuView = null;
            isMenuVisible = false;
        }
    }

    private void showTaskTypeMenu() {
        // Clean up any existing menu first
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
        }
        try {
            menuView = LayoutInflater.from(getThemedContext()).inflate(R.layout.floating_task_type_menu, null);
            int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            windowManager.addView(menuView, menuParams);
            isMenuVisible = true;
            
            // Back button - go back to main menu
            View backButton = menuView.findViewById(R.id.backButton);
            if (backButton != null) backButton.setOnClickListener(v -> showMainMenu());
            
            View taskTypeCard = menuView.findViewById(R.id.taskTypeCard);
            View focusTypeCard = menuView.findViewById(R.id.focusTypeCard);
            if (taskTypeCard != null) taskTypeCard.setOnClickListener(v -> showQuickTaskForm());
            if (focusTypeCard != null) focusTypeCard.setOnClickListener(v -> showFocusTaskForm());
            // Click outside goes back to main menu
            menuView.setOnClickListener(v -> showMainMenu());
        } catch (Exception e) {
            menuView = null;
            isMenuVisible = false;
        }
    }

    private void showQuickTaskForm() {
        // Clean up any existing menu first
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
        }
        try {
            menuView = LayoutInflater.from(getThemedContext()).inflate(R.layout.floating_quick_task_form, null);
            int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
            menuParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            windowManager.addView(menuView, menuParams);
            isMenuVisible = true;
            
            // Back button - go back to task type menu
            View backButton = menuView.findViewById(R.id.backButton);
            if (backButton != null) backButton.setOnClickListener(v -> showTaskTypeMenu());
            
            selectedTaskTime = Calendar.getInstance();
            selectedTaskTime.add(Calendar.MINUTE, 5);
            
            EditText taskNameInput = menuView.findViewById(R.id.taskNameInput);
            RadioGroup priorityGroup = menuView.findViewById(R.id.priorityRadioGroup);
            TextView timePreview = menuView.findViewById(R.id.timePreview);
            View timePickerLayout = menuView.findViewById(R.id.timePickerLayout);
            SwitchMaterial vibrationSwitch = menuView.findViewById(R.id.vibrationSwitch);
            SwitchMaterial alarmSwitch = menuView.findViewById(R.id.alarmSwitch);
            MaterialButton cancelButton = menuView.findViewById(R.id.cancelButton);
            MaterialButton saveButton = menuView.findViewById(R.id.saveButton);
            
            SimpleDateFormat tf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            if (timePreview != null) timePreview.setText(tf.format(selectedTaskTime.getTime()));
            
            if (timePickerLayout != null) {
                timePickerLayout.setOnClickListener(v -> showTimePicker("Select Time",
                    selectedTaskTime.get(Calendar.HOUR_OF_DAY),
                    selectedTaskTime.get(Calendar.MINUTE),
                    (h, m) -> {
                        Calendar sel = Calendar.getInstance();
                        sel.set(Calendar.HOUR_OF_DAY, h);
                        sel.set(Calendar.MINUTE, m);
                        if (sel.before(Calendar.getInstance())) {
                            Toast.makeText(this, "Select a future time", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedTaskTime.set(Calendar.HOUR_OF_DAY, h);
                        selectedTaskTime.set(Calendar.MINUTE, m);
                        if (timePreview != null) timePreview.setText(tf.format(selectedTaskTime.getTime()));
                    }));
            }
            
            if (priorityGroup != null) {
                priorityGroup.setOnCheckedChangeListener((g, id) -> {
                    boolean isHigh = (id == R.id.priorityHigh);
                    if (vibrationSwitch != null) { vibrationSwitch.setChecked(isHigh); vibrationSwitch.setEnabled(!isHigh); }
                    if (alarmSwitch != null) { alarmSwitch.setChecked(isHigh); alarmSwitch.setEnabled(!isHigh); }
                });
            }
            
            if (cancelButton != null) cancelButton.setOnClickListener(v -> hideMenu());
            if (saveButton != null) {
                saveButton.setOnClickListener(v -> {
                    String name = taskNameInput != null && taskNameInput.getText() != null ? taskNameInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(name)) { Toast.makeText(this, "Enter a task name", Toast.LENGTH_SHORT).show(); return; }
                    if (selectedTaskTime.before(Calendar.getInstance())) { Toast.makeText(this, "Select a future time", Toast.LENGTH_SHORT).show(); return; }
                    int priority = 1;
                    if (priorityGroup != null) {
                        int checkedId = priorityGroup.getCheckedRadioButtonId();
                        if (checkedId == R.id.priorityLow) priority = 0;
                        else if (checkedId == R.id.priorityHigh) priority = 2;
                    }
                    boolean vib = vibrationSwitch != null && vibrationSwitch.isChecked();
                    boolean alarm = alarmSwitch != null && alarmSwitch.isChecked();
                    saveTask(name, priority, selectedTaskTime, vib, alarm);
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            menuView = null;
            isMenuVisible = false;
        }
    }

    private void showFocusTaskForm() {
        try {
            menuView = LayoutInflater.from(getThemedContext()).inflate(R.layout.floating_focus_task_form, null);
            int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
            menuParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            windowManager.addView(menuView, menuParams);
            isMenuVisible = true;
            
            focusStartTime = Calendar.getInstance();
            focusStartTime.add(Calendar.MINUTE, 5);
            focusEndTime = Calendar.getInstance();
            focusEndTime.add(Calendar.HOUR_OF_DAY, 1);
            focusEndTime.add(Calendar.MINUTE, 5);
            
            // Back button - go back to task type menu
            View backButton = menuView.findViewById(R.id.backButton);
            if (backButton != null) backButton.setOnClickListener(v -> showTaskTypeMenu());
            
            EditText focusNameInput = menuView.findViewById(R.id.focusTaskNameInput);
            RadioGroup priorityGroup = menuView.findViewById(R.id.priorityRadioGroup);
            TextView startTimePreview = menuView.findViewById(R.id.startTimePreview);
            TextView endTimePreview = menuView.findViewById(R.id.endTimePreview);
            View startTimeLayout = menuView.findViewById(R.id.startTimePickerLayout);
            View endTimeLayout = menuView.findViewById(R.id.endTimePickerLayout);
            MaterialButton cancelButton = menuView.findViewById(R.id.cancelButton);
            MaterialButton saveButton = menuView.findViewById(R.id.saveButton);
            
            SimpleDateFormat tf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            if (startTimePreview != null) startTimePreview.setText(tf.format(focusStartTime.getTime()));
            if (endTimePreview != null) endTimePreview.setText(tf.format(focusEndTime.getTime()));
            
            if (startTimeLayout != null) {
                startTimeLayout.setOnClickListener(v -> showTimePicker("Select Start Time",
                    focusStartTime.get(Calendar.HOUR_OF_DAY),
                    focusStartTime.get(Calendar.MINUTE),
                    (h, m) -> {
                        Calendar sel = Calendar.getInstance();
                        sel.set(Calendar.HOUR_OF_DAY, h);
                        sel.set(Calendar.MINUTE, m);
                        if (sel.before(Calendar.getInstance())) { Toast.makeText(this, "Select a future time", Toast.LENGTH_SHORT).show(); return; }
                        focusStartTime.set(Calendar.HOUR_OF_DAY, h);
                        focusStartTime.set(Calendar.MINUTE, m);
                        if (startTimePreview != null) startTimePreview.setText(tf.format(focusStartTime.getTime()));
                        if (!focusEndTime.after(focusStartTime)) {
                            focusEndTime.setTimeInMillis(focusStartTime.getTimeInMillis());
                            focusEndTime.add(Calendar.HOUR_OF_DAY, 1);
                            if (endTimePreview != null) endTimePreview.setText(tf.format(focusEndTime.getTime()));
                        }
                    }));
            }
            
            if (endTimeLayout != null) {
                endTimeLayout.setOnClickListener(v -> showTimePicker("Select End Time",
                    focusEndTime.get(Calendar.HOUR_OF_DAY),
                    focusEndTime.get(Calendar.MINUTE),
                    (h, m) -> {
                        Calendar sel = Calendar.getInstance();
                        sel.set(Calendar.HOUR_OF_DAY, h);
                        sel.set(Calendar.MINUTE, m);
                        if (!sel.after(focusStartTime)) { Toast.makeText(this, "End must be after start", Toast.LENGTH_SHORT).show(); return; }
                        focusEndTime.set(Calendar.HOUR_OF_DAY, h);
                        focusEndTime.set(Calendar.MINUTE, m);
                        if (endTimePreview != null) endTimePreview.setText(tf.format(focusEndTime.getTime()));
                    }));
            }
            
            if (cancelButton != null) cancelButton.setOnClickListener(v -> hideMenu());
            if (saveButton != null) {
                saveButton.setOnClickListener(v -> {
                    String name = focusNameInput != null && focusNameInput.getText() != null ? focusNameInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(name)) { Toast.makeText(this, "Enter a focus task name", Toast.LENGTH_SHORT).show(); return; }
                    if (focusStartTime.before(Calendar.getInstance())) { Toast.makeText(this, "Start time must be in future", Toast.LENGTH_SHORT).show(); return; }
                    if (!focusEndTime.after(focusStartTime)) { Toast.makeText(this, "End must be after start", Toast.LENGTH_SHORT).show(); return; }
                    int priority = 1;
                    if (priorityGroup != null) {
                        int checkedId = priorityGroup.getCheckedRadioButtonId();
                        if (checkedId == R.id.priorityLow) priority = 0;
                        else if (checkedId == R.id.priorityHigh) priority = 2;
                    }
                    saveFocusTask(name, priority, focusStartTime, focusEndTime);
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            menuView = null;
            isMenuVisible = false;
        }
    }

    private void showNoteForm() {
        // Clean up any existing menu first
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
        }
        try {
            menuView = LayoutInflater.from(getThemedContext()).inflate(R.layout.floating_quick_note_form, null);
            int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
            menuParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            windowManager.addView(menuView, menuParams);
            isMenuVisible = true;
            
            // Back button - go back to main menu
            View backButton = menuView.findViewById(R.id.backButton);
            if (backButton != null) backButton.setOnClickListener(v -> showMainMenu());
            
            EditText titleInput = menuView.findViewById(R.id.noteTitleInput);
            EditText contentInput = menuView.findViewById(R.id.noteContentInput);
            MaterialButton cancelButton = menuView.findViewById(R.id.cancelButton);
            MaterialButton saveButton = menuView.findViewById(R.id.saveButton);
            
            if (cancelButton != null) cancelButton.setOnClickListener(v -> showMainMenu());
            if (saveButton != null) {
                saveButton.setOnClickListener(v -> {
                    String title = titleInput != null && titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
                    String content = contentInput != null && contentInput.getText() != null ? contentInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) { Toast.makeText(this, "Enter a note title", Toast.LENGTH_SHORT).show(); return; }
                    if (TextUtils.isEmpty(content)) { Toast.makeText(this, "Enter note content", Toast.LENGTH_SHORT).show(); return; }
                    saveNote(title, content);
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            menuView = null;
            isMenuVisible = false;
        }
    }

    private void saveTask(String name, int priority, Calendar time, boolean vibration, boolean alarm) {
        String urgency = priority == 0 ? "Low" : (priority == 2 ? "High" : "Medium");
        int hour = time.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        int minute = time.get(Calendar.MINUTE);
        String amPm = time.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = df.format(time.getTime());
        
        // Determine time category based on the time
        String timeCategory;
        if (amPm.equals("AM")) {
            timeCategory = "morning";
        } else if (hour == 12 || (hour >= 1 && hour < 6)) {
            timeCategory = "afternoon";
        } else {
            timeCategory = "night";
        }
        
        Task task = new Task(name, hour, minute, amPm, urgency, new boolean[7]);
        task.date = date;
        task.timeCategory = timeCategory;
        task.isAlarmOn = alarm;
        task.vibrationEnabled = vibration;
        task.taskType = "reminder";
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                long id = taskRepository.addTask(task);
                task.id = (int) id;
                if (alarm) AlarmHelper.scheduleTaskAlarm(this, task);
                runOnUiThread(() -> { Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show(); hideMenu(); });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error saving task", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveFocusTask(String name, int priority, Calendar start, Calendar end) {
        String urgency = priority == 0 ? "Low" : (priority == 2 ? "High" : "Medium");
        int sh = start.get(Calendar.HOUR);
        if (sh == 0) sh = 12;
        int sm = start.get(Calendar.MINUTE);
        String sa = start.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        int eh = end.get(Calendar.HOUR);
        if (eh == 0) eh = 12;
        int em = end.get(Calendar.MINUTE);
        String ea = end.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = df.format(start.getTime());
        
        // Determine time category based on start time
        String timeCategory;
        if (sa.equals("AM")) {
            timeCategory = "morning";
        } else if (sh == 12 || (sh >= 1 && sh < 6)) {
            timeCategory = "afternoon";
        } else {
            timeCategory = "night";
        }
        
        Task task = new Task(name, sh, sm, sa, eh, em, ea, urgency);
        task.date = date;
        task.timeCategory = timeCategory;
        task.taskType = "focus";
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                taskRepository.addTask(task);
                runOnUiThread(() -> {
                    hideMenu();
                    Toast.makeText(this, "Focus task added!", Toast.LENGTH_SHORT).show();
                    // Send broadcast to refresh the main app
                    Intent refreshIntent = new Intent("com.example.mainactivity.REFRESH_TASKS");
                    sendBroadcast(refreshIntent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error saving focus task", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveNote(String title, String content) {
        Note note = new Note(title, content);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                taskDatabase.noteDao().insert(note);
                runOnUiThread(() -> {
                    hideMenu();
                    Toast.makeText(this, "Note added!", Toast.LENGTH_SHORT).show();
                    // Send broadcast to refresh the main app
                    Intent refreshIntent = new Intent("com.example.mainactivity.REFRESH_TASKS");
                    refreshIntent.setPackage(getPackageName());
                    sendBroadcast(refreshIntent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error saving note", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void runOnUiThread(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }

    private void hideMenu() {
        // Clean up time picker if visible
        if (timePickerView != null && windowManager != null) {
            try { windowManager.removeView(timePickerView); } catch (Exception ignored) {}
            timePickerView = null;
        }
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
            isMenuVisible = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Floating Button", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ClockWise")
                .setContentText("Floating button active")
                .setSmallIcon(R.drawable.ic_flash)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        }
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
