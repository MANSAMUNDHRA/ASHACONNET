package com.macrovision.sihasha.fragments.asha;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.adapters.PatientAdapter;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class AShaReferralFragment extends Fragment implements PatientAdapter.OnPatientClickListener {

    private static final String TAG = "AShaReferralFragment";
    
    private RecyclerView recyclerView;
    private PatientAdapter adapter;
    private TextView tvReferralCount, tvNoData;
    private Button btnReferPatient;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<Patient> myPatients = new ArrayList<>();
    private List<Patient> referredPatients = new ArrayList<>();
    private List<Patient> displayPatients = new ArrayList<>();
    private boolean showReferred = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_asha_referral, container, false);
        
        try {
            initializeViews(view);
            setupDataManager();
            setupRecyclerView();
            setupClickListeners();
            
            // Load patients after everything is set up
            loadPatients();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_referrals);
        tvReferralCount = view.findViewById(R.id.tv_referral_count);
        tvNoData = view.findViewById(R.id.tv_no_data);
        btnReferPatient = view.findViewById(R.id.btn_refer_patient);
    }

    private void setupDataManager() {
        if (getContext() != null) {
            dataManager = DataManager.getInstance(getContext());
            prefsManager = new SharedPrefsManager(getContext());
            
            // Try to get current user
            currentUser = prefsManager.getCurrentUser();
            Log.d(TAG, "Current user from prefs: " + (currentUser != null ? currentUser.getName() : "null"));
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null && getContext() != null) {
            adapter = new PatientAdapter(displayPatients, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupClickListeners() {
        if (btnReferPatient != null) {
            btnReferPatient.setOnClickListener(v -> {
                toggleView();
            });
        }
    }
    
    private void toggleView() {
        showReferred = !showReferred;
        if (showReferred) {
            btnReferPatient.setText("Show My Patients");
            displayPatients.clear();
            displayPatients.addAll(referredPatients);
            if (tvReferralCount != null) {
                tvReferralCount.setText("Referred: " + referredPatients.size());
            }
        } else {
            btnReferPatient.setText("Show Referred Patients");
            displayPatients.clear();
            displayPatients.addAll(myPatients);
            if (tvReferralCount != null) {
                tvReferralCount.setText("My Patients: " + myPatients.size());
            }
        }
        updateUI();
    }

    private void loadPatients() {
        // Try to reload user if null
        if (currentUser == null && getContext() != null) {
            Log.d(TAG, "Current user is null, trying to reload...");
            prefsManager = new SharedPrefsManager(getContext());
            currentUser = prefsManager.getCurrentUser();
        }
        
        // Check if user is still null
        if (currentUser == null) {
            Log.e(TAG, "Current user is still null after reload");
            Toast.makeText(getContext(), "Please login again", Toast.LENGTH_LONG).show();
            
            // Option: Redirect to login
            // You can add code here to go back to login screen
            return;
        }
        
        if (dataManager == null && getContext() != null) {
            dataManager = DataManager.getInstance(getContext());
        }
        
        if (dataManager == null) {
            Toast.makeText(getContext(), "DataManager not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Log.d(TAG, "Loading patients for ASHA: " + currentUser.getId());
            myPatients = dataManager.getPatientsForASHA(currentUser.getId());
            
            if (myPatients == null) {
                myPatients = new ArrayList<>();
            }
            
            Log.d(TAG, "Found " + myPatients.size() + " patients");
            
            referredPatients = new ArrayList<>();
            
            // Filter referred patients
            for (Patient p : myPatients) {
                if (p != null) {
                    try {
                        if (p.isReferred()) {
                            referredPatients.add(p);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking referral status", e);
                    }
                }
            }
            
            // Set display list based on current view
            displayPatients.clear();
            if (!showReferred) {
                displayPatients.addAll(myPatients);
                if (tvReferralCount != null) {
                    tvReferralCount.setText("My Patients: " + myPatients.size());
                }
            } else {
                displayPatients.addAll(referredPatients);
                if (tvReferralCount != null) {
                    tvReferralCount.setText("Referred: " + referredPatients.size());
                }
            }
            
            updateUI();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading patients", e);
            Toast.makeText(getContext(), "Error loading patients: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        if (displayPatients == null || displayPatients.isEmpty()) {
            if (tvNoData != null) tvNoData.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (tvNoData != null) tvNoData.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPatientClick(Patient patient) {
        if (patient != null) {
            showReferralDialog(patient);
        }
    }

    @Override
    public void onPatientLongClick(Patient patient) {
        // Optional
    }

    private void showReferralDialog(Patient patient) {
        if (getContext() == null || dataManager == null) return;
        
        // Get list of doctors
        List<User> doctors = dataManager.getDoctors();
        if (doctors == null || doctors.isEmpty()) {
            Toast.makeText(getContext(), "No doctors available. Please register a doctor first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String[] doctorNames = new String[doctors.size()];
        for (int i = 0; i < doctors.size(); i++) {
            doctorNames[i] = doctors.get(i).getName();
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Refer Patient - " + patient.getName())
                .setMessage("Select a doctor to refer this patient to:")
                .setItems(doctorNames, (dialog, which) -> {
                    User selectedDoctor = doctors.get(which);
                    confirmReferral(patient, selectedDoctor);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReferral(Patient patient, User doctor) {
        if (getContext() == null || patient == null || doctor == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Referral")
                .setMessage("Refer " + patient.getName() + " to Dr. " + doctor.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {
                        // Set referral data
                        patient.setReferred(true);
                        patient.setReferredTo(doctor.getId());
                        if (currentUser != null) {
                            patient.setReferredBy(currentUser.getId());
                        }
                        patient.setReferralDate(new java.util.Date().toString());
                        patient.setReferralStatus("pending");
                        
                        // Save to DataManager
                        if (dataManager != null) {
                            boolean success = dataManager.updatePatient(patient);
                            
                            if (success) {
                                Toast.makeText(getContext(), 
                                    "Patient referred to Dr. " + doctor.getName(), 
                                    Toast.LENGTH_SHORT).show();
                                
                                // Refresh list
                                loadPatients();
                            } else {
                                Toast.makeText(getContext(), "Failed to save referral", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving referral", e);
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment resumes
        loadPatients();
    }

    public void refreshData() {
        loadPatients();
    }
}