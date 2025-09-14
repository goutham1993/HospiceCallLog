package com.document.hospicecalllog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder> {
    private List<CallLogEntry> callLogEntries;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    public interface OnItemClickListener {
        void onItemClick(CallLogEntry entry);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(CallLogEntry entry);
    }
    
    public CallLogAdapter(List<CallLogEntry> callLogEntries) {
        this.callLogEntries = callLogEntries;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    
    @NonNull
    @Override
    public CallLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_log_card, parent, false);
        return new CallLogViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CallLogViewHolder holder, int position) {
        CallLogEntry entry = callLogEntries.get(position);
        holder.bind(entry);
    }
    
    @Override
    public int getItemCount() {
        return callLogEntries.size();
    }
    
    public void updateEntries(List<CallLogEntry> newEntries) {
        this.callLogEntries = newEntries;
        notifyDataSetChanged();
    }
    
    class CallLogViewHolder extends RecyclerView.ViewHolder {
        private TextView medicalRecordNumber;
        private TextView name;
        private TextView notes;
        private TextView action;
        private TextView duration;
        private View actionLayout;
        private View nameLayout;
        private View notesLayout;
        
        public CallLogViewHolder(@NonNull View itemView) {
            super(itemView);
            medicalRecordNumber = itemView.findViewById(R.id.medicalRecordNumber);
            name = itemView.findViewById(R.id.name);
            notes = itemView.findViewById(R.id.notes);
            action = itemView.findViewById(R.id.action);
            duration = itemView.findViewById(R.id.duration);
            actionLayout = itemView.findViewById(R.id.actionLayout);
            nameLayout = itemView.findViewById(R.id.nameLayout);
            notesLayout = itemView.findViewById(R.id.notesLayout);
            
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(callLogEntries.get(position));
                    }
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemLongClickListener.onItemLongClick(callLogEntries.get(position));
                        return true;
                    }
                }
                return false;
            });
        }
        
        public void bind(CallLogEntry entry) {
            // Handle medical record number (now optional)
            if (entry.getMedicalRecordNumber() != null && !entry.getMedicalRecordNumber().trim().isEmpty()) {
                medicalRecordNumber.setText("MRN: " + entry.getMedicalRecordNumber());
                medicalRecordNumber.setVisibility(View.VISIBLE);
            } else {
                medicalRecordNumber.setText("No MRN");
                medicalRecordNumber.setVisibility(View.VISIBLE);
            }
            
            duration.setText(entry.getDurationMinutes() + " minutes");
            
            if (entry.getName() != null && !entry.getName().trim().isEmpty()) {
                name.setText(entry.getName());
                nameLayout.setVisibility(View.VISIBLE);
            } else {
                nameLayout.setVisibility(View.GONE);
            }
            
            // Handle action field
            if (entry.getAction() != null && !entry.getAction().trim().isEmpty()) {
                action.setText(entry.getAction());
                actionLayout.setVisibility(View.VISIBLE);
            } else {
                actionLayout.setVisibility(View.GONE);
            }
            
            if (entry.getNotes() != null && !entry.getNotes().trim().isEmpty()) {
                notes.setText(entry.getNotes());
                notesLayout.setVisibility(View.VISIBLE);
            } else {
                notesLayout.setVisibility(View.GONE);
            }
        }
    }
}
