package com.document.hospicecalllog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.document.hospicecalllog.databinding.ActivityMainBinding;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private CallLogRepository repository;
    private CallLogAdapter adapter;
    private Calendar selectedDate;
    private static final int ADD_CALL_LOG_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hospice Call Log");
        }
        
        // Set toolbar background color to match app theme
        binding.toolbar.setBackgroundColor(getResources().getColor(R.color.app_primary, getTheme()));
        binding.toolbar.setTitleTextColor(getResources().getColor(android.R.color.white, getTheme()));
        
        // Set toolbar icon colors to white for visibility on purple background
        binding.toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_more_vert_white, getTheme()));

        // Initialize repository
        AppDatabase database = AppDatabase.getDatabase(this);
        repository = new CallLogRepository(database.callLogDao());

        // Initialize selected date to current date
        selectedDate = Calendar.getInstance();

        setupRecyclerView();
        setupDateSelector();
        setupFAB();
        loadCallLogsForSelectedDate();
    }

    private void setupRecyclerView() {
        adapter = new CallLogAdapter(List.of());
        RecyclerView recyclerView = findViewById(R.id.callLogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set up click listeners
        adapter.setOnItemClickListener(entry -> {
            // Open edit activity
            Intent intent = new Intent(this, AddCallLogActivity.class);
            intent.putExtra("entry_id", entry.getId());
            intent.putExtra("entry_data", entry);
            startActivityForResult(intent, ADD_CALL_LOG_REQUEST);
        });

        adapter.setOnItemLongClickListener(entry -> {
            // Show delete confirmation dialog
            showDeleteConfirmationDialog(entry);
        });
    }

    private void setupDateSelector() {
        updateDateDisplay();
        
        ImageView calendarIcon = findViewById(R.id.calendarIcon);
        if (calendarIcon != null) {
            calendarIcon.setOnClickListener(v -> {
                android.util.Log.d("MainActivity", "Calendar icon clicked");
                showDatePicker();
            });
        } else {
            android.util.Log.e("MainActivity", "Calendar icon not found!");
        }
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                    loadCallLogsForSelectedDate();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        String dateText = sdf.format(selectedDate.getTime());
        
        // Check if it's today
        Calendar today = Calendar.getInstance();
        if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            dateText = "Today (" + dateText + ")";
        }
        
        ((android.widget.TextView) findViewById(R.id.selectedDateText)).setText(dateText);
    }

    private void setupFAB() {
        binding.fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCallLogActivity.class);
            // Pass the currently selected date to the add activity
            intent.putExtra("selected_date", selectedDate.getTimeInMillis());
            startActivityForResult(intent, ADD_CALL_LOG_REQUEST);
        });
    }

    private void loadCallLogsForSelectedDate() {
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

        repository.getEntriesByDateRange(startOfDay.getTime(), endOfDay.getTime())
                .observe(this, new Observer<List<CallLogEntry>>() {
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
            findViewById(R.id.emptyStateLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.callLogRecyclerView).setVisibility(View.GONE);
            findViewById(R.id.totalsLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.emptyStateLayout).setVisibility(View.GONE);
            findViewById(R.id.callLogRecyclerView).setVisibility(View.VISIBLE);
            findViewById(R.id.totalsLayout).setVisibility(View.VISIBLE);
        }
    }
    
    private void updateTotals(List<CallLogEntry> entries) {
        int totalMinutes = 0;
        for (CallLogEntry entry : entries) {
            totalMinutes += entry.getDurationMinutes();
        }
        
        double totalHours = totalMinutes / 60.0;
        
        ((android.widget.TextView) findViewById(R.id.totalMinutesText)).setText(totalMinutes + " minutes");
        ((android.widget.TextView) findViewById(R.id.totalHoursText)).setText(String.format("%.1f hours", totalHours));
    }

    private void showDeleteConfirmationDialog(CallLogEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Call Log")
                .setMessage("Are you sure you want to delete this call log entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.delete(entry);
                    // Show success message
                    View rootView = findViewById(android.R.id.content);
                    com.google.android.material.snackbar.Snackbar.make(rootView, "Call log deleted", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_CALL_LOG_REQUEST && resultCode == RESULT_OK) {
            // Refresh the list
            loadCallLogsForSelectedDate();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {
            // Handle search action
            showSearchDialog();
            return true;
        } else if (id == R.id.action_export_data) {
            // Handle export data action
            android.widget.Toast.makeText(this, "Export Data - Coming Soon", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_import_data) {
            // Handle import data action
            android.widget.Toast.makeText(this, "Import Data - Coming Soon", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_clear_all_data) {
            // Handle clear all data action
            showClearDataConfirmation();
            return true;
        } else if (id == R.id.action_about) {
            // Handle about action
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void showSearchDialog() {
        // Create a simple search dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Search Call Logs");
        
        // Create input field
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Search by name, MRN, or notes...");
        builder.setView(input);
        
        builder.setPositiveButton("Search", (dialog, which) -> {
            String searchText = input.getText().toString().trim();
            if (!searchText.isEmpty()) {
                // TODO: Implement search functionality
                android.widget.Toast.makeText(this, "Searching for: " + searchText, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    
    private void showClearDataConfirmation() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to delete all call log entries? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    // TODO: Implement clear all data functionality
                    android.widget.Toast.makeText(this, "Clear All Data - Coming Soon", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showAboutDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("About Hospice Call Log")
                .setMessage("Version 1.0\n\nA simple and efficient call log application for hospice care.")
                .setPositiveButton("OK", null)
                .show();
    }
}