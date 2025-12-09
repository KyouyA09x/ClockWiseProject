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

    public Task(String name, int hour, int minute, String amPm, String urgency, boolean[] selectedDays) {
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.amPm = amPm;
        this.urgency = urgency;
        this.selectedDays = selectedDays;
        this.isComplete = false;
        this.isAlarmOn = true;
        this.vibrationEnabled = false; // Default to false
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
        date = in.readString(); // Read date
        timeCategory = in.readString();
        vibrationEnabled = in.readByte() != 0; // Read vibration
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
    }
}
