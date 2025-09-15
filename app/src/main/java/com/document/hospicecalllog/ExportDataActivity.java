package com.document.hospicecalllog;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import com.document.hospicecalllog.databinding.ActivityExportDataBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportDataActivity extends AppCompatActivity {
    private ActivityExportDataBinding binding;
    private CallLogRepository repository;
    private Calendar selectedMonth;
    private List<CallLogEntry> entriesToExport;
    private String selectedFormat = "JSON";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityExportDataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Export Data");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize repository
        AppDatabase database = AppDatabase.getDatabase(this);
        repository = new CallLogRepository(database.callLogDao());

        // Initialize selected month to current month
        selectedMonth = Calendar.getInstance();
        selectedMonth.set(Calendar.DAY_OF_MONTH, 1); // Set to first day of month

        setupFormatSpinner();
        setupMonthSelector();
        setupExportButton();
        updateMonthDisplay();
    }

    private void setupFormatSpinner() {
        String[] formats = {"JSON", "CSV"};
        
        // Check if we're in dark mode
        boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, formats) {
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
        binding.formatSpinner.setAdapter(adapter);
        
        binding.formatSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedFormat = formats[position];
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedFormat = "JSON";
            }
        });
    }

    private void setupMonthSelector() {
        binding.monthSelectorButton.setOnClickListener(v -> showMonthPicker());
    }

    private void showMonthPicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedMonth.set(year, month, 1);
                    updateMonthDisplay();
                    loadEntriesForSelectedMonth();
                },
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH),
                1
        );
        datePickerDialog.show();
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthText = sdf.format(selectedMonth.getTime());
        binding.monthSelectorButton.setText(monthText);
    }

    private void setupExportButton() {
        binding.exportButton.setOnClickListener(v -> {
            if (entriesToExport == null || entriesToExport.isEmpty()) {
                Toast.makeText(this, "No data to export for selected month", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try to export directly - the saveFile method will handle fallback
            if (selectedFormat.equals("JSON")) {
                exportToJSON();
            } else {
                exportToCSV();
            }
        });
    }

    private void loadEntriesForSelectedMonth() {
        // Get start and end of selected month
        Calendar startOfMonth = (Calendar) selectedMonth.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);

        Calendar endOfMonth = (Calendar) selectedMonth.clone();
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);
        endOfMonth.set(Calendar.MILLISECOND, 999);

        repository.getEntriesByDateRange(startOfMonth.getTime(), endOfMonth.getTime())
                .observe(this, new Observer<List<CallLogEntry>>() {
                    @Override
                    public void onChanged(List<CallLogEntry> entries) {
                        entriesToExport = entries;
                        updateExportInfo(entries.size());
                    }
                });
    }

    private void updateExportInfo(int entryCount) {
        if (entryCount > 0) {
            binding.exportInfoText.setText("Found " + entryCount + " entries for export");
            binding.exportButton.setEnabled(true);
        } else {
            binding.exportInfoText.setText("No entries found for selected month");
            binding.exportButton.setEnabled(false);
        }
    }

    private void exportToJSON() {
        try {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .setPrettyPrinting()
                    .create();

            String json = gson.toJson(entriesToExport);

            String fileName = "hospice_call_log_" + 
                    new SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(selectedMonth.getTime()) + ".json";

            if (saveFile(fileName, json)) {
                showExportSuccessDialog(fileName);
            } else {
                Toast.makeText(this, "Failed to save JSON file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error creating JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToCSV() {
        try {
            StringBuilder csv = new StringBuilder();
            
            // Add header
            csv.append("ID,Medical Record Number,Name,Notes,Action,Date,Duration (Minutes)\n");
            
            // Add data rows
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (CallLogEntry entry : entriesToExport) {
                csv.append(entry.getId()).append(",");
                csv.append(escapeCsvField(entry.getMedicalRecordNumber())).append(",");
                csv.append(escapeCsvField(entry.getName())).append(",");
                csv.append(escapeCsvField(entry.getNotes())).append(",");
                csv.append(escapeCsvField(entry.getAction())).append(",");
                csv.append(dateFormat.format(entry.getDate())).append(",");
                csv.append(entry.getDurationMinutes()).append("\n");
            }

            String fileName = "hospice_call_log_" + 
                    new SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(selectedMonth.getTime()) + ".csv";

            if (saveFile(fileName, csv.toString())) {
                showExportSuccessDialog(fileName);
            } else {
                Toast.makeText(this, "Failed to save CSV file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error creating CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private boolean saveFile(String fileName, String content) {
        try {
            // Try to save to Downloads directory first
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File file = new File(downloadsDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();

            // Notify the system about the new file
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            // If external storage fails, try to save to app's external files directory
            try {
                File appDir = new File(getExternalFilesDir(null), "exports");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                File file = new File(appDir, fileName);
                FileWriter writer = new FileWriter(file);
                writer.write(content);
                writer.close();
                
                // Show success with alternative location
                showExportSuccessDialog(fileName, file.getAbsolutePath());
                return true;
            } catch (IOException e2) {
                e2.printStackTrace();
                return false;
            }
        }
    }

    private void showExportSuccessDialog(String fileName) {
        showExportSuccessDialog(fileName, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
    }

    private void showExportSuccessDialog(String fileName, String filePath) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Export Successful")
                .setMessage("Data exported successfully to:\n" + fileName + "\n\nFile saved to: " + filePath)
                .setPositiveButton("OK", (dialogInterface, which) -> {
                    // Optionally open the folder
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(filePath), "resource/folder");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Close", null)
                .create();
        
        dialog.show();
        
        // Check if we're in dark mode
        boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        // Set button text colors based on theme
        int textColor = getResources().getColor(
            isDarkMode ? android.R.color.white : android.R.color.black, getTheme());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEntriesForSelectedMonth();
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with export
                if (selectedFormat.equals("JSON")) {
                    exportToJSON();
                } else {
                    exportToCSV();
                }
            } else {
                Toast.makeText(this, "Storage permission is required to export data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
