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
    private List<Staff> displayList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_management, container, false);
        
        try {
            initializeViews(view);
            setupDataManager();
            setupRecyclerView();
            setupClickListeners();
            loadStaffData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initializeViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.recycler_staff);
            tvStaffCount = view.findViewById(R.id.tv_staff_count);
            tvNoData = view.findViewById(R.id.tv_no_data);
            btnAddStaff = view.findViewById(R.id.btn_add_staff);
            btnRefresh = view.findViewById(R.id.btn_refresh);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void setupDataManager() {
        try {
            if (getContext() != null) {
                dataManager = DataManager.getInstance(getContext());
                prefsManager = new SharedPrefsManager(getContext());
                currentUser = prefsManager.getCurrentUser();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up data manager", e);
        }
    }

    private void setupRecyclerView() {
        try {
            if (recyclerView != null && getContext() != null) {
                adapter = new StaffAdapter(displayList);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up recyclerView", e);
        }
    }

    private void setupClickListeners() {
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> loadStaffData());
        }
        
        if (btnAddStaff != null) {
            btnAddStaff.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Add staff feature coming soon!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadStaffData() {
        try {
            if (dataManager == null) {
                Log.e(TAG, "DataManager is null");
                return;
            }
            
            staffList.clear();
            
            // 1. Get all users and convert to staff format
            List<User> users = dataManager.getAllUsers();
            Log.d(TAG, "Found " + (users != null ? users.size() : 0) + " users");
            
            if (users != null) {
                for (User user : users) {
                    if (user != null) {
                        Staff staff = convertUserToStaff(user);
                        staffList.add(staff);
                    }
                }
            }
            
            // 2. Also include manually added Staff records (avoid duplicates)
            List<Staff> manualStaff = dataManager.getStaffList();
            Log.d(TAG, "Found " + (manualStaff != null ? manualStaff.size() : 0) + " manual staff records");
            
            if (manualStaff != null) {
                for (Staff s : manualStaff) {
                    if (s == null) continue;
                    
                    boolean exists = false;
                    for (Staff existing : staffList) {
                        if (existing != null && existing.getId() != null && 
                            existing.getId().equals(s.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        staffList.add(s);
                    }
                }
            }
            
            // Update display
            displayList.clear();
            displayList.addAll(staffList);
            
            // Update UI
            if (displayList.isEmpty()) {
                if (tvNoData != null) tvNoData.setVisibility(View.VISIBLE);
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                if (tvStaffCount != null) tvStaffCount.setText("0 staff members");
            } else {
                if (tvNoData != null) tvNoData.setVisibility(View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                if (tvStaffCount != null) tvStaffCount.setText(displayList.size() + " staff members");
                if (adapter != null) adapter.notifyDataSetChanged();
            }
            
            Log.d(TAG, "Total staff loaded: " + displayList.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading staff data", e);
            Toast.makeText(getContext(), "Error loading staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Staff convertUserToStaff(User user) {
        Staff staff = new Staff(
            user.getId() != null ? user.getId() : "",
            user.getName() != null ? user.getName() : "Unknown",
            user.getRole() != null ? user.getRole() : "unknown",
            user.getPhone() != null ? user.getPhone() : "",
            user.getVillage() != null ? user.getVillage() : "",
            user.getDistrict() != null ? user.getDistrict() : ""
        );
        
        staff.setBlock(user.getBlock());
        staff.setState(user.getState());
        staff.setPhcId(user.getPhcId());
        staff.setStatus("active");
        
        // Set role-specific fields
        if (user.getRole() != null) {
            switch (user.getRole()) {
                case "phcdoctor":
                    staff.setQualification("MBBS");
                    staff.setSpecialization("General Medicine");
                    break;
                case "phcnurse":
                    staff.setQualification("GNM");
                    break;
                case "asha":
                    staff.setAssignedPopulation("0");
                    staff.setAssignedFamilies("0");
                    break;
                case "phcadmin":
                    staff.setDesignation("Administrator");
                    break;
            }
        }
        
        return staff;
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