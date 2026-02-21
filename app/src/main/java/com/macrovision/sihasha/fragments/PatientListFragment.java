package com.macrovision.sihasha.fragments;

import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.activities.AddPatientActivity;
import com.macrovision.sihasha.adapters.PatientAdapter;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PatientListFragment extends Fragment implements PatientAdapter.OnPatientClickListener, DataManager.OnDataChangedListener {

    private RecyclerView recyclerPatients;
    private PatientAdapter patientAdapter;
    private EditText editPatientSearch;
    private Spinner spinnerRiskFilter, spinnerSortOptions;
    private TextView tvPatientCount, tvPatientsTitle;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmptyState, layoutErrorState, filterTabsContainer;
    private FloatingActionButton fabAddPatient;
    private Button btnAddPatient;
    private Button btnFilterAll, btnFilterPregnant, btnFilterDelivered, btnFilterHighRisk, btnFilterReferrals;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private final List<Patient> allPatients = new ArrayList<>();
    private final List<Patient> filteredPatients = new ArrayList<>();

    private String currentFilter = "all";
    private String currentSearchTerm = "";
    private String currentRiskFilter = "all";
    private String currentSortOption = "name";

    private final Handler mainHandler = new Handler();
    private ActivityResultLauncher<Intent> addPatientLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // ✅ Register launcher FIRST before any setup
        addPatientLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadPatients();
                    }
                });

        View view = inflater.inflate(R.layout.fragment_patient_list, container, false);
        initializeComponents(view);
        setupRecyclerView();
        setupSpinners();
        setupEventListeners();
        setupFilterTabs();
        loadUserData();
        loadPatients();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataManager != null) dataManager.addDataListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dataManager != null) dataManager.removeDataListener(this);
    }

    @Override
    public void onPatientsChanged() {
        if (getActivity() != null) getActivity().runOnUiThread(this::loadPatients);
    }

    @Override
    public void onUsersChanged() {}

    private void initializeComponents(View view) {
        recyclerPatients    = view.findViewById(R.id.recycler_patients);
        editPatientSearch   = view.findViewById(R.id.edit_patient_search);
        spinnerRiskFilter   = view.findViewById(R.id.spinner_risk_filter);
        spinnerSortOptions  = view.findViewById(R.id.spinner_sort_options);
        tvPatientCount      = view.findViewById(R.id.tv_patient_count);
        tvPatientsTitle     = view.findViewById(R.id.tv_patients_title);
        progressLoading     = view.findViewById(R.id.progress_loading);
        layoutEmptyState    = view.findViewById(R.id.layout_empty_state);
        layoutErrorState    = view.findViewById(R.id.layout_error_state);
        filterTabsContainer = view.findViewById(R.id.filter_tabs_container);
        fabAddPatient       = view.findViewById(R.id.fab_add_patient);
        btnAddPatient       = view.findViewById(R.id.btn_add_patient);
        btnFilterAll        = view.findViewById(R.id.btn_filter_all);
        btnFilterPregnant   = view.findViewById(R.id.btn_filter_pregnant);
        btnFilterDelivered  = view.findViewById(R.id.btn_filter_delivered);
        btnFilterHighRisk   = view.findViewById(R.id.btn_filter_high_risk);
        btnFilterReferrals  = view.findViewById(R.id.btn_filter_referrals);

        dataManager  = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());
    }

    private void setupRecyclerView() {
        if (recyclerPatients != null) {
            recyclerPatients.setLayoutManager(new LinearLayoutManager(requireContext()));
            patientAdapter = new PatientAdapter(filteredPatients, this);
            recyclerPatients.setAdapter(patientAdapter);
        }
    }

    private void setupSpinners() {
        if (spinnerRiskFilter != null) {
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item,
                    Arrays.asList("All Risk Levels", "High Risk", "Medium Risk", "Low Risk", "Normal"));
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRiskFilter.setAdapter(a);
        }
        if (spinnerSortOptions != null) {
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item,
                    Arrays.asList("Name", "Age", "Registration Date", "Risk Level", "Village"));
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSortOptions.setAdapter(a);
        }
    }

    private void setupEventListeners() {
        if (editPatientSearch != null) {
            editPatientSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchTerm = s.toString().toLowerCase().trim();
                    applyFiltersAndSort();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (spinnerRiskFilter != null) {
            spinnerRiskFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    currentRiskFilter = convertRiskFilterToKey(p.getItemAtPosition(pos).toString());
                    applyFiltersAndSort();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        }

        if (spinnerSortOptions != null) {
            spinnerSortOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    currentSortOption = convertSortOptionToKey(p.getItemAtPosition(pos).toString());
                    applyFiltersAndSort();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        }

        // ✅ Both add buttons wired to launcher — null checked
        if (btnAddPatient != null) btnAddPatient.setOnClickListener(v -> openAddPatient());
        if (fabAddPatient != null) fabAddPatient.setOnClickListener(v -> openAddPatient());

        // ✅ Null-checked — won't crash if IDs missing from layout
        if (layoutErrorState != null) {
            View btn = layoutErrorState.findViewById(R.id.btn_retry_loading);
            if (btn != null) btn.setOnClickListener(v -> loadPatients());
        }
        if (layoutEmptyState != null) {
            View btn = layoutEmptyState.findViewById(R.id.btn_clear_filters);
            if (btn != null) btn.setOnClickListener(v -> clearAllFilters());
        }
    }

    private void openAddPatient() {
        Intent intent = new Intent(requireContext(), AddPatientActivity.class);
        addPatientLauncher.launch(intent);
    }

    private void setupFilterTabs() {
        View.OnClickListener filterClick = v -> {
            resetFilterTabStates();
            v.setSelected(true);
            if (v.getId() == R.id.btn_filter_all)          currentFilter = "all";
            else if (v.getId() == R.id.btn_filter_pregnant) currentFilter = "pregnant";
            else if (v.getId() == R.id.btn_filter_delivered) currentFilter = "delivered";
            else if (v.getId() == R.id.btn_filter_high_risk) currentFilter = "highrisk";
            else if (v.getId() == R.id.btn_filter_referrals) currentFilter = "referrals";
            applyFiltersAndSort();
        };

        if (btnFilterAll != null)      { btnFilterAll.setOnClickListener(filterClick); btnFilterAll.setSelected(true); }
        if (btnFilterPregnant != null)   btnFilterPregnant.setOnClickListener(filterClick);
        if (btnFilterDelivered != null)  btnFilterDelivered.setOnClickListener(filterClick);
        if (btnFilterHighRisk != null)   btnFilterHighRisk.setOnClickListener(filterClick);
        if (btnFilterReferrals != null)  btnFilterReferrals.setOnClickListener(filterClick);
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null && tvPatientsTitle != null) {
            tvPatientsTitle.setText("asha".equals(currentUser.getRole()) ? "My Patients" : "Patients");
        }
    }

    private void loadPatients() {
        showLoading(true);
        mainHandler.postDelayed(() -> {
            try {
                allPatients.clear();
                if (dataManager != null) {
                    if (currentUser != null && "asha".equals(currentUser.getRole())) {
                        allPatients.addAll(dataManager.getPatientsForASHA(currentUser.getId()));
                    } else {
                        allPatients.addAll(dataManager.getAllPatients());
                    }
                }
                applyFiltersAndSort();
                showLoading(false);
            } catch (Exception e) {
                Log.e("PatientListFragment", "Error loading patients", e);
                showError();
                showLoading(false);
            }
        }, 300);
    }

    private void applyFiltersAndSort() {
        filteredPatients.clear();
        List<Patient> working = new ArrayList<>(allPatients);
        working = applyCategoryFilter(working);
        working = applyRiskFilter(working);
        working = applySearchFilter(working);
        working = applySorting(working);
        filteredPatients.addAll(working);
        updateUI();
    }

    private List<Patient> applyCategoryFilter(List<Patient> patients) {
        if ("all".equals(currentFilter)) return patients;
        List<Patient> out = new ArrayList<>();
        for (Patient p : patients) {
            switch (currentFilter) {
                case "pregnant":  if ("pregnant".equals(p.getPregnancyStatus()))  out.add(p); break;
                case "delivered": if ("delivered".equals(p.getPregnancyStatus())) out.add(p); break;
                case "highrisk":
                case "referrals": if (p.isHighRisk()) out.add(p); break;
                default:          out.add(p); break;
            }
        }
        return out;
    }

    private List<Patient> applyRiskFilter(List<Patient> patients) {
        if ("all".equals(currentRiskFilter)) return patients;
        List<Patient> out = new ArrayList<>();
        for (Patient p : patients) {
            switch (currentRiskFilter) {
                case "high":   if (p.isHighRisk()) out.add(p); break;
                case "medium": if (!p.isHighRisk() && p.getAge() > 35) out.add(p); break;
                case "low":
                case "normal": if (!p.isHighRisk() && p.getAge() <= 35) out.add(p); break;
            }
        }
        return out;
    }

    private List<Patient> applySearchFilter(List<Patient> patients) {
        if (currentSearchTerm.isEmpty()) return patients;
        List<Patient> out = new ArrayList<>();
        for (Patient p : patients) {
            if ((p.getName() != null && p.getName().toLowerCase().contains(currentSearchTerm)) ||
                (p.getVillage() != null && p.getVillage().toLowerCase().contains(currentSearchTerm)) ||
                (p.getPhoneNumber() != null && p.getPhoneNumber().contains(currentSearchTerm)) ||
                String.valueOf(p.getAge()).contains(currentSearchTerm)) {
                out.add(p);
            }
        }
        return out;
    }

    private List<Patient> applySorting(List<Patient> patients) {
        List<Patient> sorted = new ArrayList<>(patients);
        Collections.sort(sorted, (p1, p2) -> {
            switch (currentSortOption) {
                case "age":     return Integer.compare(p2.getAge(), p1.getAge());
                case "date":    return compareStrings(p2.getRegistrationDate(), p1.getRegistrationDate());
                case "risk":    return Boolean.compare(p2.isHighRisk(), p1.isHighRisk());
                case "village": return compareStrings(p1.getVillage(), p2.getVillage());
                default:        return compareStrings(p1.getName(), p2.getName());
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

    private String convertRiskFilterToKey(String d) {
        switch (d) {
            case "High Risk":   return "high";
            case "Medium Risk": return "medium";
            case "Low Risk":    return "low";
            case "Normal":      return "normal";
            default:            return "all";
        }
    }

    private String convertSortOptionToKey(String d) {
        switch (d) {
            case "Age":               return "age";
            case "Registration Date": return "date";
            case "Risk Level":        return "risk";
            case "Village":           return "village";
            default:                  return "name";
        }
    }

    private void updateUI() {
        if (tvPatientCount != null) {
            int c = filteredPatients.size();
            tvPatientCount.setText(c == 0 ? "No patients found" : c + (c == 1 ? " patient found" : " patients found"));
        }
        if (filteredPatients.isEmpty()) showEmptyState();
        else showPatientList();
        if (patientAdapter != null) patientAdapter.notifyDataSetChanged();
    }

    private void resetFilterTabStates() {
        if (btnFilterAll != null)      btnFilterAll.setSelected(false);
        if (btnFilterPregnant != null)  btnFilterPregnant.setSelected(false);
        if (btnFilterDelivered != null) btnFilterDelivered.setSelected(false);
        if (btnFilterHighRisk != null)  btnFilterHighRisk.setSelected(false);
        if (btnFilterReferrals != null) btnFilterReferrals.setSelected(false);
    }

    private void clearAllFilters() {
        currentFilter = "all"; currentRiskFilter = "all";
        currentSearchTerm = ""; currentSortOption = "name";
        if (editPatientSearch != null)  editPatientSearch.setText("");
        if (spinnerRiskFilter != null)  spinnerRiskFilter.setSelection(0);
        if (spinnerSortOptions != null) spinnerSortOptions.setSelection(0);
        resetFilterTabStates();
        if (btnFilterAll != null) btnFilterAll.setSelected(true);
        applyFiltersAndSort();
    }

    private void showLoading(boolean show) {
        if (progressLoading != null)  progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerPatients != null) recyclerPatients.setVisibility(show ? View.GONE : View.VISIBLE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
    }

    private void showPatientList() {
        if (recyclerPatients != null) recyclerPatients.setVisibility(View.VISIBLE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
        if (progressLoading != null)  progressLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (recyclerPatients != null) recyclerPatients.setVisibility(View.GONE);
        if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
        if (progressLoading != null)  progressLoading.setVisibility(View.GONE);
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            TextView msg = layoutEmptyState.findViewById(R.id.tv_empty_message);
            if (msg != null) msg.setText(hasActiveFilters() ? "Try adjusting your filters" : "No patients registered yet");
        }
    }

    private void showError() {
        if (recyclerPatients != null) recyclerPatients.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (layoutErrorState != null) layoutErrorState.setVisibility(View.VISIBLE);
        if (progressLoading != null)  progressLoading.setVisibility(View.GONE);
    }

    private boolean hasActiveFilters() {
        return !currentFilter.equals("all") || !currentRiskFilter.equals("all") || !currentSearchTerm.isEmpty();
    }

    @Override
    public void onPatientClick(Patient patient) { if (patient != null) showPatientDetail(patient); }

    @Override
    public void onPatientLongClick(Patient patient) { if (patient != null) showPatientContextMenu(patient); }

    private void showPatientDetail(Patient patient) {
        String details = "Name: " + patient.getName() + "\n" +
                "Age: " + patient.getAge() + " years\n" +
                "Village: " + (patient.getVillage() != null ? patient.getVillage() : "N/A") + "\n" +
                "Phone: " + (patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "N/A") + "\n" +
                "Status: " + (patient.getPregnancyStatus() != null ? patient.getPregnancyStatus() : "N/A") + "\n" +
                "Risk: " + (patient.isHighRisk() ? "High Risk" : "Normal");

        new AlertDialog.Builder(requireContext())
                .setTitle("Patient Information")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Edit", (d, w) -> {
                    Intent intent = new Intent(requireContext(), AddPatientActivity.class);
                    intent.putExtra(AddPatientActivity.EXTRA_IS_EDIT_MODE, true);
                    intent.putExtra(AddPatientActivity.EXTRA_PATIENT_ID, patient.getId());
                    addPatientLauncher.launch(intent);
                })
                .show();
    }

    private void showPatientContextMenu(Patient patient) {
        new AlertDialog.Builder(requireContext())
                .setTitle(patient.getName())
                .setItems(new String[]{"View Details", "Edit Patient", "Schedule Visit", "Add Note"}, (dialog, which) -> {
                    switch (which) {
                        case 0: showPatientDetail(patient); break;
                        case 1:
                            Intent intent = new Intent(requireContext(), AddPatientActivity.class);
                            intent.putExtra(AddPatientActivity.EXTRA_IS_EDIT_MODE, true);
                            intent.putExtra(AddPatientActivity.EXTRA_PATIENT_ID, patient.getId());
                            addPatientLauncher.launch(intent);
                            break;
                        case 2:
                            Toast.makeText(requireContext(), "Schedule visit: " + patient.getName(), Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            Toast.makeText(requireContext(), "Add note: " + patient.getName(), Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    public void refreshPatients() { loadPatients(); }

    public void filterByStatus(String status) {
        resetFilterTabStates();
        currentFilter = status;
        switch (status) {
            case "pregnant":  if (btnFilterPregnant != null)  btnFilterPregnant.setSelected(true);  break;
            case "delivered": if (btnFilterDelivered != null) btnFilterDelivered.setSelected(true); break;
            case "highrisk":  if (btnFilterHighRisk != null)  btnFilterHighRisk.setSelected(true);  break;
            default:
                currentFilter = "all";
                if (btnFilterAll != null) btnFilterAll.setSelected(true);
                break;
        }
        applyFiltersAndSort();
    }
}