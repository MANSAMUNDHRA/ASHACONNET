package com.macrovision.sihasha;

import android.app.AlertDialog;
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

public class fragment_patient_list extends Fragment implements PatientAdapter.OnPatientClickListener {

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
        View view = inflater.inflate(R.layout.fragment_patient_list, container, false);

        initializeComponents(view);
        setupRecyclerView();
        Log.d("PatientListFragment", "Adding test data...");
        filteredPatients.clear();
        filteredPatients.add(new Patient("TEST001", "Test Patient 1", 25, "1234567890", "Test Village", "pregnant", "ASHA001", "PHC001", false));
        filteredPatients.add(new Patient("TEST002", "Test Patient 2", 28, "0987654321", "Test Village 2", "delivered", "ASHA001", "PHC001", false));
        patientAdapter.notifyDataSetChanged();
        Log.d("PatientListFragment", "Test data added: " + filteredPatients.size() + " patients");

        setupSpinners();
        setupEventListeners();
        setupFilterTabs();
        loadUserData();
        loadPatients();

        return view;
    }


    private void initializeComponents(View view) {
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

        // Initialize data managers
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());

        // Initialize lists
        allPatients = new ArrayList<>();
        filteredPatients = new ArrayList<>();
        // ✅ Add debug log
        Log.d("PatientListFragment", "Components initialized");
    }

    private void setupRecyclerView() {
        recyclerPatients.setLayoutManager(new LinearLayoutManager(requireContext()));
        patientAdapter = new PatientAdapter(filteredPatients, this);
        recyclerPatients.setAdapter(patientAdapter);
    }

    private void setupSpinners() {
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
    }

    private void setupEventListeners() {
        // Search functionality
        editPatientSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchTerm = s.toString().toLowerCase().trim();
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Risk filter spinner
        spinnerRiskFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                currentRiskFilter = convertRiskFilterToKey(selected);
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sort options spinner
        spinnerSortOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                currentSortOption = convertSortOptionToKey(selected);
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add patient buttons
        btnAddPatient.setOnClickListener(v -> showAddPatientDialog());
        fabAddPatient.setOnClickListener(v -> showAddPatientDialog());

        // Error state retry button
        layoutErrorState.findViewById(R.id.btn_retry_loading)
                .setOnClickListener(v -> {
                    showLoading(true);
                    loadPatients();
                });

        // Empty state clear filters button
        layoutEmptyState.findViewById(R.id.btn_clear_filters)
                .setOnClickListener(v -> clearAllFilters());
    }

    private void setupFilterTabs() {
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

                applyFiltersAndSort();
            }
        };

        btnFilterAll.setOnClickListener(filterClickListener);
        btnFilterPregnant.setOnClickListener(filterClickListener);
        btnFilterDelivered.setOnClickListener(filterClickListener);
        btnFilterHighRisk.setOnClickListener(filterClickListener);
        btnFilterReferrals.setOnClickListener(filterClickListener);

        // Set default selected state
        btnFilterAll.setSelected(true);
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            // Update title based on user role
            String title = "Patients";
            if ("asha".equals(currentUser.getRole())) {
                title = "My Patients";
            }
            tvPatientsTitle.setText(title);
        }
    }

    private void loadPatients() {
        showLoading(true);
        Log.d("PatientListFragment", "Starting to load patients...");

        // Simulate async loading with delay
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // Load patients based on user role
                    Log.d("PatientListFragment", "Current user: " + (currentUser != null ? currentUser.getName() : "null"));

                    if (currentUser != null && "asha".equals(currentUser.getRole())) {
                        allPatients = dataManager.getPatientsForASHA(currentUser.getId());
                        Log.d("PatientListFragment", "ASHA patients loaded: " + allPatients.size());

                    } else {
                        allPatients = dataManager.getAllPatients();
                        Log.d("PatientListFragment", "All patients loaded: " + allPatients.size());

                    }
                    for (Patient p : allPatients) {
                        Log.d("PatientListFragment", "Patient: " + p.getName() + ", Village: " + p.getVillage());
                    }

                    applyFiltersAndSort();
                    showLoading(false);

                } catch (Exception e) {
                    Log.e("PatientListFragment", "Error loading patients", e);
                    showError();
                    showLoading(false);
                }
            }
        }, 1000); // 1 second delay to simulate network call
    }

    private void applyFiltersAndSort() {
        filteredPatients.clear();

        // Start with all patients
        List<Patient> workingList = new ArrayList<>(allPatients);

        // Apply category filter
        workingList = applyCategoryFilter(workingList);

        // Apply risk filter
        workingList = applyRiskFilter(workingList);

        // Apply search filter
        workingList = applySearchFilter(workingList);

        // Apply sorting
        workingList = applySorting(workingList);

        // Update filtered list
        filteredPatients.addAll(workingList);

        // Update UI
        updateUI();
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
                    // Add logic for referrals if available in your data structure
                    // For now, including high risk patients as referrals
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
                    // Add medium risk logic if available
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
                        return Integer.compare(p2.getAge(), p1.getAge()); // Descending
                    case "date":
                        return compareStrings(p2.getRegistrationDate(), p1.getRegistrationDate()); // Latest first
                    case "risk":
                        return Boolean.compare(p2.isHighRisk(), p1.isHighRisk()); // High risk first
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
        updatePatientCount();

        if (filteredPatients.isEmpty()) {
            showEmptyState();
        } else {
            showPatientList();
            patientAdapter.notifyDataSetChanged();
        }
        Log.d("PatientList", "Patients loaded: " + filteredPatients.size());

    }

    private void updatePatientCount() {
        int count = filteredPatients.size();
        String countText;

        if (count == 0) {
            countText = "No patients found";
        } else if (count == 1) {
            countText = "1 patient found";
        } else {
            countText = count + " patients found";
        }

        tvPatientCount.setText(countText);
    }

    private void resetFilterTabStates() {
        btnFilterAll.setSelected(false);
        btnFilterPregnant.setSelected(false);
        btnFilterDelivered.setSelected(false);
        btnFilterHighRisk.setSelected(false);
        btnFilterReferrals.setSelected(false);
    }

    private void clearAllFilters() {
        // Reset filter states
        currentFilter = "all";
        currentRiskFilter = "all";
        currentSearchTerm = "";
        currentSortOption = "name";

        // Reset UI controls
        editPatientSearch.setText("");
        spinnerRiskFilter.setSelection(0);
        spinnerSortOptions.setSelection(0);

        // Reset filter tabs
        resetFilterTabStates();
        btnFilterAll.setSelected(true);

        // Apply filters
        applyFiltersAndSort();
    }

    private void showAddPatientDialog() {
        // For now, show a simple dialog. You can implement a full add patient form later
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Patient")
                .setMessage("Add Patient functionality will be implemented here.")
                .setPositiveButton("OK", null)
                .show();
    }

    // UI State Management Methods
    private void showLoading(boolean show) {
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerPatients.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
    }

    private void showPatientList() {
        recyclerPatients.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
        progressLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerPatients.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutErrorState.setVisibility(View.GONE);
        progressLoading.setVisibility(View.GONE);

        // Update empty state message based on filters
        TextView emptyMessage = layoutEmptyState.findViewById(R.id.tv_empty_message);
        if (hasActiveFilters()) {
            emptyMessage.setText("Try adjusting your filters or search terms");
        } else {
            emptyMessage.setText("No patients registered yet");
        }
    }

    private void showError() {
        recyclerPatients.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.VISIBLE);
        progressLoading.setVisibility(View.GONE);
    }

    private boolean hasActiveFilters() {
        return !currentFilter.equals("all") ||
                !currentRiskFilter.equals("all") ||
                !currentSearchTerm.isEmpty();
    }

    // PatientAdapter.OnPatientClickListener implementation
    @Override
    public void onPatientClick(Patient patient) {
        // Handle patient click - navigate to patient detail or show patient info
        showPatientDetail(patient);
    }

    @Override
    public void onPatientLongClick(Patient patient) {
        // Handle long click - show context menu or additional options
        showPatientContextMenu(patient);
    }

    private void showPatientDetail(Patient patient) {
        // Create a simple dialog showing patient details
        // In a real app, you would navigate to a detailed patient screen
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
                    // TODO: Implement edit patient functionality
                    Toast.makeText(requireContext(), "Edit functionality will be implemented", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showPatientContextMenu(Patient patient) {
        String[] options = {"View Details", "Edit Patient", "Schedule Visit", "Add Note"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Patient Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // View Details
                            showPatientDetail(patient);
                            break;
                        case 1: // Edit Patient
                            Toast.makeText(requireContext(), "Edit patient: " + patient.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case 2: // Schedule Visit
                            Toast.makeText(requireContext(), "Schedule visit for: " + patient.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case 3: // Add Note
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
        editPatientSearch.setText(query);
    }

    public void filterByStatus(String status) {
        resetFilterTabStates();
        currentFilter = status;

        switch (status) {
            case "pregnant":
                btnFilterPregnant.setSelected(true);
                break;
            case "delivered":
                btnFilterDelivered.setSelected(true);
                break;
            case "highrisk":
                btnFilterHighRisk.setSelected(true);
                break;
            default:
                btnFilterAll.setSelected(true);
                currentFilter = "all";
                break;
        }

        applyFiltersAndSort();
    }
}
