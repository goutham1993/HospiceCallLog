package com.document.hospicecalllog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SummaryFragment extends Fragment {
    private CallLogRepository repository;
    private SummaryAdapter adapter;
    private Calendar selectedDate;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private TextView totalActionsText;
    
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
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.summaryRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        totalActionsText = view.findViewById(R.id.totalActionsText);
        
        setupRecyclerView();
        setupDateSelector();
        loadActionCountsForSelectedDate();
    }
    
    private void setupRecyclerView() {
        adapter = new SummaryAdapter(List.of());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupDateSelector() {
        // Date selector is now handled by MainActivity
        // This method is kept for compatibility but does nothing
    }
    
    private void loadActionCountsForSelectedDate() {
        // Get the selected date from MainActivity
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            selectedDate = mainActivity.getSelectedDate();
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

        repository.getActionCountsByDateRange(startOfDay.getTime(), endOfDay.getTime())
                .observe(getViewLifecycleOwner(), new Observer<List<ActionCount>>() {
                    @Override
                    public void onChanged(List<ActionCount> actionCounts) {
                        adapter.updateActionCounts(actionCounts);
                        updateEmptyState(actionCounts.isEmpty());
                        updateTotalActions(actionCounts);
                    }
                });
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateTotalActions(List<ActionCount> actionCounts) {
        int totalActions = 0;
        for (ActionCount actionCount : actionCounts) {
            totalActions += actionCount.getCount();
        }
        
        if (totalActionsText != null) {
            totalActionsText.setText("Total Actions: " + totalActions);
        }
    }
    
    public void refreshData() {
        loadActionCountsForSelectedDate();
    }
}
