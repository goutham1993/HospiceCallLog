package com.document.hospicecalllog;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.document.hospicecalllog.databinding.ActivityAddCallLogBinding;
import java.util.Calendar;
import java.util.Date;

public class AddCallLogActivity extends AppCompatActivity {
    private ActivityAddCallLogBinding binding;
    private CallLogRepository repository;
    private CallLogEntry editingEntry;
    private Calendar selectedDate;
    
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
            binding.durationEditText.setText(String.valueOf(editingEntry.getDurationMinutes()));
            
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
        String durationText = binding.durationEditText.getText().toString().trim();
        
        // No required field validation needed - all fields are now optional
        
        int durationMinutes = 6; // Default value
        if (!durationText.isEmpty()) {
            try {
                durationMinutes = Integer.parseInt(durationText);
                if (durationMinutes <= 0) {
                    binding.durationEditText.setError("Duration must be greater than 0");
                    binding.durationEditText.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.durationEditText.setError("Please enter a valid number");
                binding.durationEditText.requestFocus();
                return;
            }
        }
        
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
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
