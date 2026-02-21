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

import com.macrovision.sihasha.fragments.doctors.DoctorHighRiskCasesFragment;
import com.macrovision.sihasha.fragments.doctors.DoctorReferralManagementFragment;
import com.macrovision.sihasha.fragments.doctors.DoctorAShaSupervisionFragment;
import com.macrovision.sihasha.fragments.doctors.DoctorInventoryMonitoringFragment;
import com.macrovision.sihasha.fragments.doctors.DoctorHealthAnalyticsFragment;
import com.macrovision.sihasha.fragments.asha.AShaReferralFragment;
import com.macrovision.sihasha.fragments.FinancialManagementFragment;
import com.macrovision.sihasha.fragments.InventoryFragment;
import com.macrovision.sihasha.fragments.PatientListFragment;
import com.macrovision.sihasha.fragments.StaffManagementFragment;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

public class activity_dashboard extends AppCompatActivity {

    private static final String TAG = "activity_dashboard";

    // UI Components
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

    // Fragment references
    private PatientListFragment patientListFragment;
    private DoctorHighRiskCasesFragment highRiskFragment;
    private DoctorReferralManagementFragment referralFragment;
    private DoctorAShaSupervisionFragment ashaSupervisionFragment;
    private DoctorInventoryMonitoringFragment inventoryMonitorFragment;
    private DoctorHealthAnalyticsFragment analyticsFragment;

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
            getWindow().setDecorFitsSystemWindows(false);
        } else {
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

            int topMargin = Math.max(systemBarsInsets.top, displayCutoutInsets.top);

            View mainContainer = findViewById(R.id.content_frame);
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

        tvUserName = findViewById(R.id.tv_user_name);
        tvUserRole = findViewById(R.id.tv_user_role);
        tvPatientCount = findViewById(R.id.tv_patient_count);
        btnLogout = findViewById(R.id.btn_logout);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvUserInfo = findViewById(R.id.tv_user_info);
        navigationTabs = findViewById(R.id.navigation_tabs);
        statsGrid = findViewById(R.id.stats_grid);
        dashboardView = findViewById(R.id.dashboard_view);
        contentFrame = findViewById(R.id.content_frame);

        dataManager = DataManager.getInstance(this);
        prefsManager = new SharedPrefsManager(this);

        btnLogout.setOnClickListener(v -> logout());

        Log.d(TAG, "Components initialized successfully");
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

            tvUserName.setText("Welcome, " + currentUser.getName());
            tvUserRole.setText(getRoleDisplayName(currentUser.getRole()));

            String timeOfDay = getTimeOfDay();
            tvWelcome.setText("Good " + timeOfDay + ", " + currentUser.getName() + "!");

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

    private String getTimeOfDay() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Morning";
        else if (hour >= 12 && hour < 17) return "Afternoon";
        else if (hour >= 17 && hour < 21) return "Evening";
        else return "Night";
    }

    private void setupNavigationTabs() {
        // NULL CHECK
        if (currentUser == null) {
            Log.e(TAG, "Current user is null - redirecting to login");
            finish();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }
        
        Log.d(TAG, "Setting up navigation tabs for role: " + currentUser.getRole());
        navigationTabs.removeAllViews();

        String[] tabNames;
        String[] tabKeys;

        switch (currentUser.getRole()) {
            case "asha":
                // ✅ ASHA now has 4 tabs including Referrals
                tabNames = new String[]{"Dashboard", "My Patients", "Referrals", "Inventory"};
                tabKeys = new String[]{"dashboard", "patients", "referrals", "inventory"};
                break;

            case "phcdoctor":
                tabNames = new String[]{
                    "Dashboard",
                    "High Risk Cases",
                    "Referrals",
                    "ASHA Supervision",
                    "Inventory Monitor",
                    "Analytics"
                };
                tabKeys = new String[]{
                    "dashboard",
                    "high_risk",
                    "referrals",
                    "asha_supervision",
                    "inventory_monitor",
                    "analytics"
                };
                break;

            case "phcadmin":
                tabNames = new String[]{"Dashboard", "All Patients", "Staff", "Inventory", "Financial", "Reports"};
                tabKeys = new String[]{"dashboard", "patients", "staff", "inventory", "financial", "reports"};
                break;

            case "phcnurse":
                tabNames = new String[]{"Dashboard", "Patients", "Inventory"};
                tabKeys = new String[]{"dashboard", "patients", "inventory"};
                break;

            default:
                tabNames = new String[]{"Dashboard", "Patients", "Inventory"};
                tabKeys = new String[]{"dashboard", "patients", "inventory"};
                break;
        }

        for (int i = 0; i < tabNames.length; i++) {
            Button tabButton = createTabButton(tabNames[i], tabKeys[i]);
            navigationTabs.addView(tabButton);

            if (i == 0) {
                setSelectedTab(tabButton);
            }
        }

        Log.d(TAG, "Navigation tabs setup completed with " + tabNames.length + " tabs");
    }

    private Button createTabButton(String tabName, String tabKey) {
        Button button = new Button(this);
        button.setText(tabName);
        button.setTag(tabKey);
        button.setTextSize(12);
        button.setPadding(32, 16, 32, 16);
        button.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

        button.setBackgroundResource(R.drawable.tab_button_selector);
        button.setTextColor(getColor(R.color.color_text_secondary));

        button.setOnClickListener(v -> {
            String viewKey = (String) v.getTag();
            handleNavigation(viewKey);
            setSelectedTab(button);
        });

        return button;
    }

    private void setSelectedTab(Button selectedButton) {
        for (int i = 0; i < navigationTabs.getChildCount(); i++) {
            View child = navigationTabs.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setSelected(false);
                button.setTextColor(getColor(R.color.color_text_secondary));
            }
        }

        if (selectedButton != null) {
            selectedButton.setSelected(true);
            selectedButton.setTextColor(getColor(R.color.color_healthcare_primary));
            currentSelectedTab = selectedButton;
        }
    }

    private void setupStatsGrid() {
        Log.d(TAG, "Setting up stats grid for role: " + currentUser.getRole());
        statsGrid.removeAllViews();

        switch (currentUser.getRole()) {
            case "asha":
                addStatsCard("My Patients", "0", R.color.color_healthcare_primary, "patients");
                addStatsCard("High Risk", "0", R.color.color_error, "high_risk");
                break;

            case "phcdoctor":
                addStatsCard("High-Risk Cases", "0", R.color.color_error, "high_risk");
                addStatsCard("Pending Referrals", "0", R.color.color_warning, "referrals");
                addStatsCard("Low Stock Alerts", "0", R.color.color_error, "inventory_monitor");
                addStatsCard("ASHA Workers", "0", R.color.color_success, "asha_supervision");
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    statsGrid.setColumnCount(4);
                }
                break;

            case "phcadmin":
                addStatsCard("Total Patients", "0", R.color.color_healthcare_primary, "patients");
                addStatsCard("Staff", "0", R.color.color_success, "staff");
                addStatsCard("Inventory", "0", R.color.color_warning, "inventory");
                addStatsCard("Low Stock", "0", R.color.color_error, "inventory");
                break;

            default:
                addStatsCard("Patients", "0", R.color.color_healthcare_primary, "patients");
                addStatsCard("Inventory", "0", R.color.color_success, "inventory");
                break;
        }

        Log.d(TAG, "Stats grid setup completed");
    }

    private void addStatsCard(String title, String value, int colorRes, String action) {
        CardView cardView = new CardView(this);

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

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(24, 24, 24, 24);
        innerLayout.setGravity(android.view.Gravity.CENTER);

        TextView valueTextView = new TextView(this);
        valueTextView.setText(value);
        valueTextView.setTextSize(24);
        valueTextView.setTextColor(getColor(colorRes));
        valueTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        valueTextView.setGravity(android.view.Gravity.CENTER);

        TextView titleTextView = new TextView(this);
        titleTextView.setText(title);
        titleTextView.setTextSize(12);
        titleTextView.setTextColor(getColor(R.color.color_text_secondary));
        titleTextView.setGravity(android.view.Gravity.CENTER);

        innerLayout.addView(valueTextView);
        innerLayout.addView(titleTextView);
        cardView.addView(innerLayout);

        cardView.setOnClickListener(v -> handleStatsCardClick(action));

        statsGrid.addView(cardView);
        valueTextView.setTag("value_" + action);
    }

    private void handleStatsCardClick(String action) {
        Log.d(TAG, "Stats card clicked: " + action);
        handleNavigation(action);
    }

    private void handleNavigation(String viewKey) {
        Log.d(TAG, "Navigation clicked: " + viewKey);
        currentView = viewKey;

        switch (viewKey) {
            case "dashboard":
                showDashboard();
                break;

            // ASHA views
            case "patients":
                showPatientListFragment();
                break;
                
            // ✅ Handle referrals based on role
            case "referrals":
                if ("asha".equals(currentUser.getRole())) {
                    showAShaReferralFragment();
                } else {
                    showDoctorReferralFragment();
                }
                break;

            // Doctor views
            case "high_risk":
                showDoctorHighRiskFragment();
                break;
            case "asha_supervision":
                showDoctorAShaSupervisionFragment();
                break;
            case "inventory_monitor":
                showDoctorInventoryMonitorFragment();
                break;
            case "analytics":
                showDoctorAnalyticsFragment();
                break;

            // Common views
            case "inventory":
                showInventoryFragment();
                break;
            case "staff":
                showStaffFragment();
                break;
            case "financial":
                showFinancialFragment();
                break;
            case "reports":
                showReportsFragment();
                break;

            default:
                Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
                showDashboard();
                break;
        }
    }

    private void showDashboard() {
        Log.d(TAG, "Showing dashboard");
        dashboardView.setVisibility(View.VISIBLE);
        clearFragments();
        loadDashboardData();
        updateTabSelection("dashboard");
    }

    private void showPatientListFragment() {
        Log.d(TAG, "Showing patient list fragment");
        dashboardView.setVisibility(View.GONE);
        
        if (patientListFragment == null) {
            patientListFragment = new PatientListFragment();
        }
        
        replaceFragment(patientListFragment);
        updateTabSelection("patients");
    }

    private void showDoctorHighRiskFragment() {
        Log.d(TAG, "Showing doctor high risk fragment");
        dashboardView.setVisibility(View.GONE);
        
        if (highRiskFragment == null) {
            highRiskFragment = new DoctorHighRiskCasesFragment();
        }
        
        replaceFragment(highRiskFragment);
        updateTabSelection("high_risk");
    }

    private void showDoctorReferralFragment() {
        Log.d(TAG, "Showing doctor referral fragment");
        dashboardView.setVisibility(View.GONE);
        
        if (referralFragment == null) {
            referralFragment = new DoctorReferralManagementFragment();
        }
        
        replaceFragment(referralFragment);
        updateTabSelection("referrals");
    }

    private void showDoctorAShaSupervisionFragment() {
        Log.d(TAG, "Showing doctor ASHA supervision fragment");
        dashboardView.setVisibility(View.GONE);
        
        if (ashaSupervisionFragment == null) {
            ashaSupervisionFragment = new DoctorAShaSupervisionFragment();
        }
        
        replaceFragment(ashaSupervisionFragment);
        updateTabSelection("asha_supervision");
    }

    private void showDoctorInventoryMonitorFragment() {
        Log.d(TAG, "Showing doctor inventory monitor fragment");
        dashboardView.setVisibility(View.GONE);
        
        if (inventoryMonitorFragment == null) {
            inventoryMonitorFragment = new DoctorInventoryMonitoringFragment();
        }
        
        replaceFragment(inventoryMonitorFragment);
        updateTabSelection("inventory_monitor");
    }

    private void showDoctorAnalyticsFragment() {
        Log.d(TAG, "Showing doctor analytics fragment");
        dashboardView.setVisibility(View.GONE);
        
        if (analyticsFragment == null) {
            analyticsFragment = new DoctorHealthAnalyticsFragment();
        }
        
        replaceFragment(analyticsFragment);
        updateTabSelection("analytics");
    }

    // ✅ NEW: Show ASHA Referral Fragment
    private void showAShaReferralFragment() {
        Log.d(TAG, "Showing ASHA referral fragment");
        dashboardView.setVisibility(View.GONE);
        
        AShaReferralFragment fragment = new AShaReferralFragment();
        replaceFragment(fragment);
        updateTabSelection("referrals");
    }

    private void showInventoryFragment() {
        dashboardView.setVisibility(View.GONE);
        InventoryFragment fragment = new InventoryFragment();
        replaceFragment(fragment);
        updateTabSelection("inventory");
    }

    private void showStaffFragment() {
        dashboardView.setVisibility(View.GONE);
        StaffManagementFragment fragment = new StaffManagementFragment();
        replaceFragment(fragment);
        updateTabSelection("staff");
    }

    private void showFinancialFragment() {
        dashboardView.setVisibility(View.GONE);
        FinancialManagementFragment fragment = new FinancialManagementFragment();
        replaceFragment(fragment);
        updateTabSelection("financial");
    }

    private void showReportsFragment() {
        Log.d(TAG, "Showing reports fragment");
        dashboardView.setVisibility(View.GONE);
        Toast.makeText(this, "Reports feature coming soon!", Toast.LENGTH_SHORT).show();
        showDashboard();
    }

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
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Log.d(TAG, "Fragments cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing fragments", e);
        }
    }

    private void updateTabSelection(String viewKey) {
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

    private void loadDashboardData() {
        try {
            Log.d(TAG, "Loading dashboard data for role: " + currentUser.getRole());

            int patientCount = dataManager.getPatientCountForUser(currentUser);
            tvPatientCount.setText(String.valueOf(patientCount));

            switch (currentUser.getRole()) {
                case "asha":
                    updateStatsValue("patients", patientCount);
                    updateStatsValue("high_risk", 
                        dataManager.getHighRiskPatientsForASHA(currentUser.getId()).size());
                    break;

                case "phcdoctor":
                    int highRiskCount = dataManager.getHighRiskPatients().size();
                    updateStatsValue("high_risk", highRiskCount);
                    
                    int referralCount = dataManager.getPendingReferrals() != null ? 
                                        dataManager.getPendingReferrals().size() : 0;
                    updateStatsValue("referrals", referralCount);
                    
                    updateStatsValue("inventory_monitor", dataManager.getLowStockItems().size());
                    updateStatsValue("asha_supervision", dataManager.getASHAWorkers().size());
                    
                    tvPatientCount.setText(String.valueOf(highRiskCount));
                    tvPatientCount.setTextColor(getColor(R.color.color_error));
                    break;

                case "phcadmin":
                    updateStatsValue("patients", dataManager.getAllPatients().size());
                    updateStatsValue("staff", dataManager.getStaffList().size());
                    updateStatsValue("inventory", dataManager.getAllInventoryItems().size());
                    tvPatientCount.setText(String.valueOf(dataManager.getAllPatients().size()));
                    break;

                default:
                    updateStatsValue("patients", patientCount);
                    updateStatsValue("inventory", dataManager.getAllInventoryItems().size());
                    break;
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

    private String getRoleDisplayName(String role) {
        switch (role) {
            case "asha": return "ASHA Worker";
            case "phcdoctor": return "PHC Doctor";
            case "phcnurse": return "PHC Nurse";
            case "phcadmin": return "PHC Administrator";
            default: return "Healthcare Worker";
        }
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
        if ("dashboard".equals(currentView)) {
            loadDashboardData();
        }
    }
}