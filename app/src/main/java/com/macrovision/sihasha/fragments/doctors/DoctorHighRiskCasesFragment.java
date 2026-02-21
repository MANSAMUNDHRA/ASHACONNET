package com.macrovision.sihasha.fragments.doctors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.adapters.PatientAdapter;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class DoctorHighRiskCasesFragment extends Fragment implements PatientAdapter.OnPatientClickListener {

    private RecyclerView recyclerView;
    private PatientAdapter adapter;
    private TextView tvHighRiskCount;
    private LinearLayout layoutNoData;
    private ChipGroup chipGroupSeverity;
    private Chip chipAll, chipCritical, chipSevere, chipModerate;
    private Button btnAddNotes, btnEscalate;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<Patient> highRiskPatients = new ArrayList<>();
    private List<Patient> filteredPatients = new ArrayList<>();
    private String currentSeverity = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_high_risk_cases, container, false);
        try {
            initializeViews(view);
            setupDataManager();
            setupRecyclerView();
            setupChipGroup();
            loadHighRiskPatients();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null)
                Toast.makeText(getContext(), "Error loading high risk cases: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_high_risk);
        tvHighRiskCount = view.findViewById(R.id.tv_high_risk_count);
        layoutNoData = view.findViewById(R.id.tv_no_data);
        chipGroupSeverity = view.findViewById(R.id.chip_group_severity);
        chipAll = view.findViewById(R.id.chip_all);
        chipCritical = view.findViewById(R.id.chip_critical);
        chipSevere = view.findViewById(R.id.chip_severe);
        chipModerate = view.findViewById(R.id.chip_moderate);
        btnAddNotes = view.findViewById(R.id.btn_add_notes);
        btnEscalate = view.findViewById(R.id.btn_escalate);
    }

    private void setupDataManager() {
        if (getContext() != null) {
            dataManager = DataManager.getInstance(getContext());
            prefsManager = new SharedPrefsManager(getContext());
            currentUser = prefsManager.getCurrentUser();
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null && getContext() != null) {
            adapter = new PatientAdapter(filteredPatients, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupChipGroup() {
        if (chipAll != null)
            chipAll.setOnCheckedChangeListener((b, c) -> { if (c) { currentSeverity = "all"; filterBySeverity(); } });
        if (chipCritical != null)
            chipCritical.setOnCheckedChangeListener((b, c) -> { if (c) { currentSeverity = "critical"; filterBySeverity(); } });
        if (chipSevere != null)
            chipSevere.setOnCheckedChangeListener((b, c) -> { if (c) { currentSeverity = "severe"; filterBySeverity(); } });
        if (chipModerate != null)
            chipModerate.setOnCheckedChangeListener((b, c) -> { if (c) { currentSeverity = "moderate"; filterBySeverity(); } });
        if (btnAddNotes != null)
            btnAddNotes.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Select a patient to add notes", Toast.LENGTH_SHORT).show());
        if (btnEscalate != null)
            btnEscalate.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Select a patient to escalate", Toast.LENGTH_SHORT).show());
    }

    private void loadHighRiskPatients() {
        if (dataManager == null || currentUser == null) {
            showNoData();
            return;
        }
        try {
            // ✅ FIX: Doctors see ALL high-risk patients in the system,
            // not just those with assignedDoctor == doctorId
            // (assignedDoctor is never set when ASHA workers add patients)
            highRiskPatients = dataManager.getHighRiskPatients();

            if (highRiskPatients == null || highRiskPatients.isEmpty()) {
                showNoData();
                if (tvHighRiskCount != null) tvHighRiskCount.setText("0 High Risk Cases");
            } else {
                hideNoData();
                if (tvHighRiskCount != null)
                    tvHighRiskCount.setText(highRiskPatients.size() + " High Risk Cases");
                filterBySeverity();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showNoData();
        }
    }

    private void filterBySeverity() {
        if (filteredPatients == null) return;
        filteredPatients.clear();
        if (highRiskPatients != null) filteredPatients.addAll(highRiskPatients);
        if (adapter != null) adapter.notifyDataSetChanged();
        if (filteredPatients.isEmpty()) showNoData(); else hideNoData();
    }

    private void showNoData() {
        if (layoutNoData != null) layoutNoData.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
    }

    private void hideNoData() {
        if (layoutNoData != null) layoutNoData.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPatientClick(Patient patient) {
        if (patient != null) showPatientClinicalDialog(patient);
    }

    @Override
    public void onPatientLongClick(Patient patient) {
        if (patient != null) showClinicalContextMenu(patient);
    }

    private void showPatientClinicalDialog(Patient patient) {
        if (getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("High Risk Patient")
                .setMessage("👤 " + patient.getName() +
                        "\nAge: " + patient.getAge() + " years" +
                        "\nVillage: " + (patient.getVillage() != null ? patient.getVillage() : "N/A") +
                        "\nStatus: " + (patient.getPregnancyStatus() != null ? patient.getPregnancyStatus() : "N/A") +
                        "\n⚠️ HIGH RISK PATIENT")
                .setPositiveButton("Add Notes", (d, w) ->
                        Toast.makeText(getContext(), "Add clinical notes for " + patient.getName(), Toast.LENGTH_SHORT).show())
                .setNeutralButton("Escalate", (d, w) ->
                        Toast.makeText(getContext(), "Escalated: " + patient.getName(), Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showClinicalContextMenu(Patient patient) {
        if (getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clinical Actions")
                .setItems(new String[]{"View Details", "Add Clinical Notes", "Update Risk Status"}, (d, w) -> {
                    switch (w) {
                        case 0: showPatientClinicalDialog(patient); break;
                        case 1: Toast.makeText(getContext(), "Add clinical notes", Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(getContext(), "Update risk status", Toast.LENGTH_SHORT).show(); break;
                    }
                }).show();
    }

    public void refreshData() {
        loadHighRiskPatients();
    }
}