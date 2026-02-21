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

public class DoctorReferralManagementFragment extends Fragment implements PatientAdapter.OnPatientClickListener {

    private RecyclerView recyclerView;
    private PatientAdapter adapter;
    private TextView tvReferralCount;
    private LinearLayout layoutNoData; // tv_no_data in XML is a LinearLayout — bind correctly
    private ChipGroup chipGroupStatus;
    private Chip chipPending, chipAccepted, chipRejected, chipAll;
    private Button btnAccept, btnReject, btnAddDiagnosis;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<Patient> referralPatients = new ArrayList<>();
    private List<Patient> filteredPatients = new ArrayList<>();
    private String currentStatus = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_referral_management, container, false);
        try {
            initializeViews(view);
            setupDataManager();
            setupRecyclerView();
            setupChipGroup();
            loadReferrals();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null)
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_referrals);
        tvReferralCount = view.findViewById(R.id.tv_referral_count);
        layoutNoData = view.findViewById(R.id.tv_no_data); // LinearLayout in XML
        chipGroupStatus = view.findViewById(R.id.chip_group_status);
        chipPending = view.findViewById(R.id.chip_pending);
        chipAccepted = view.findViewById(R.id.chip_accepted);
        chipRejected = view.findViewById(R.id.chip_rejected);
        chipAll = view.findViewById(R.id.chip_all);
        btnAccept = view.findViewById(R.id.btn_accept);
        btnReject = view.findViewById(R.id.btn_reject);
        btnAddDiagnosis = view.findViewById(R.id.btn_add_diagnosis);
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
        if (chipPending != null)
            chipPending.setOnCheckedChangeListener((b, c) -> { if (c) { currentStatus = "pending"; filterByStatus(); } });
        if (chipAccepted != null)
            chipAccepted.setOnCheckedChangeListener((b, c) -> { if (c) { currentStatus = "accepted"; filterByStatus(); } });
        if (chipRejected != null)
            chipRejected.setOnCheckedChangeListener((b, c) -> { if (c) { currentStatus = "rejected"; filterByStatus(); } });
        if (chipAll != null)
            chipAll.setOnCheckedChangeListener((b, c) -> { if (c) { currentStatus = "all"; filterByStatus(); } });
        if (btnAccept != null)
            btnAccept.setOnClickListener(v -> Toast.makeText(getContext(), "Select a referral to accept", Toast.LENGTH_SHORT).show());
        if (btnReject != null)
            btnReject.setOnClickListener(v -> Toast.makeText(getContext(), "Select a referral to reject", Toast.LENGTH_SHORT).show());
        if (btnAddDiagnosis != null)
            btnAddDiagnosis.setOnClickListener(v -> Toast.makeText(getContext(), "Select a patient to add diagnosis", Toast.LENGTH_SHORT).show());
    }

    private void loadReferrals() {
        if (dataManager == null || currentUser == null) {
            showNoData();
            return;
        }
        try {
            // Only show patients assigned to THIS doctor, not all patients
            referralPatients = dataManager.getPatientsForDoctor(currentUser.getId());

            if (referralPatients == null || referralPatients.isEmpty()) {
                showNoData();
                if (tvReferralCount != null) tvReferralCount.setText("0");
            } else {
                hideNoData();
                if (tvReferralCount != null) tvReferralCount.setText(String.valueOf(referralPatients.size()));
                filterByStatus();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showNoData();
        }
    }

    private void filterByStatus() {
        if (filteredPatients == null) return;
        filteredPatients.clear();
        if (referralPatients != null) filteredPatients.addAll(referralPatients);
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
    public void onPatientClick(Patient patient) { if (patient != null) showReferralDialog(patient); }

    @Override
    public void onPatientLongClick(Patient patient) { if (patient != null) showReferralContextMenu(patient); }

    private void showReferralDialog(Patient patient) {
        if (getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Referral Action - " + patient.getName())
                .setItems(new String[]{"Accept Referral", "Reject Referral", "Add Diagnosis", "View Details"}, (d, w) -> {
                    switch (w) {
                        case 0: Toast.makeText(getContext(), "Accepted: " + patient.getName(), Toast.LENGTH_SHORT).show(); break;
                        case 1: Toast.makeText(getContext(), "Rejected: " + patient.getName(), Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(getContext(), "Diagnosis saved for " + patient.getName(), Toast.LENGTH_SHORT).show(); break;
                        case 3: showPatientDetails(patient); break;
                    }
                }).show();
    }

    private void showReferralContextMenu(Patient patient) {
        if (getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Referral Management")
                .setItems(new String[]{"Accept", "Reject", "Add Diagnosis"}, (d, w) -> {
                    switch (w) {
                        case 0: Toast.makeText(getContext(), "Accepted: " + patient.getName(), Toast.LENGTH_SHORT).show(); break;
                        case 1: Toast.makeText(getContext(), "Rejected: " + patient.getName(), Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(getContext(), "Diagnosis saved", Toast.LENGTH_SHORT).show(); break;
                    }
                }).show();
    }

    private void showPatientDetails(Patient patient) {
        if (getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Patient Details")
                .setMessage("Patient: " + patient.getName() + "\nAge: " + patient.getAge() +
                        "\nStatus: " + (patient.getPregnancyStatus() != null ? patient.getPregnancyStatus() : "N/A") +
                        "\nRisk: " + (patient.isHighRisk() ? "HIGH" : "Normal"))
                .setPositiveButton("OK", null).show();
    }

    public void refreshData() { loadReferrals(); }
}