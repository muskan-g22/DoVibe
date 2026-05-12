package com.project.dovibe;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String  title;        // e.g. "John's Birthday"
    public String  type;         // "Birthday" | "Anniversary" | "Meeting" | "Holiday" | "Other"
    public String  date;         // display e.g. "Dec 25"
    public int     month;        // 1–12
    public int     day;          // 1–31
    public int     hour;
    public int     minute;
    public String  repeatMode;   // "yearly" | "once"
    public boolean enabled;
    public long    createdAt;
}