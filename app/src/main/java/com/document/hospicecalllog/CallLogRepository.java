package com.document.hospicecalllog;

import androidx.lifecycle.LiveData;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallLogRepository {
    private CallLogDao callLogDao;
    private ExecutorService executor;
    
    public CallLogRepository(CallLogDao callLogDao) {
        this.callLogDao = callLogDao;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public LiveData<List<CallLogEntry>> getAllEntries() {
        return callLogDao.getAllEntries();
    }
    
    public LiveData<List<CallLogEntry>> getEntriesByDateRange(Date startDate, Date endDate) {
        return callLogDao.getEntriesByDateRange(startDate, endDate);
    }
    
    public LiveData<CallLogEntry> getEntryById(int id) {
        return callLogDao.getEntryById(id);
    }
    
    public void insert(CallLogEntry entry) {
        executor.execute(() -> callLogDao.insert(entry));
    }
    
    public void update(CallLogEntry entry) {
        executor.execute(() -> callLogDao.update(entry));
    }
    
    public void delete(CallLogEntry entry) {
        executor.execute(() -> callLogDao.delete(entry));
    }
    
    public void deleteById(int id) {
        executor.execute(() -> callLogDao.deleteById(id));
    }
    
    public LiveData<List<ActionCount>> getActionCountsByDateRange(Date startDate, Date endDate) {
        return callLogDao.getActionCountsByDateRange(startDate, endDate);
    }
}
