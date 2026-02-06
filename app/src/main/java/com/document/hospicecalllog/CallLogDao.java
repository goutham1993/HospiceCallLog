package com.document.hospicecalllog;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.Date;
import java.util.List;

@Dao
public interface CallLogDao {
    @Query("SELECT * FROM call_log_entries ORDER BY date DESC")
    LiveData<List<CallLogEntry>> getAllEntries();
    
    @Query("SELECT * FROM call_log_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<CallLogEntry>> getEntriesByDateRange(Date startDate, Date endDate);
    
    @Query("SELECT * FROM call_log_entries WHERE id = :id")
    LiveData<CallLogEntry> getEntryById(int id);
    
    @Insert
    void insert(CallLogEntry entry);
    
    @Update
    void update(CallLogEntry entry);
    
    @Delete
    void delete(CallLogEntry entry);
    
    @Query("DELETE FROM call_log_entries WHERE id = :id")
    void deleteById(int id);
    
    @Query("SELECT action, COUNT(*) as count FROM call_log_entries WHERE date BETWEEN :startDate AND :endDate GROUP BY action ORDER BY count DESC")
    LiveData<List<ActionCount>> getActionCountsByDateRange(Date startDate, Date endDate);
}
