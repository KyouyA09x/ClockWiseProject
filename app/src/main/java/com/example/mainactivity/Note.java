package com.example.mainactivity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public long createdTimestamp;
    public long modifiedTimestamp;
    
    // Text formatting properties
    public String fontFamily; // e.g., "default", "serif", "monospace"
    public int textSize; // in sp
    public String textColor; // hex color
    public boolean isBold;
    public boolean isItalic;
    public boolean isUnderline;
    public boolean isChecklist; // whether the description is a checklist
    public String priority; // "None", "Low", "Medium", "High"
    public boolean isConvertedToTask; // whether this note has been converted to a task
    public String taskType; // "None", "Reminder", "Focus Task"
    public String dueDate; // due date in yyyy-MM-dd format (null if not set)

    public Note() {
        this.createdTimestamp = System.currentTimeMillis();
        this.modifiedTimestamp = System.currentTimeMillis();
        this.fontFamily = "default";
        this.textSize = 16;
        this.textColor = "#000000";
        this.isBold = false;
        this.isItalic = false;
        this.isUnderline = false;
        this.isChecklist = false;
        this.priority = "None";
        this.isConvertedToTask = false;
        this.taskType = "None";
        this.dueDate = null;
    }

    public Note(String title, String description) {
        this();
        this.title = title;
        this.description = description;
    }

    protected Note(Parcel in) {
        id = in.readInt();
        title = in.readString();
        description = in.readString();
        createdTimestamp = in.readLong();
        modifiedTimestamp = in.readLong();
        fontFamily = in.readString();
        textSize = in.readInt();
        textColor = in.readString();
        isBold = in.readByte() != 0;
        isItalic = in.readByte() != 0;
        isUnderline = in.readByte() != 0;
        isChecklist = in.readByte() != 0;
        priority = in.readString();
        isConvertedToTask = in.readByte() != 0;
        taskType = in.readString();
    }

    public static final Creator<Note> CREATOR = new Creator<Note>() {
        @Override
        public Note createFromParcel(Parcel in) {
            return new Note(in);
        }

        @Override
        public Note[] newArray(int size) {
            return new Note[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeLong(createdTimestamp);
        dest.writeLong(modifiedTimestamp);
        dest.writeString(fontFamily);
        dest.writeInt(textSize);
        dest.writeString(textColor);
        dest.writeByte((byte) (isBold ? 1 : 0));
        dest.writeByte((byte) (isItalic ? 1 : 0));
        dest.writeByte((byte) (isUnderline ? 1 : 0));
        dest.writeByte((byte) (isChecklist ? 1 : 0));
        dest.writeString(priority);
        dest.writeByte((byte) (isConvertedToTask ? 1 : 0));
        dest.writeString(taskType);
        dest.writeString(dueDate);
    }
}
