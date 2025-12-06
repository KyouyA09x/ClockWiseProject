package com.example.mainactivity;

import android.os.Parcel;
import android.os.Parcelable;

public class Task implements Parcelable {
    String name;
    int hour;
    int minute;
    String amPm;
    String urgency;
    boolean[] selectedDays;
    boolean isComplete;
    boolean isAlarmOn;
    String date; // Added date field

    public Task(String name, int hour, int minute, String amPm, String urgency, boolean[] selectedDays) {
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.amPm = amPm;
        this.urgency = urgency;
        this.selectedDays = selectedDays;
        this.isComplete = false;
        this.isAlarmOn = true;
    }

    protected Task(Parcel in) {
        name = in.readString();
        hour = in.readInt();
        minute = in.readInt();
        amPm = in.readString();
        urgency = in.readString();
        selectedDays = in.createBooleanArray();
        isComplete = in.readByte() != 0;
        isAlarmOn = in.readByte() != 0;
        date = in.readString(); // Read date
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
        dest.writeString(name);
        dest.writeInt(hour);
        dest.writeInt(minute);
        dest.writeString(amPm);
        dest.writeString(urgency);
        dest.writeBooleanArray(selectedDays);
        dest.writeByte((byte) (isComplete ? 1 : 0));
        dest.writeByte((byte) (isAlarmOn ? 1 : 0));
        dest.writeString(date); // Write date
    }
}
