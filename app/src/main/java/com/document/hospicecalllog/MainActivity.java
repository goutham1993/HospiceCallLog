package com.document.hospicecalllog;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.document.hospicecalllog.databinding.ActivityMainBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private LogsFragment logsFragment;
    private SummaryFragment summaryFragment;
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

        // Initialize selected date to current date
        selectedDate = Calendar.getInstance();

        setupViewPager();
        setupDateSelector();
        setupFAB();
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        
        // Create fragments
        logsFragment = new LogsFragment();
        summaryFragment = new SummaryFragment();
        
        // Create adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Logs");
                    break;
                case 1:
                    tab.setText("Summary");
                    break;
            }
        }).attach();
    }

    private void setupDateSelector() {
        updateDateDisplay();
        
        ImageView calendarIcon = findViewById(R.id.calendarIcon);
        if (calendarIcon != null) {
            calendarIcon.setOnClickListener(v -> {
                showDatePicker();
            });
        }
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                    refreshFragments();
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
        
        TextView selectedDateText = findViewById(R.id.selectedDateText);
        if (selectedDateText != null) {
            selectedDateText.setText(dateText);
        }
    }

    private void refreshFragments() {
        if (logsFragment != null) {
            logsFragment.refreshData();
        }
        if (summaryFragment != null) {
            summaryFragment.refreshData();
        }
    }

    public Calendar getSelectedDate() {
        return selectedDate;
    }

    private void setupFAB() {
        binding.fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCallLogActivity.class);
            startActivityForResult(intent, ADD_CALL_LOG_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_CALL_LOG_REQUEST && resultCode == RESULT_OK) {
            refreshFragments();
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
            Intent intent = new Intent(this, ExportDataActivity.class);
            startActivity(intent);
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

    private class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return logsFragment;
                case 1:
                    return summaryFragment;
                default:
                    return logsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}