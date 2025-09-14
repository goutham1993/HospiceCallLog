package com.document.hospicecalllog;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "call_log_entries")
public class CallLogEntry implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String medicalRecordNumber;
    private String name;
    private String notes;
    private String action; // Action field for things like "Rx x 3", "txt x 2", etc.
    private Date date;
    private int durationMinutes; // Duration in minutes, default 6 minutes
    
    public CallLogEntry() {}
    
    @Ignore
    public CallLogEntry(String medicalRecordNumber, String name, String notes, String action, Date date, int durationMinutes) {
        this.medicalRecordNumber = medicalRecordNumber;
        this.name = name;
        this.notes = notes;
        this.action = action;
        this.date = date;
        this.durationMinutes = durationMinutes;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getMedicalRecordNumber() {
        return medicalRecordNumber;
    }
    
    public void setMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
