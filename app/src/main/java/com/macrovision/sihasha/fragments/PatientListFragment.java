package com.macrovision.sihasha.fragments;

import static android.app.Activity.RESULT_OK;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.macrovision.sihasha.activities.AddPatientActivity;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.adapters.PatientAdapter;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PatientListFragment extends Fragment implements PatientAdapter.OnPatientClickListener {

    private static final String TAG = "PatientListFragment";

    // UI Components
    private RecyclerView recyclerPatients;
    private PatientAdapter patientAdapter;
    private EditText editPatientSearch;
    private Spinner spinnerRiskFilter, spinnerSortOptions;
    private TextView tvPatientCount, tvPatientsTitle;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmptyState, layoutErrorState, filterTabsContainer;
    private FloatingActionButton fabAddPatient;
    private Button btnAddPatient;

    // Filter Tab Buttons
    private Button btnFilterAll, btnFilterPregnant, btnFilterDelivered,
            btnFilterHighRisk, btnFilterReferrals;

    // Data Management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<Patient> allPatients;
    private List<Patient> filteredPatients;

    // Filter States
    private String currentFilter = "all";
    private String currentSearchTerm = "";
    private String currentRiskFilter = "all";
    private String currentSortOption = "name";

    // Handler for async operations
    private Handler mainHandler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_patient_list, container, false);

        try {
            initializeComponents(view);
            setupRecyclerView();
            setupSpinners();
            setupEventListeners();
            setupFilterTabs();
            loadUserData();
            loadPatients();

            Log.d(TAG, "Fragment setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
        }

        return view;
    }

    private void initializeComponents(View view) {
        Log.d(TAG, "Initializing components...");

        // Initialize UI components
        recyclerPatients = view.findViewById(R.id.recycler_patients);
        editPatientSearch = view.findViewById(R.id.edit_patient_search);
        spinnerRiskFilter = view.findViewById(R.id.spinner_risk_filter);
        spinnerSortOptions = view.findViewById(R.id.spinner_sort_options);
        tvPatientCount = view.findViewById(R.id.tv_patient_count);
        tvPatientsTitle = view.findViewById(R.id.tv_patients_title);
        progressLoading = view.findViewById(R.id.progress_loading);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutErrorState = view.findViewById(R.id.layout_error_state);
        filterTabsContainer = view.findViewById(R.id.filter_tabs_container);
        fabAddPatient = view.findViewById(R.id.fab_add_patient);
        btnAddPatient = view.findViewById(R.id.btn_add_patient);

        // Filter tab buttons
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterPregnant = view.findViewById(R.id.btn_filter_pregnant);
        btnFilterDelivered = view.findViewById(R.id.btn_filter_delivered);
        btnFilterHighRisk = view.findViewById(R.id.btn_filter_high_risk);
        btnFilterReferrals = view.findViewById(R.id.btn_filter_referrals);

        // Check if views are found
        Log.d(TAG, "RecyclerView found: " + (recyclerPatients != null));
        Log.d(TAG, "Search EditText found: " + (editPatientSearch != null));
        Log.d(TAG, "Patient Count TextView found: " + (tvPatientCount != null));

        // Initialize data managers
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());

        // Initialize lists
        allPatients = new ArrayList<>();
        filteredPatients = new ArrayList<>();

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView...");

        if (recyclerPatients == null) {
            Log.e(TAG, "RecyclerView is null!");
            return;
        }

        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            recyclerPatients.setLayoutManager(layoutManager);

            // Create adapter with empty list initially
            patientAdapter = new PatientAdapter(filteredPatients, this);
            recyclerPatients.setAdapter(patientAdapter);

            Log.d(TAG, "RecyclerView setup completed with adapter: " + (patientAdapter != null));

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");

        if (spinnerRiskFilter == null || spinnerSortOptions == null) {
            Log.e(TAG, "Spinners are null!");
            return;
        }

        try {
            // Risk Filter Spinner
            List<String> riskLevels = Arrays.asList(
                    "All Risk Levels", "High Risk", "Medium Risk", "Low Risk", "Normal"
            );
            ArrayAdapter<String> riskAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    riskLevels
            );
            riskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRiskFilter.setAdapter(riskAdapter);

            // Sort Options Spinner
            List<String> sortOptions = Arrays.asList(
                    "Name", "Age", "Registration Date", "Risk Level", "Village"
            );
            ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    sortOptions
            );
            sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSortOptions.setAdapter(sortAdapter);

            Log.d(TAG, "Spinners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    private void setupEventListeners() {
        Log.d(TAG, "Setting up event listeners...");

        try {
            // Search functionality
            if (editPatientSearch != null) {
                editPatientSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        currentSearchTerm = s.toString().toLowerCase().trim();
                        Log.d(TAG, "Search term changed: " + currentSearchTerm);
                        applyFiltersAndSort();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }

            // Risk filter spinner
            if (spinnerRiskFilter != null) {
                spinnerRiskFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selected = parent.getItemAtPosition(position).toString();
                        currentRiskFilter = convertRiskFilterToKey(selected);
                        Log.d(TAG, "Risk filter changed: " + currentRiskFilter);
                        applyFiltersAndSort();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            // Sort options spinner
            if (spinnerSortOptions != null) {
                spinnerSortOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selected = parent.getItemAtPosition(position).toString();
                        currentSortOption = convertSortOptionToKey(selected);
                        Log.d(TAG, "Sort option changed: " + currentSortOption);
                        applyFiltersAndSort();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            // Add patient buttons
            if (btnAddPatient != null) {
                btnAddPatient.setOnClickListener(v -> showAddPatientDialog());
            }
            if (fabAddPatient != null) {
                fabAddPatient.setOnClickListener(v -> showAddPatientDialog());
            }

            // Error state retry button
            if (layoutErrorState != null) {
                Button retryBtn = layoutErrorState.findViewById(R.id.btn_retry_loading);
                if (retryBtn != null) {
                    retryBtn.setOnClickListener(v -> {
                        showLoading(true);
                        loadPatients();
                    });
                }
            }

            // Empty state clear filters button
            if (layoutEmptyState != null) {
                Button clearBtn = layoutEmptyState.findViewById(R.id.btn_clear_filters);
                if (clearBtn != null) {
                    clearBtn.setOnClickListener(v -> clearAllFilters());
                }
            }

            Log.d(TAG, "Event listeners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up event listeners", e);
        }
    }

    private void setupFilterTabs() {
        Log.d(TAG, "Setting up filter tabs...");

        try {
            View.OnClickListener filterClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetFilterTabStates();
                    v.setSelected(true);

                    if (v.getId() == R.id.btn_filter_all) {
                        currentFilter = "all";
                    } else if (v.getId() == R.id.btn_filter_pregnant) {
                        currentFilter = "pregnant";
                    } else if (v.getId() == R.id.btn_filter_delivered) {
                        currentFilter = "delivered";
                    } else if (v.getId() == R.id.btn_filter_high_risk) {
                        currentFilter = "highrisk";
                    } else if (v.getId() == R.id.btn_filter_referrals) {
                        currentFilter = "referrals";
                    }

                    Log.d(TAG, "Filter changed to: " + currentFilter);
                    applyFiltersAndSort();
                }
            };

            if (btnFilterAll != null) btnFilterAll.setOnClickListener(filterClickListener);
            if (btnFilterPregnant != null) btnFilterPregnant.setOnClickListener(filterClickListener);
            if (btnFilterDelivered != null) btnFilterDelivered.setOnClickListener(filterClickListener);
            if (btnFilterHighRisk != null) btnFilterHighRisk.setOnClickListener(filterClickListener);
            if (btnFilterReferrals != null) btnFilterReferrals.setOnClickListener(filterClickListener);

            // Set default selected state
            if (btnFilterAll != null) {
                btnFilterAll.setSelected(true);
            }

            Log.d(TAG, "Filter tabs setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter tabs", e);
        }
    }

    private void loadUserData() {
        Log.d(TAG, "Loading user data...");

        try {
            currentUser = prefsManager.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

                // Update title based on user role
                String title = "Patients";
                if ("asha".equals(currentUser.getRole())) {
                    title = "My Patients";
                }

                if (tvPatientsTitle != null) {
                    tvPatientsTitle.setText(title);
                }
            } else {
                Log.w(TAG, "Current user is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
        }
    }

    private void loadPatients() {
        Log.d(TAG, "Loading patients...");
        showLoading(true);

        // Use immediate execution for testing
        try {
            Log.d(TAG, "Current user: " + (currentUser != null ? currentUser.getName() : "null"));

            if (currentUser != null && "asha".equals(currentUser.getRole())) {
                allPatients = dataManager.getPatientsForASHA(currentUser.getId());
                Log.d(TAG, "ASHA patients loaded: " + allPatients.size());
            } else {
                allPatients = dataManager.getAllPatients();
                Log.d(TAG, "All patients loaded: " + allPatients.size());
            }

            // Debug each patient
            for (int i = 0; i < allPatients.size(); i++) {
                Patient p = allPatients.get(i);
                Log.d(TAG, "Patient " + i + ": " + p.getName() + ", Village: " + p.getVillage() + ", Status: " + p.getPregnancyStatus());
            }

            applyFiltersAndSort();
            showLoading(false);

        } catch (Exception e) {
            Log.e(TAG, "Error loading patients", e);
            showError();
            showLoading(false);
        }
    }

    private void applyFiltersAndSort() {
        Log.d(TAG, "Applying filters and sort...");

        try {
            filteredPatients.clear();

            // Start with all patients
            List<Patient> workingList = new ArrayList<>(allPatients);
            Log.d(TAG, "Starting with " + workingList.size() + " patients");

            // Apply category filter
            workingList = applyCategoryFilter(workingList);
            Log.d(TAG, "After category filter: " + workingList.size() + " patients");

            // Apply risk filter
            workingList = applyRiskFilter(workingList);
            Log.d(TAG, "After risk filter: " + workingList.size() + " patients");

            // Apply search filter
            workingList = applySearchFilter(workingList);
            Log.d(TAG, "After search filter: " + workingList.size() + " patients");

            // Apply sorting
            workingList = applySorting(workingList);
            Log.d(TAG, "After sorting: " + workingList.size() + " patients");

            // Update filtered list
            filteredPatients.addAll(workingList);

            // Update UI
            updateUI();

        } catch (Exception e) {
            Log.e(TAG, "Error applying filters", e);
        }
    }

    private List<Patient> applyCategoryFilter(List<Patient> patients) {
        if ("all".equals(currentFilter)) {
            return patients;
        }

        List<Patient> filtered = new ArrayList<>();
        for (Patient patient : patients) {
            switch (currentFilter) {
                case "pregnant":
                    if ("pregnant".equals(patient.getPregnancyStatus())) {
                        filtered.add(patient);
                    }
                    break;
                case "delivered":
                    if ("delivered".equals(patient.getPregnancyStatus())) {
                        filtered.add(patient);
                    }
                    break;
                case "highrisk":
                    if (patient.isHighRisk()) {
                        filtered.add(patient);
                    }
                    break;
                case "referrals":
                    if (patient.isHighRisk()) {
                        filtered.add(patient);
                    }
                    break;
                default:
                    filtered.add(patient);
                    break;
            }
        }
        return filtered;
    }

    private List<Patient> applyRiskFilter(List<Patient> patients) {
        if ("all".equals(currentRiskFilter)) {
            return patients;
        }

        List<Patient> filtered = new ArrayList<>();
        for (Patient patient : patients) {
            switch (currentRiskFilter) {
                case "high":
                    if (patient.isHighRisk()) {
                        filtered.add(patient);
                    }
                    break;
                case "medium":
                    if (!patient.isHighRisk() && patient.getAge() > 35) {
                        filtered.add(patient);
                    }
                    break;
                case "low":
                case "normal":
                    if (!patient.isHighRisk() && patient.getAge() <= 35) {
                        filtered.add(patient);
                    }
                    break;
            }
        }
        return filtered;
    }

    private List<Patient> applySearchFilter(List<Patient> patients) {
        if (currentSearchTerm.isEmpty()) {
            return patients;
        }

        List<Patient> filtered = new ArrayList<>();
        for (Patient patient : patients) {
            if (matchesSearchTerm(patient)) {
                filtered.add(patient);
            }
        }
        return filtered;
    }

    private boolean matchesSearchTerm(Patient patient) {
        String searchLower = currentSearchTerm.toLowerCase();

        return (patient.getName() != null && patient.getName().toLowerCase().contains(searchLower)) ||
                (patient.getVillage() != null && patient.getVillage().toLowerCase().contains(searchLower)) ||
                (patient.getPhoneNumber() != null && patient.getPhoneNumber().contains(searchLower)) ||
                (patient.getHusbandName() != null && patient.getHusbandName().toLowerCase().contains(searchLower)) ||
                String.valueOf(patient.getAge()).contains(searchLower);
    }

    private List<Patient> applySorting(List<Patient> patients) {
        List<Patient> sorted = new ArrayList<>(patients);

        Collections.sort(sorted, new Comparator<Patient>() {
            @Override
            public int compare(Patient p1, Patient p2) {
                switch (currentSortOption) {
                    case "name":
                        return compareStrings(p1.getName(), p2.getName());
                    case "age":
                        return Integer.compare(p2.getAge(), p1.getAge());
                    case "date":
                        return compareStrings(p2.getRegistrationDate(), p1.getRegistrationDate());
                    case "risk":
                        return Boolean.compare(p2.isHighRisk(), p1.isHighRisk());
                    case "village":
                        return compareStrings(p1.getVillage(), p2.getVillage());
                    default:
                        return compareStrings(p1.getName(), p2.getName());
                }
            }
        });

        return sorted;
    }

    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;
        return s1.compareToIgnoreCase(s2);
    }

    private String convertRiskFilterToKey(String displayName) {
        switch (displayName) {
            case "High Risk": return "high";
            case "Medium Risk": return "medium";
            case "Low Risk": return "low";
            case "Normal": return "normal";
            default: return "all";
        }
    }

    private String convertSortOptionToKey(String displayName) {
        switch (displayName) {
            case "Name": return "name";
            case "Age": return "age";
            case "Registration Date": return "date";
            case "Risk Level": return "risk";
            case "Village": return "village";
            default: return "name";
        }
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI with " + filteredPatients.size() + " filtered patients");

        try {
            updatePatientCount();

            if (filteredPatients.isEmpty()) {
                Log.d(TAG, "Showing empty state");
                showEmptyState();
            } else {
                Log.d(TAG, "Showing patient list");
                showPatientList();
                if (patientAdapter != null) {
                    patientAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Adapter notified of data change");
                } else {
                    Log.e(TAG, "Patient adapter is null!");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    private void updatePatientCount() {
        try {
            int count = filteredPatients.size();
            String countText;

            if (count == 0) {
                countText = "No patients found";
            } else if (count == 1) {
                countText = "1 patient found";
            } else {
                countText = count + " patients found";
            }

            if (tvPatientCount != null) {
                tvPatientCount.setText(countText);
            }

            Log.d(TAG, "Patient count updated: " + countText);

        } catch (Exception e) {
            Log.e(TAG, "Error updating patient count", e);
        }
    }

    private void resetFilterTabStates() {
        if (btnFilterAll != null) btnFilterAll.setSelected(false);
        if (btnFilterPregnant != null) btnFilterPregnant.setSelected(false);
        if (btnFilterDelivered != null) btnFilterDelivered.setSelected(false);
        if (btnFilterHighRisk != null) btnFilterHighRisk.setSelected(false);
        if (btnFilterReferrals != null) btnFilterReferrals.setSelected(false);
    }

    private void clearAllFilters() {
        Log.d(TAG, "Clearing all filters");

        // Reset filter states
        currentFilter = "all";
        currentRiskFilter = "all";
        currentSearchTerm = "";
        currentSortOption = "name";

        // Reset UI controls
        if (editPatientSearch != null) editPatientSearch.setText("");
        if (spinnerRiskFilter != null) spinnerRiskFilter.setSelection(0);
        if (spinnerSortOptions != null) spinnerSortOptions.setSelection(0);

        // Reset filter tabs
        resetFilterTabStates();
        if (btnFilterAll != null) btnFilterAll.setSelected(true);

        // Apply filters
        applyFiltersAndSort();
    }

    // Update the showAddPatientDialog method in PatientListFragment
    private void showAddPatientDialog() {
        Intent intent = new Intent(requireContext(), AddPatientActivity.class);
        startActivityForResult(intent, 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1001) {
                // Patient was added successfully
                Toast.makeText(requireContext(), "Patient added successfully!", Toast.LENGTH_SHORT).show();
                loadPatients();
            } else if (requestCode == 1002) {
                // Patient was updated successfully
                Toast.makeText(requireContext(), "Patient updated successfully!", Toast.LENGTH_SHORT).show();
                loadPatients();
            }
        }
    }

    // UI State Management Methods
    private void showLoading(boolean show) {
        try {
            if (progressLoading != null) {
                progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerPatients != null) {
                recyclerPatients.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.GONE);
            }
            if (layoutErrorState != null) {
                layoutErrorState.setVisibility(View.GONE);
            }

            Log.d(TAG, "Loading state: " + show);
        } catch (Exception e) {
            Log.e(TAG, "Error in showLoading", e);
        }
    }

    private void showPatientList() {
        try {
            if (recyclerPatients != null) {
                recyclerPatients.setVisibility(View.VISIBLE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.GONE);
            }
            if (layoutErrorState != null) {
                layoutErrorState.setVisibility(View.GONE);
            }
            if (progressLoading != null) {
                progressLoading.setVisibility(View.GONE);
            }

            Log.d(TAG, "Showing patient list");
        } catch (Exception e) {
            Log.e(TAG, "Error in showPatientList", e);
        }
    }

    private void showEmptyState() {
        try {
            if (recyclerPatients != null) {
                recyclerPatients.setVisibility(View.GONE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
            if (layoutErrorState != null) {
                layoutErrorState.setVisibility(View.GONE);
            }
            if (progressLoading != null) {
                progressLoading.setVisibility(View.GONE);
            }

            // Update empty state message based on filters
            TextView emptyMessage = layoutEmptyState.findViewById(R.id.tv_empty_message);
            if (emptyMessage != null) {
                if (hasActiveFilters()) {
                    emptyMessage.setText("Try adjusting your filters or search terms");
                } else {
                    emptyMessage.setText("No patients registered yet");
                }
            }

            Log.d(TAG, "Showing empty state");
        } catch (Exception e) {
            Log.e(TAG, "Error in showEmptyState", e);
        }
    }

    private void showError() {
        try {
            if (recyclerPatients != null) {
                recyclerPatients.setVisibility(View.GONE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.GONE);
            }
            if (layoutErrorState != null) {
                layoutErrorState.setVisibility(View.VISIBLE);
            }
            if (progressLoading != null) {
                progressLoading.setVisibility(View.GONE);
            }

            Log.d(TAG, "Showing error state");
        } catch (Exception e) {
            Log.e(TAG, "Error in showError", e);
        }
    }

    private boolean hasActiveFilters() {
        return !currentFilter.equals("all") ||
                !currentRiskFilter.equals("all") ||
                !currentSearchTerm.isEmpty();
    }

    // PatientAdapter.OnPatientClickListener implementation
    @Override
    public void onPatientClick(Patient patient) {
        Log.d(TAG, "Patient clicked: " + patient.getName());
        showPatientDetail(patient);
    }

    @Override
    public void onPatientLongClick(Patient patient) {
        Log.d(TAG, "Patient long clicked: " + patient.getName());
        showPatientContextMenu(patient);
    }

    private void showPatientDetail(Patient patient) {
        String details = "Patient Details:\n\n" +
                "Name: " + patient.getName() + "\n" +
                "Age: " + patient.getAge() + " years\n" +
                "Village: " + patient.getVillage() + "\n" +
                "Phone: " + patient.getPhoneNumber() + "\n" +
                "Status: " + patient.getPregnancyStatus() + "\n" +
                "Risk Level: " + (patient.isHighRisk() ? "High Risk" : "Normal");

        new AlertDialog.Builder(requireContext())
                .setTitle("Patient Information")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Edit", (dialog, which) -> {
                    editPatient(patient);
                })
                .show();
    }

    private void showPatientContextMenu(Patient patient) {
        String[] options = {"View Details", "Edit Patient", "Schedule Visit", "Add Note"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Patient Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showPatientDetail(patient);
                            break;
                        case 1:
                            editPatient(patient);
                            break;
                        case 2:
                            Toast.makeText(requireContext(), "Schedule visit for: " + patient.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            Toast.makeText(requireContext(), "Add note for: " + patient.getName(), Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    // Public methods for external access
    public void refreshPatients() {
        loadPatients();
    }

    public void searchPatients(String query) {
        if (editPatientSearch != null) {
            editPatientSearch.setText(query);
        }
    }

    private void editPatient(Patient patient) {
        Log.d(TAG, "Editing patient: " + patient.getName());

        Intent intent = new Intent(requireContext(), AddPatientActivity.class);
        intent.putExtra(AddPatientActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(AddPatientActivity.EXTRA_PATIENT_ID, patient.getId());
        startActivityForResult(intent, 1002); // Different request code for edit
    }

    private void confirmDeletePatient(Patient patient) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Patient")
                .setMessage("Are you sure you want to delete " + patient.getName() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deletePatient(patient);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePatient(Patient patient) {
        boolean success = dataManager.deletePatient(patient.getId());

        if (success) {
            Toast.makeText(requireContext(), "Patient deleted successfully", Toast.LENGTH_SHORT).show();
            loadPatients(); // Refresh the list
        } else {
            Toast.makeText(requireContext(), "Failed to delete patient", Toast.LENGTH_SHORT).show();
        }
    }

    public void filterByStatus(String status) {
        resetFilterTabStates();
        currentFilter = status;

        switch (status) {
            case "pregnant":
                if (btnFilterPregnant != null) btnFilterPregnant.setSelected(true);
                break;
            case "delivered":
                if (btnFilterDelivered != null) btnFilterDelivered.setSelected(true);
                break;
            case "highrisk":
                if (btnFilterHighRisk != null) btnFilterHighRisk.setSelected(true);
                break;
            default:
                if (btnFilterAll != null) btnFilterAll.setSelected(true);
                currentFilter = "all";
                break;
        }

        applyFiltersAndSort();
    }
}
