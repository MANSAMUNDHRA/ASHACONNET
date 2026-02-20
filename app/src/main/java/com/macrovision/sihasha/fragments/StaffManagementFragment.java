package com.macrovision.sihasha.fragments;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.adapters.StaffAdapter;
import com.macrovision.sihasha.models.Staff;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StaffManagementFragment extends Fragment implements StaffAdapter.OnStaffClickListener {

    private static final String TAG = "StaffManagementFragment";

    // UI Components
    private TextView tvTotalStaff, tvActiveStaff, tvOnLeave, tvPerformanceAvg;
    private EditText editSearchStaff;
    private Spinner spinnerRoleFilter, spinnerStatusFilter;
    private Button btnAddStaff, btnGenerateReport;
    private RecyclerView recyclerStaffList;
    private LinearLayout btnAllStaff, btnASHAWorkers, btnDoctors, btnNurses, btnAdmins;

    // Adapter and data
    private StaffAdapter staffAdapter;
    private List<Staff> allStaff;
    private List<Staff> filteredStaff;
    private String currentFilter = "all";

    // Data management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_staff_management, container, false);

        try {
            initializeComponents(view);
            setupRecyclerView();
            setupSpinners();
            setupEventListeners();
            setupFilterTabs();
            loadUserData();
            loadStaffData();

            Log.d(TAG, "Fragment setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
        }

        return view;
    }

    private void initializeComponents(View view) {
        Log.d(TAG, "Initializing components...");

        // Summary cards
        tvTotalStaff = view.findViewById(R.id.tv_total_staff);
        tvActiveStaff = view.findViewById(R.id.tv_active_staff);
        tvOnLeave = view.findViewById(R.id.tv_on_leave);
        tvPerformanceAvg = view.findViewById(R.id.tv_performance_avg);

        // Search and filters
        editSearchStaff = view.findViewById(R.id.edit_search_staff);
        spinnerRoleFilter = view.findViewById(R.id.spinner_role_filter);
        spinnerStatusFilter = view.findViewById(R.id.spinner_status_filter);

        // Action buttons
        btnAddStaff = view.findViewById(R.id.btn_add_staff);
        btnGenerateReport = view.findViewById(R.id.btn_generate_report);

        // Filter tabs
        btnAllStaff = view.findViewById(R.id.btn_all_staff);
        btnASHAWorkers = view.findViewById(R.id.btn_asha_workers);
        btnDoctors = view.findViewById(R.id.btn_doctors);
        btnNurses = view.findViewById(R.id.btn_nurses);
        btnAdmins = view.findViewById(R.id.btn_admins);

        // RecyclerView
        recyclerStaffList = view.findViewById(R.id.recycler_staff_list);

        // Data management
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());

        // Initialize lists
        allStaff = new ArrayList<>();
        filteredStaff = new ArrayList<>();

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView...");

        if (recyclerStaffList == null) {
            Log.e(TAG, "RecyclerView is null!");
            return;
        }

        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            recyclerStaffList.setLayoutManager(layoutManager);

            staffAdapter = new StaffAdapter(filteredStaff, this);
            recyclerStaffList.setAdapter(staffAdapter);

            Log.d(TAG, "RecyclerView setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");

        try {
            // Role filter spinner
            List<String> roles = Arrays.asList(
                    "All Roles", "ASHA Workers", "PHC Doctors", "PHC Nurses", "PHC Admins"
            );
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, roles
            );
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRoleFilter.setAdapter(roleAdapter);

            // Status filter spinner
            List<String> statuses = Arrays.asList(
                    "All Status", "Active", "On Leave", "Inactive"
            );
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, statuses
            );
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStatusFilter.setAdapter(statusAdapter);

            Log.d(TAG, "Spinners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    private void setupEventListeners() {
        Log.d(TAG, "Setting up event listeners...");

        try {
            // Add staff button
            if (btnAddStaff != null) {
                btnAddStaff.setOnClickListener(v -> showAddStaffDialog());
            }

            // Generate report button
            if (btnGenerateReport != null) {
                btnGenerateReport.setOnClickListener(v -> generateStaffReport());
            }

            // Search functionality
            // ✅ CORRECT:
            if (editSearchStaff != null) {
                editSearchStaff.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // Not needed
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Apply filters as user types
                        applyFilters();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // Not needed
                    }
                });
            }


            // Spinner listeners
            spinnerRoleFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    applyFilters();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    applyFilters();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Log.d(TAG, "Event listeners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up event listeners", e);
        }
    }

    private void setupFilterTabs() {
        Log.d(TAG, "Setting up filter tabs...");

        try {
            // Set click listeners for filter tabs
            btnAllStaff.setOnClickListener(v -> selectFilterTab("all"));
            btnASHAWorkers.setOnClickListener(v -> selectFilterTab("asha"));
            btnDoctors.setOnClickListener(v -> selectFilterTab("phcdoctor"));
            btnNurses.setOnClickListener(v -> selectFilterTab("phcnurse"));
            btnAdmins.setOnClickListener(v -> selectFilterTab("phcadmin"));

            // Set initial selection
            selectFilterTab("all");

            Log.d(TAG, "Filter tabs setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter tabs", e);
        }
    }

    private void selectFilterTab(String filter) {
        // Update current filter
        currentFilter = filter;

        // Update tab appearance
        resetFilterTabsAppearance();

        switch (filter) {
            case "all":
                btnAllStaff.setSelected(true);
                break;
            case "asha":
                btnASHAWorkers.setSelected(true);
                break;
            case "phcdoctor":
                btnDoctors.setSelected(true);
                break;
            case "phcnurse":
                btnNurses.setSelected(true);
                break;
            case "phcadmin":
                btnAdmins.setSelected(true);
                break;
        }

        // Apply filters
        applyFilters();
    }

    private void resetFilterTabsAppearance() {
        btnAllStaff.setSelected(false);
        btnASHAWorkers.setSelected(false);
        btnDoctors.setSelected(false);
        btnNurses.setSelected(false);
        btnAdmins.setSelected(false);
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

            // Verify user has staff management access
            if (!"phcadmin".equals(currentUser.getRole())) {
                Log.w(TAG, "User does not have staff management access");
                Toast.makeText(requireContext(), "Access denied - Admin only", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadStaffData() {
        Log.d(TAG, "Loading staff data...");

        try {
            allStaff = dataManager.getStaffList();

            if (allStaff != null) {
                Log.d(TAG, "Staff data loaded: " + allStaff.size() + " staff members");
                updateSummaryCards();
                applyFilters();
            } else {
                Log.w(TAG, "Staff data is null, initializing demo data");
                allStaff = new ArrayList<>();
                updateSummaryCards();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading staff data", e);
            Toast.makeText(requireContext(), "Error loading staff data", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSummaryCards() {
        if (allStaff == null) return;

        int totalStaff = allStaff.size();
        int activeStaff = 0;
        int onLeave = 0;
        double performanceSum = 0;
        int performanceCount = 0;

        for (Staff staff : allStaff) {
            if ("active".equals(staff.getStatus())) {
                activeStaff++;
            } else if ("on_leave".equals(staff.getStatus())) {
                onLeave++;
            }

            if (staff.getPerformance() != null) {
                performanceSum += staff.getPerformance().getEfficiency();
                performanceCount++;
            }
        }

        // Update summary cards
        tvTotalStaff.setText(String.valueOf(totalStaff));
        tvActiveStaff.setText(String.valueOf(activeStaff));
        tvOnLeave.setText(String.valueOf(onLeave));

        if (performanceCount > 0) {
            double avgPerformance = performanceSum / performanceCount;
            tvPerformanceAvg.setText(String.format("%.1f%%", avgPerformance));
        } else {
            tvPerformanceAvg.setText("N/A");
        }
    }

    private void applyFilters() {
        if (allStaff == null) return;

        filteredStaff.clear();
        String searchQuery = editSearchStaff.getText().toString().toLowerCase().trim();

        for (Staff staff : allStaff) {
            boolean matchesRole = filterByRole(staff);
            boolean matchesStatus = filterByStatus(staff);
            boolean matchesSearch = filterBySearch(staff, searchQuery);
            boolean matchesTab = filterByTab(staff);

            if (matchesRole && matchesStatus && matchesSearch && matchesTab) {
                filteredStaff.add(staff);
            }
        }

        // Update adapter
        if (staffAdapter != null) {
            staffAdapter.notifyDataSetChanged();
        }

        Log.d(TAG, "Filters applied: " + filteredStaff.size() + " staff members shown");
    }

    private boolean filterByRole(Staff staff) {
        int selectedRole = spinnerRoleFilter.getSelectedItemPosition();
        if (selectedRole == 0) return true; // All Roles

        String[] roles = {"", "asha", "phcdoctor", "phcnurse", "phcadmin"};
        return roles[selectedRole].equals(staff.getRole());
    }

    private boolean filterByStatus(Staff staff) {
        int selectedStatus = spinnerStatusFilter.getSelectedItemPosition();
        if (selectedStatus == 0) return true; // All Status

        String[] statuses = {"", "active", "on_leave", "inactive"};
        return statuses[selectedStatus].equals(staff.getStatus());
    }

    private boolean filterBySearch(Staff staff, String searchQuery) {
        if (searchQuery.isEmpty()) return true;

        return staff.getName().toLowerCase().contains(searchQuery) ||
                staff.getPhone().contains(searchQuery) ||
                (staff.getVillage() != null && staff.getVillage().toLowerCase().contains(searchQuery)) ||
                (staff.getPhcName() != null && staff.getPhcName().toLowerCase().contains(searchQuery));
    }

    private boolean filterByTab(Staff staff) {
        if ("all".equals(currentFilter)) return true;
        return currentFilter.equals(staff.getRole());
    }

    // StaffAdapter.OnStaffClickListener implementation
    @Override
    public void onStaffClick(Staff staff) {
        Log.d(TAG, "Staff clicked: " + staff.getName());
        showStaffDetailDialog(staff);
    }

    @Override
    public void onStaffLongClick(Staff staff) {
        Log.d(TAG, "Staff long clicked: " + staff.getName());
        showStaffManagementDialog(staff);
    }

    private void showStaffDetailDialog(Staff staff) {
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(staff.getName()).append("\n");
        details.append("Role: ").append(getRoleDisplayName(staff.getRole())).append("\n");
        details.append("Phone: ").append(staff.getPhone()).append("\n");

        if (staff.getEmail() != null) {
            details.append("Email: ").append(staff.getEmail()).append("\n");
        }

        if (staff.getVillage() != null) {
            details.append("Location: ").append(staff.getVillage());
            if (staff.getDistrict() != null) {
                details.append(", ").append(staff.getDistrict());
            }
            details.append("\n");
        }

        if (staff.getPhcName() != null) {
            details.append("PHC: ").append(staff.getPhcName()).append("\n");
        }

        if (staff.getQualification() != null) {
            details.append("Qualification: ").append(staff.getQualification()).append("\n");
        }

        if (staff.getExperience() != null) {
            details.append("Experience: ").append(staff.getExperience()).append("\n");
        }

        if (staff.getPerformance() != null) {
            Staff.Performance perf = staff.getPerformance();
            details.append("\nPerformance:\n");
            details.append("Efficiency: ").append(String.format("%.1f%%", perf.getEfficiency())).append("\n");
            details.append("Monthly Target: ").append(perf.getMonthlyTarget()).append("\n");
            details.append("Achieved: ").append(perf.getAchieved()).append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Staff Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showStaffManagementDialog(Staff staff) {
        String[] options = {"Edit Details", "View Performance", "Assign Training", "Change Status", "Contact Info"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Manage: " + staff.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit Details
                            showEditStaffDialog(staff);
                            break;
                        case 1: // View Performance
                            showPerformanceDialog(staff);
                            break;
                        case 2: // Assign Training
                            showAssignTrainingDialog(staff);
                            break;
                        case 3: // Change Status
                            showChangeStatusDialog(staff);
                            break;
                        case 4: // Contact Info
                            showContactInfoDialog(staff);
                            break;
                    }
                })
                .show();
    }

    private void showAddStaffDialog() {
        // Create add staff dialog with form fields
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_staff, null);

        EditText editName = dialogView.findViewById(R.id.edit_staff_name);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinner_staff_role);
        EditText editPhone = dialogView.findViewById(R.id.edit_staff_phone);
        EditText editEmail = dialogView.findViewById(R.id.edit_staff_email);
        EditText editVillage = dialogView.findViewById(R.id.edit_staff_village);
        EditText editQualification = dialogView.findViewById(R.id.edit_staff_qualification);

        // Setup role spinner
        List<String> roles = Arrays.asList("ASHA Worker", "PHC Doctor", "PHC Nurse", "PHC Admin");
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        builder.setView(dialogView)
                .setTitle("Add New Staff Member")
                .setPositiveButton("Add", (dialog, which) -> {
                    // Validate and add staff
                    String name = editName.getText().toString().trim();
                    String phone = editPhone.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] roleValues = {"asha", "phcdoctor", "phcnurse", "phcadmin"};
                    String role = roleValues[spinnerRole.getSelectedItemPosition()];

                    Staff newStaff = new Staff();
                    newStaff.setId("STAFF" + System.currentTimeMillis());
                    newStaff.setName(name);
                    newStaff.setRole(role);
                    newStaff.setPhone(phone);
                    newStaff.setEmail(editEmail.getText().toString().trim());
                    newStaff.setVillage(editVillage.getText().toString().trim());
                    newStaff.setQualification(editQualification.getText().toString().trim());
                    newStaff.setStatus("active");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        newStaff.setJoiningDate(LocalDate.now().toString());
                    }

                    // Add to list and update UI
                    allStaff.add(newStaff);
                    dataManager.addStaffMember(newStaff);
                    updateSummaryCards();
                    applyFilters();

                    Toast.makeText(requireContext(), "Staff member added successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateStaffReport() {
        StringBuilder report = new StringBuilder();
        report.append("STAFF MANAGEMENT REPORT\n\n");

        report.append("SUMMARY:\n");
        report.append("Total Staff: ").append(allStaff.size()).append("\n");

        // Count by role
        int ashaCount = 0, doctorCount = 0, nurseCount = 0, adminCount = 0;
        for (Staff staff : allStaff) {
            switch (staff.getRole()) {
                case "asha": ashaCount++; break;
                case "phcdoctor": doctorCount++; break;
                case "phcnurse": nurseCount++; break;
                case "phcadmin": adminCount++; break;
            }
        }

        report.append("ASHA Workers: ").append(ashaCount).append("\n");
        report.append("PHC Doctors: ").append(doctorCount).append("\n");
        report.append("PHC Nurses: ").append(nurseCount).append("\n");
        report.append("PHC Admins: ").append(adminCount).append("\n\n");

        report.append("PERFORMANCE OVERVIEW:\n");
        for (Staff staff : allStaff) {
            if (staff.getPerformance() != null) {
                report.append("• ").append(staff.getName()).append(" (").append(getRoleDisplayName(staff.getRole())).append("): ");
                report.append(String.format("%.1f%% efficiency", staff.getPerformance().getEfficiency())).append("\n");
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Staff Report")
                .setMessage(report.toString())
                .setPositiveButton("Export", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Report export feature coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private String getRoleDisplayName(String role) {
        switch (role) {
            case "asha": return "ASHA Worker";
            case "phcdoctor": return "PHC Doctor";
            case "phcnurse": return "PHC Nurse";
            case "phcadmin": return "PHC Administrator";
            default: return role;
        }
    }

    // Additional dialog methods...
    private void showEditStaffDialog(Staff staff) {
        Toast.makeText(requireContext(), "Edit staff functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showPerformanceDialog(Staff staff) {
        if (staff.getPerformance() == null) {
            Toast.makeText(requireContext(), "No performance data available", Toast.LENGTH_SHORT).show();
            return;
        }

        Staff.Performance perf = staff.getPerformance();
        String perfDetails = "Performance Details for " + staff.getName() + "\n\n" +
                "Monthly Target: " + perf.getMonthlyTarget() + "\n" +
                "Achieved: " + perf.getAchieved() + "\n" +
                "Efficiency: " + String.format("%.1f%%", perf.getEfficiency()) + "\n" +
                "Patients Handled: " + perf.getPatientsHandled() + "\n" +
                "Trainings Completed: " + perf.getTrainingsCompleted();

        new AlertDialog.Builder(requireContext())
                .setTitle("Performance Report")
                .setMessage(perfDetails)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAssignTrainingDialog(Staff staff) {
        Toast.makeText(requireContext(), "Training assignment coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showChangeStatusDialog(Staff staff) {
        String[] statuses = {"Active", "On Leave", "Inactive"};
        String[] statusValues = {"active", "on_leave", "inactive"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Status: " + staff.getName())
                .setItems(statuses, (dialog, which) -> {
                    staff.setStatus(statusValues[which]);
                    dataManager.updateStaffMember(staff);
                    updateSummaryCards();
                    applyFilters();
                    Toast.makeText(requireContext(), "Status updated to " + statuses[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showContactInfoDialog(Staff staff) {
        String contact = "Contact Information\n\n" +
                "Name: " + staff.getName() + "\n" +
                "Phone: " + staff.getPhone() + "\n";

        if (staff.getEmail() != null && !staff.getEmail().isEmpty()) {
            contact += "Email: " + staff.getEmail() + "\n";
        }

        if (staff.getVillage() != null) {
            contact += "Location: " + staff.getVillage();
            if (staff.getDistrict() != null) {
                contact += ", " + staff.getDistrict();
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Contact Information")
                .setMessage(contact)
                .setPositiveButton("Call", (dialog, which) -> {
                    // Implement call functionality
                    Toast.makeText(requireContext(), "Calling " + staff.getPhone(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // Public methods for external access
    public void refreshStaffData() {
        loadStaffData();
    }
}
