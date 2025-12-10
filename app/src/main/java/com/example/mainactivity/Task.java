package com.example.mainactivity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "tasks")
@TypeConverters(Converters.class)
public class Task implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    String name;
    int hour;
    int minute;
    String amPm;
    String urgency;
    boolean[] selectedDays;
    boolean isComplete;
    boolean isAlarmOn;
    String date; // Added date field
    String timeCategory; // "morning", "afternoon", or "night"
    boolean vibrationEnabled; // Added vibration field

    // Focus Task fields
    String taskType; // "reminder" or "focus"
    int endHour;
    int endMinute;
    String endAmPm;
    boolean isFocusTaskActive; // Tracks if focus session is ongoing
    int extendedMinutes; // Total minutes extended via "Keep Going"

    // Required empty constructor for Room
    public Task() {
        this.taskType = "reminder";
        this.endAmPm = "AM";
    }

    public Task(String name, int hour, int minute, String amPm, String urgency, boolean[] selectedDays) {
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.amPm = amPm;
        this.urgency = urgency;
        this.selectedDays = selectedDays;
        this.isComplete = false;
        this.isAlarmOn = true;
        this.vibrationEnabled = false;
        this.taskType = "reminder";
        this.endHour = 0;
        this.endMinute = 0;
        this.endAmPm = "AM";
        this.isFocusTaskActive = false;
        this.extendedMinutes = 0;
    }

    // Constructor for Focus Task
    public Task(String name, int startHour, int startMinute, String startAmPm,
                int endHour, int endMinute, String endAmPm, String urgency) {
        this.name = name;
        this.hour = startHour;
        this.minute = startMinute;
        this.amPm = startAmPm;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.endAmPm = endAmPm;
        this.urgency = urgency;
        this.selectedDays = new boolean[7];
        this.isComplete = false;
        this.isAlarmOn = true;
        this.vibrationEnabled = false;
        this.taskType = "focus";
        this.isFocusTaskActive = false;
        this.extendedMinutes = 0;
    }

    protected Task(Parcel in) {
        id = in.readInt();
        name = in.readString();
        hour = in.readInt();
        minute = in.readInt();
        amPm = in.readString();
        urgency = in.readString();
        selectedDays = in.createBooleanArray();
        isComplete = in.readByte() != 0;
        isAlarmOn = in.readByte() != 0;
        date = in.readString();
        timeCategory = in.readString();
        vibrationEnabled = in.readByte() != 0;
        taskType = in.readString();
        endHour = in.readInt();
        endMinute = in.readInt();
        endAmPm = in.readString();
        isFocusTaskActive = in.readByte() != 0;
        extendedMinutes = in.readInt();
    }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) {
            return new Task(in);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(hour);
        dest.writeInt(minute);
        dest.writeString(amPm);
        dest.writeString(urgency);
        dest.writeBooleanArray(selectedDays);
        dest.writeByte((byte) (isComplete ? 1 : 0));
        dest.writeByte((byte) (isAlarmOn ? 1 : 0));
        dest.writeString(date); // Write date
        dest.writeString(timeCategory);
        dest.writeByte((byte) (vibrationEnabled ? 1 : 0)); // Write vibration
        dest.writeString(taskType);
        dest.writeInt(endHour);
        dest.writeInt(endMinute);
        dest.writeString(endAmPm);
        dest.writeByte((byte) (isFocusTaskActive ? 1 : 0));
        dest.writeInt(extendedMinutes);
    }

    // Helper method to get formatted start time
    public String getStartTimeFormatted() {
        return String.format("%d:%02d %s", hour, minute, amPm != null ? amPm : "AM");
    }

    // Helper method to get formatted end time
    public String getEndTimeFormatted() {
        return String.format("%d:%02d %s", endHour, endMinute, endAmPm != null ? endAmPm : "AM");
    }

    // Helper method to check if this is a focus task
    public boolean isFocusTask() {
        return "focus".equals(taskType);
    }
}
