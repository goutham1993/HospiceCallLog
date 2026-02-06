package com.document.hospicecalllog;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.document.hospicecalllog.databinding.ActivityAddCallLogBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddCallLogActivity extends AppCompatActivity {
    private ActivityAddCallLogBinding binding;
    private CallLogRepository repository;
    private CallLogEntry editingEntry;
    private Calendar selectedDate;
    private int selectedDurationMinutes = 12; // Default duration
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCallLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize repository
        AppDatabase database = AppDatabase.getDatabase(this);
        repository = new CallLogRepository(database.callLogDao());
        
        setupUI();
        setupDatePicker();
        setupClickListeners();
        
        // Check if we're editing an existing entry
        Intent intent = getIntent();
        if (intent.hasExtra("entry_id")) {
            int entryId = intent.getIntExtra("entry_id", -1);
            if (entryId != -1) {
                // Load existing entry for editing
                loadEntryForEditing(entryId);
            }
        }
    }
    
    private void setupUI() {
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(editingEntry != null ? "Edit Call Log" : "Add Call Log");
        }
        
        // Initialize date - use selected date from main activity if available, otherwise current date
        Intent intent = getIntent();
        if (intent.hasExtra("selected_date")) {
            long selectedDateMillis = intent.getLongExtra("selected_date", System.currentTimeMillis());
            selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(selectedDateMillis);
        } else {
            selectedDate = Calendar.getInstance();
        }
        updateDateDisplay();
        
        // Setup duration spinner
        setupDurationSpinner();
    }
    
    private void loadEntryForEditing(int entryId) {
        // For now, we'll handle this in the main activity by passing the entry data
        // In a real app, you might want to load from database here
        if (getIntent().hasExtra("entry_data")) {
            editingEntry = (CallLogEntry) getIntent().getSerializableExtra("entry_data");
            if (editingEntry != null) {
                populateFields();
            }
        }
    }
    
    private void populateFields() {
        if (editingEntry != null) {
            binding.medicalRecordNumberEditText.setText(editingEntry.getMedicalRecordNumber());
            binding.nameEditText.setText(editingEntry.getName());
            binding.notesEditText.setText(editingEntry.getNotes());
            binding.actionEditText.setText(editingEntry.getAction());
            
            // Set duration spinner selection
            int duration = editingEntry.getDurationMinutes();
            int spinnerPosition = (duration - 6) / 6; // Convert duration to spinner position
            if (spinnerPosition >= 0 && spinnerPosition < binding.durationSpinner.getCount()) {
                binding.durationSpinner.setSelection(spinnerPosition);
                selectedDurationMinutes = duration;
            }
            
            // Ensure selectedDate is initialized before setting time
            if (selectedDate == null) {
                selectedDate = Calendar.getInstance();
            }
            selectedDate.setTime(editingEntry.getDate());
            updateDateDisplay();
        }
    }
    
    private void setupDatePicker() {
        binding.dateEditText.setOnClickListener(v -> showDatePicker());
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        binding.dateEditText.setText(sdf.format(selectedDate.getTime()));
    }
    
    private void setupClickListeners() {
        binding.saveButton.setOnClickListener(v -> saveEntry());
        binding.cancelButton.setOnClickListener(v -> finish());
    }
    
    private void saveEntry() {
        String medicalRecordNumber = binding.medicalRecordNumberEditText.getText().toString().trim();
        String name = binding.nameEditText.getText().toString().trim();
        String notes = binding.notesEditText.getText().toString().trim();
        String action = binding.actionEditText.getText().toString().trim();
        
        // Use selected duration from spinner
        int durationMinutes = selectedDurationMinutes;
        
        // Create or update entry
        if (editingEntry != null) {
            // Update existing entry
            editingEntry.setMedicalRecordNumber(medicalRecordNumber.isEmpty() ? null : medicalRecordNumber);
            editingEntry.setName(name.isEmpty() ? null : name);
            editingEntry.setNotes(notes.isEmpty() ? null : notes);
            editingEntry.setAction(action.isEmpty() ? null : action);
            editingEntry.setDate(selectedDate.getTime());
            editingEntry.setDurationMinutes(durationMinutes);
            repository.update(editingEntry);
            Toast.makeText(this, "Call log updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            // Create new entry
            CallLogEntry newEntry = new CallLogEntry(
                    medicalRecordNumber.isEmpty() ? null : medicalRecordNumber,
                    name.isEmpty() ? null : name,
                    notes.isEmpty() ? null : notes,
                    action.isEmpty() ? null : action,
                    selectedDate.getTime(),
                    durationMinutes
            );
            repository.insert(newEntry);
            Toast.makeText(this, "Call log added successfully", Toast.LENGTH_SHORT).show();
        }
        
        // Return to main activity
        setResult(RESULT_OK);
        finish();
    }
    
    private void setupDurationSpinner() {
        // Create duration options: 6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, 84, 90, 96, 102, 108, 114, 120
        List<String> durationOptions = new ArrayList<>();
        for (int i = 6; i <= 120; i += 6) {
            durationOptions.add(String.valueOf(i));
        }
        
        // Check if we're in dark mode
        boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, durationOptions) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((android.widget.TextView) view).setTextColor(getResources().getColor(
                    isDarkMode ? android.R.color.white : android.R.color.black, getTheme()));
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) view).setTextColor(getResources().getColor(
                    isDarkMode ? android.R.color.white : android.R.color.black, getTheme()));
                view.setBackgroundColor(getResources().getColor(
                    isDarkMode ? android.R.color.black : android.R.color.white, getTheme()));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.durationSpinner.setAdapter(adapter);
        
        // Set default selection to 12 minutes (index 1)
        binding.durationSpinner.setSelection(1);
        
        binding.durationSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedDurationMinutes = Integer.parseInt(durationOptions.get(position));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedDurationMinutes = 12; // Default to 12 minutes
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
