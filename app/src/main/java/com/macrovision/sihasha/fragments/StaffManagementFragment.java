package com.macrovision.sihasha.fragments;

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
import com.macrovision.sihasha.adapters.StaffAdapter;
import com.macrovision.sihasha.models.Staff;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class StaffManagementFragment extends Fragment {

    private static final String TAG = "StaffManagementFragment";
    
    private RecyclerView recyclerView;
    private StaffAdapter adapter;
    private TextView tvStaffCount, tvNoData;
    private Button btnAddStaff, btnRefresh;
    
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<Staff> staffList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_management, container, false);
        
        try {
            initializeViews(view);
            setupDataManager();
            setupRecyclerView();
            setupClickListeners();
            
            // Load staff data
            loadStaffData();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_staff);
        tvStaffCount = view.findViewById(R.id.tv_staff_count);
        tvNoData = view.findViewById(R.id.tv_no_data);
        btnAddStaff = view.findViewById(R.id.btn_add_staff);
        btnRefresh = view.findViewById(R.id.btn_refresh);
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
            adapter = new StaffAdapter(staffList);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupClickListeners() {
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                // Show loading
                if (tvNoData != null) {
                    tvNoData.setText("Refreshing staff list...");
                    tvNoData.setVisibility(View.VISIBLE);
                }
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                
                // Refresh from Firebase
                refreshFromFirebase();
            });
        }
        
        if (btnAddStaff != null) {
            btnAddStaff.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Add staff feature coming soon!", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void refreshFromFirebase() {
        if (dataManager == null) return;
        
        // Force refresh users from Firebase
        dataManager.refreshUsersFromFirebase();
        
        // Wait a moment for sync, then reload
        new android.os.Handler().postDelayed(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadStaffData();
                });
            }
        }, 1500);
    }

    private void loadStaffData() {
        try {
            if (dataManager == null) return;
            
            staffList.clear();
            
            // Get fresh staff list from DataManager
            List<Staff> freshStaff = dataManager.getStaffList();
            
            if (freshStaff != null && !freshStaff.isEmpty()) {
                staffList.addAll(freshStaff);
                Log.d(TAG, "Loaded " + staffList.size() + " staff members");
            } else {
                Log.d(TAG, "No staff members found");
            }
            
            // Update UI
            if (staffList.isEmpty()) {
                if (tvNoData != null) {
                    tvNoData.setText("No staff members found");
                    tvNoData.setVisibility(View.VISIBLE);
                }
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                if (tvStaffCount != null) tvStaffCount.setText("0 staff members");
            } else {
                if (tvNoData != null) tvNoData.setVisibility(View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                if (tvStaffCount != null) tvStaffCount.setText(staffList.size() + " staff members");
                if (adapter != null) adapter.notifyDataSetChanged();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading staff data", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStaffData();
    }

    public void refreshData() {
        loadStaffData();
    }
}