package com.document.hospicecalllog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LogsFragment extends Fragment {
    private CallLogRepository repository;
    private CallLogAdapter adapter;
    private Calendar selectedDate;
    private LiveData<List<CallLogEntry>> entriesLiveData;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private View totalsLayout;
    private TextView totalCallsText;
    private TextView totalDurationText;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize repository
        AppDatabase database = AppDatabase.getDatabase(requireContext());
        repository = new CallLogRepository(database.callLogDao());
        
        // Initialize selected date to current date
        selectedDate = Calendar.getInstance();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logs, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.callLogRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        totalsLayout = view.findViewById(R.id.totalsLayout);
        totalCallsText = view.findViewById(R.id.totalCallsText);
        totalDurationText = view.findViewById(R.id.totalDurationText);
        
        setupRecyclerView();
        setupDateSelector();
        loadCallLogsForSelectedDate();
    }
    
    private void setupRecyclerView() {
        adapter = new CallLogAdapter(List.of());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Set up click listeners
        adapter.setOnItemClickListener(entry -> {
            // Open edit activity
            Intent intent = new Intent(getContext(), AddCallLogActivity.class);
            intent.putExtra("entry_id", entry.getId());
            intent.putExtra("entry_data", entry);
            startActivityForResult(intent, 1);
        });
        
        adapter.setOnItemLongClickListener(entry -> {
            // Show delete confirmation dialog
            showDeleteConfirmationDialog(entry);
        });
    }
    
    private void setupDateSelector() {
        // Date selector is now handled by MainActivity
        // This method is kept for compatibility but does nothing
    }
    
    private void loadCallLogsForSelectedDate() {
        // Ensure repository is initialized
        if (repository == null) {
            if (getContext() == null) {
                return; // Fragment not attached yet
            }
            AppDatabase database = AppDatabase.getDatabase(requireContext());
            repository = new CallLogRepository(database.callLogDao());
        }
        
        // Ensure adapter is initialized (view must be created)
        if (adapter == null) {
            return; // View not created yet, will be loaded in onViewCreated
        }
        
        // Get the selected date from MainActivity
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.getSelectedDate() != null) {
            selectedDate = mainActivity.getSelectedDate();
        }
        
        // Ensure selectedDate is never null
        if (selectedDate == null) {
            selectedDate = Calendar.getInstance();
        }
        
        // Get start and end of selected date
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        // Detach previous observer(s) so old date queries can't overwrite UI
        if (entriesLiveData != null) {
            entriesLiveData.removeObservers(getViewLifecycleOwner());
        }

        entriesLiveData = repository.getEntriesByDateRange(startOfDay.getTime(), endOfDay.getTime());
        entriesLiveData.observe(getViewLifecycleOwner(), new Observer<List<CallLogEntry>>() {
            @Override
            public void onChanged(List<CallLogEntry> entries) {
                adapter.updateEntries(entries);
                updateEmptyState(entries.isEmpty());
                updateTotals(entries);
            }
        });
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            totalsLayout.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            totalsLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateTotals(List<CallLogEntry> entries) {
        int totalCalls = entries.size();
        int totalDuration = 0;
        
        for (CallLogEntry entry : entries) {
            totalDuration += entry.getDurationMinutes();
        }
        
        if (totalCallsText != null) {
            totalCallsText.setText("Total Calls: " + totalCalls);
        }
        
        if (totalDurationText != null) {
            int hours = totalDuration / 60;
            int minutes = totalDuration % 60;
            if (hours > 0) {
                totalDurationText.setText("Total Duration: " + hours + "h " + minutes + "m");
            } else {
                totalDurationText.setText("Total Duration: " + minutes + "m");
            }
        }
    }
    
    private void showDeleteConfirmationDialog(CallLogEntry entry) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this call log entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.delete(entry);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    public void refreshData() {
        // Only refresh if the fragment is properly initialized
        if (getView() != null && getContext() != null) {
            loadCallLogsForSelectedDate();
        }
    }
}
