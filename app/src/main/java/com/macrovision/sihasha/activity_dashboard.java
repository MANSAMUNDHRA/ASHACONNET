package com.macrovision.sihasha;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.macrovision.sihasha.activities.AddPatientActivity;
import com.macrovision.sihasha.fragments.FinancialManagementFragment;
import com.macrovision.sihasha.fragments.InventoryFragment;
import com.macrovision.sihasha.fragments.PatientListFragment;
import com.macrovision.sihasha.fragments.StaffManagementFragment;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

public class activity_dashboard extends AppCompatActivity {

    private static final String TAG = "activity_dashboard";

    // UI Components from your existing layout
    private TextView tvUserName, tvUserRole, tvPatientCount;
    private TextView tvWelcome, tvUserInfo;
    private Button btnLogout;
    private LinearLayout navigationTabs;
    private GridLayout statsGrid;
    private ScrollView dashboardView;
    private FrameLayout contentFrame;


    // Navigation state
    private String currentView = "dashboard";
    private Button currentSelectedTab = null;

    // Data Management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_dashboard);
        handleSystemUIInsets();


        Log.d(TAG, "DashboardActivity started");

        initializeComponents();
        loadUserData();
        setupNavigationTabs();
        setupStatsGrid();
        loadDashboardData();
    }
    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            // Android 10 and below
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }
    private void handleSystemUIInsets() {
        View rootLayout = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets displayCutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());

            // Calculate the top margin needed (status bar + notch)
            int topMargin = Math.max(systemBarsInsets.top, displayCutoutInsets.top);

            // Apply margin to your main container
            View mainContainer = findViewById(R.id.content_frame); // Your root LinearLayout
            if (mainContainer != null) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) mainContainer.getLayoutParams();
                params.topMargin = topMargin;
                params.leftMargin = Math.max(systemBarsInsets.left, displayCutoutInsets.left);
                params.rightMargin = Math.max(systemBarsInsets.right, displayCutoutInsets.right);
                mainContainer.setLayoutParams(params);
            }

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initializeComponents() {
        Log.d(TAG, "Initializing components...");

        // Header components
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserRole = findViewById(R.id.tv_user_role);
        tvPatientCount = findViewById(R.id.tv_patient_count);
        btnLogout = findViewById(R.id.btn_logout);

        // Welcome section
        tvWelcome = findViewById(R.id.tv_welcome);
        tvUserInfo = findViewById(R.id.tv_user_info);

        // Main containers
        navigationTabs = findViewById(R.id.navigation_tabs);
        statsGrid = findViewById(R.id.stats_grid);
        dashboardView = findViewById(R.id.dashboard_view);
        contentFrame = findViewById(R.id.content_frame);

        // Data management
        dataManager = DataManager.getInstance(this);
        prefsManager = new SharedPrefsManager(this);

        // Setup logout button
        btnLogout.setOnClickListener(v -> logout());

        Log.d(TAG, "Components initialized successfully");
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

            // Update header
            tvUserName.setText("Welcome, " + currentUser.getName());
            tvUserRole.setText(getRoleDisplayName(currentUser.getRole()));

            // Update welcome section
            tvWelcome.setText("Hello, " + currentUser.getName() + "!");

            String locationInfo = "";
            if (currentUser.getVillage() != null && !currentUser.getVillage().isEmpty()) {
                locationInfo = currentUser.getVillage();
                if (currentUser.getBlock() != null && !currentUser.getBlock().isEmpty()) {
                    locationInfo += ", " + currentUser.getBlock();
                }
                if (currentUser.getDistrict() != null && !currentUser.getDistrict().isEmpty()) {
                    locationInfo += ", " + currentUser.getDistrict();
                }
            } else {
                locationInfo = getRoleDisplayName(currentUser.getRole()) + " Dashboard";
            }
            tvUserInfo.setText(locationInfo);

        } else {
            Log.w(TAG, "Current user is null - redirecting to login");
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private void setupNavigationTabs() {
        Log.d(TAG, "Setting up navigation tabs...");

        // Clear existing tabs
        navigationTabs.removeAllViews();

        // Define tabs based on user role
        String[] tabNames;
        String[] tabKeys;

        if ("asha".equals(currentUser.getRole())) {
            // Removed "Visits" from ASHA tabs - now only Dashboard, My Patients, Inventory
            tabNames = new String[]{"Dashboard", "My Patients", "Inventory"};
            tabKeys = new String[]{"dashboard", "patients", "inventory"};
        } else if ("phcadmin".equals(currentUser.getRole())) {
            tabNames = new String[]{"Dashboard", "All Patients", "Staff", "Inventory", "Financial", "Reports", "Training"};
            tabKeys = new String[]{"dashboard", "patients", "staff", "inventory", "financial", "reports", "training"};

        } else if ("phcdoctor".equals(currentUser.getRole())) {
            // Removed "Referrals" from Doctor tabs
            tabNames = new String[]{"Dashboard", "Patients", "Inventory", "Reports"};
            tabKeys = new String[]{"dashboard", "patients", "inventory", "reports"};
        } else if ("phcnurse".equals(currentUser.getRole())) {
            // Removed "Visits" and "Immunization" from Nurse tabs - now only Dashboard, Patients, Inventory
            tabNames = new String[]{"Dashboard", "Patients", "Inventory"};
            tabKeys = new String[]{"dashboard", "patients", "inventory"};
        } else {
            tabNames = new String[]{"Dashboard", "Patients", "Inventory"};
            tabKeys = new String[]{"dashboard", "patients", "inventory"};
        }

        // Create tab buttons dynamically
        for (int i = 0; i < tabNames.length; i++) {
            Button tabButton = createTabButton(tabNames[i], tabKeys[i]);
            navigationTabs.addView(tabButton);

            // Set first tab as selected
            if (i == 0) {
                setSelectedTab(tabButton);
            }
        }

        Log.d(TAG, "Navigation tabs setup completed with " + tabNames.length + " tabs");
    }

    private Button createTabButton(String tabName, String tabKey) {
        Button button = new Button(this);

        // Set button properties
        button.setText(tabName);
        button.setTag(tabKey);
        button.setTextSize(12);
        button.setPadding(32, 16, 32, 16);

        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

        // Set initial appearance
        button.setBackgroundResource(R.drawable.tab_button_selector);
        button.setTextColor(getColor(R.color.color_text_secondary));

        // Set click listener
        button.setOnClickListener(v -> {
            String viewKey = (String) v.getTag();
            handleNavigation(viewKey);
            setSelectedTab(button);
        });

        return button;
    }

    private void setSelectedTab(Button selectedButton) {
        // Reset all tab button states
        for (int i = 0; i < navigationTabs.getChildCount(); i++) {
            View child = navigationTabs.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setSelected(false);
                button.setTextColor(getColor(R.color.color_text_secondary));
            }
        }

        // Set selected tab state
        if (selectedButton != null) {
            selectedButton.setSelected(true);
            selectedButton.setTextColor(getColor(R.color.color_healthcare_primary));
            currentSelectedTab = selectedButton;
        }
    }

    private void setupStatsGrid() {
        Log.d(TAG, "Setting up stats grid...");

        // Clear existing stats
        statsGrid.removeAllViews();

        // Define stats based on user role
        if ("asha".equals(currentUser.getRole())) {
            // Changed: Removed "Pregnant Women" and "This Month Visits", added "Patients" and "High Risk"
            addStatsCard("Patients", "0", R.color.color_healthcare_primary, "patients");
            addStatsCard("High Risk", "0", R.color.color_error, "highrisk");
        } else if ("phcadmin".equals(currentUser.getRole())) {
            addStatsCard("Total Patients", "0", R.color.color_healthcare_primary, "patients");
            addStatsCard("ASHA Workers", "0", R.color.color_success, "asha");
            addStatsCard("Inventory Items", "0", R.color.color_warning, "inventory");
            addStatsCard("Low Stock Items", "0", R.color.color_error, "lowstock");
            addStatsCard("Pending Referrals", "0", R.color.color_healthcare_primary, "referrals");
            addStatsCard("Training Sessions", "0", R.color.color_success, "training");
        } else {
            addStatsCard("Patients", "0", R.color.color_healthcare_primary, "patients");
            addStatsCard("Inventory", "0", R.color.color_success, "inventory");
            addStatsCard("Reports", "0", R.color.color_warning, "reports");
            // Removed "Visits" card for other roles
        }

        Log.d(TAG, "Stats grid setup completed");
    }

    private void addStatsCard(String title, String value, int colorRes, String action) {
        // Create card view
        CardView cardView = new CardView(this);

        // Set card properties
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.setMargins(8, 8, 8, 8);
        cardView.setLayoutParams(cardParams);

        cardView.setRadius(8);
        cardView.setCardElevation(4);
        cardView.setClickable(true);
        cardView.setFocusable(true);

        // Create inner linear layout
        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(24, 24, 24, 24);
        innerLayout.setGravity(android.view.Gravity.CENTER);

        // Create value text view
        TextView valueTextView = new TextView(this);
        valueTextView.setText(value);
        valueTextView.setTextSize(24);
        valueTextView.setTextColor(getColor(colorRes));
        valueTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        valueTextView.setGravity(android.view.Gravity.CENTER);

        // Create title text view
        TextView titleTextView = new TextView(this);
        titleTextView.setText(title);
        titleTextView.setTextSize(12);
        titleTextView.setTextColor(getColor(R.color.color_text_secondary));
        titleTextView.setGravity(android.view.Gravity.CENTER);

        // Add views to layout
        innerLayout.addView(valueTextView);
        innerLayout.addView(titleTextView);
        cardView.addView(innerLayout);

        // Set click listener
        cardView.setOnClickListener(v -> handleStatsCardClick(action));

        // Add to grid
        statsGrid.addView(cardView);

        // Store reference for updating values
        valueTextView.setTag("value_" + action);
    }

    private void handleStatsCardClick(String action) {
        Log.d(TAG, "Stats card clicked: " + action);

        switch (action) {
            case "patients":
            case "highrisk":
                handleNavigation("patients");
                break;
            case "inventory":
            case "lowstock":
                handleNavigation("inventory");
                break;
            case "asha":
                handleNavigation("staff");
                break;
            default:
                Toast.makeText(this, action.toUpperCase() + " feature coming soon!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void handleNavigation(String viewKey) {
        Log.d(TAG, "Navigation clicked: " + viewKey);

        currentView = viewKey;

        switch (viewKey) {
            case "dashboard":
                showDashboard();
                break;
            case "patients":
                showPatientListFragment();
                break;
            case "inventory":
                if (hasInventoryAccess()) {
                    showInventoryFragment();
                } else {
                    Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case "staff":
                showStaffFragment();
                break;
            case "financial":
                if ("phcadmin".equals(currentUser.getRole())) {
                    showFinancialFragment();
                } else {
                    Toast.makeText(this, "Access denied - Admin only", Toast.LENGTH_SHORT).show();
                }
                break;
            case "reports":
                showReportsFragment();
                break;
            case "training":
                showTrainingFragment();
                break;
            default:
                Toast.makeText(this, viewKey.toUpperCase() + " feature coming soon!", Toast.LENGTH_SHORT).show();
                showDashboard();
                break;
        }
    }

    // ===== VIEW MANAGEMENT METHODS =====

    private void showDashboard() {
        Log.d(TAG, "Showing dashboard");

        // Show dashboard and hide fragments
        dashboardView.setVisibility(View.VISIBLE);
        clearFragments();

        // Refresh dashboard data
        loadDashboardData();

        // Update tab selection
        updateTabSelection("dashboard");
    }

    private void showPatientListFragment() {
        Log.d(TAG, "Showing patient list fragment");

        // Hide dashboard and show fragment
        dashboardView.setVisibility(View.GONE);

        // Replace fragment
        PatientListFragment fragment = new PatientListFragment();
        replaceFragment(fragment);

        updateTabSelection("patients");
    }

    private void showInventoryFragment() {
        Log.d(TAG, "Showing inventory fragment");

        // Hide dashboard and show fragment
        dashboardView.setVisibility(View.GONE);

        // Replace fragment
        InventoryFragment fragment = new InventoryFragment();
        replaceFragment(fragment);

        updateTabSelection("inventory");
    }

    private void showStaffFragment() {
        dashboardView.setVisibility(View.GONE);
        Log.d(TAG, "Showing staff management fragment");

        try {
            // Hide dashboard view
            if (dashboardView != null) {
                dashboardView.setVisibility(View.GONE);
            }

            // Create and show staff management fragment
            StaffManagementFragment staffFragment = new StaffManagementFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, staffFragment);
            transaction.addToBackStack("staff");
            transaction.commit();

            Log.d(TAG, "Fragment replaced: StaffManagementFragment");

        } catch (Exception e) {
            Log.e(TAG, "Error showing staff management fragment", e);
            Toast.makeText(this, "Error loading staff management", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportsFragment() {
        dashboardView.setVisibility(View.GONE);
        Toast.makeText(this, "Reports feature coming soon!", Toast.LENGTH_SHORT).show();
        showDashboard();
    }

    private void showTrainingFragment() {
        dashboardView.setVisibility(View.GONE);
        Toast.makeText(this, "Training feature coming soon!", Toast.LENGTH_SHORT).show();
        showDashboard();
    }

    private void showFinancialFragment() {
        Log.d(TAG, "Showing financial management fragment");

        // Hide dashboard and show fragment
        dashboardView.setVisibility(View.GONE);

        // Replace fragment
        FinancialManagementFragment fragment = new FinancialManagementFragment();
        replaceFragment(fragment);

        updateTabSelection("financial");
    }

    // ===== FRAGMENT MANAGEMENT METHODS =====

    private void replaceFragment(Fragment fragment) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();

            Log.d(TAG, "Fragment replaced: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error replacing fragment", e);
        }
    }

    private void clearFragments() {
        try {
            // Clear the back stack
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Log.d(TAG, "Fragments cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing fragments", e);
        }
    }

    private void updateTabSelection(String viewKey) {
        // Find and select the correct tab
        for (int i = 0; i < navigationTabs.getChildCount(); i++) {
            View child = navigationTabs.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                if (viewKey.equals(button.getTag())) {
                    setSelectedTab(button);
                    break;
                }
            }
        }
    }

    // ===== DATA LOADING METHODS =====

    private void loadDashboardData() {
        try {
            Log.d(TAG, "Loading dashboard data...");

            // Update header patient count
            int patientCount = dataManager.getPatientCountForUser(currentUser);
            tvPatientCount.setText(String.valueOf(patientCount));

            // Update stats grid values
            if ("asha".equals(currentUser.getRole())) {
                // Updated: Now showing only Patients and High Risk
                updateStatsValue("patients", patientCount);
                updateStatsValue("highrisk", dataManager.getHighRiskPatientsForASHA(currentUser.getId()).size());
            } else if ("phcadmin".equals(currentUser.getRole())) {
                updateStatsValue("patients", dataManager.getAllPatients().size());
                updateStatsValue("asha", dataManager.getASHAWorkers().size());
                updateStatsValue("inventory", dataManager.getAllInventoryItems().size());
                updateStatsValue("lowstock", dataManager.getLowStockItems().size());
                updateStatsValue("referrals", 0); // Placeholder
                updateStatsValue("training", 0); // Placeholder
            } else {
                updateStatsValue("patients", patientCount);
                updateStatsValue("inventory", dataManager.getAllInventoryItems().size());
                updateStatsValue("reports", 0); // Placeholder
            }

            Log.d(TAG, "Dashboard data loaded successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error loading dashboard data", e);
        }
    }

    private void updateStatsValue(String action, int value) {
        TextView valueView = statsGrid.findViewWithTag("value_" + action);
        if (valueView != null) {
            valueView.setText(String.valueOf(value));
        }
    }

    // ===== UTILITY METHODS =====

    private String getRoleDisplayName(String role) {
        switch (role) {
            case "asha": return "ASHA Worker";
            case "phcdoctor": return "PHC Doctor";
            case "phcnurse": return "PHC Nurse";
            case "phcadmin": return "PHC Administrator";
            default: return "Healthcare Worker";
        }
    }

    private boolean hasInventoryAccess() {
        return currentUser != null && !"guest".equals(currentUser.getRole());
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    prefsManager.logout();
                    finish();
                    startActivity(new Intent(this, MainActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if ("dashboard".equals(currentView)) {
            super.onBackPressed();
        } else {
            handleNavigation("dashboard");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to dashboard
        if ("dashboard".equals(currentView)) {
            loadDashboardData();
        }
    }
}