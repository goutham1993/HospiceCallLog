package com.document.hospicecalllog;

import androidx.room.ColumnInfo;

public class ActionCount {
    @ColumnInfo(name = "action")
    private String action;
    
    @ColumnInfo(name = "count")
    private int count;
    
    public ActionCount() {}
    
    public ActionCount(String action, int count) {
        this.action = action;
        this.count = count;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
}
