package com.macrovision.sihasha.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.adapters.BudgetCategoryAdapter;
import com.macrovision.sihasha.models.FinancialData;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinancialManagementFragment extends Fragment implements BudgetCategoryAdapter.OnBudgetCategoryClickListener {

    private static final String TAG = "FinancialFragment";

    // UI Components
    private TextView tvAnnualBudget, tvUtilizedAmount, tvRemainingAmount, tvUtilizationPercentage;
    private ProgressBar progressBudgetUtilization;

    private Button btnGenerateReport, btnManageBudget, btnExpenseTracker, btnBudgetAlerts, btnFinancialAudit;
    private RecyclerView recyclerBudgetCategories;

    // Adapter
    private BudgetCategoryAdapter budgetCategoryAdapter;
    private List<FinancialData.CategoryBudget> categoryBudgets;

    // Data Management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private FinancialData financialData;
    private NumberFormat currencyFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_financial_management, container, false);

        try {
            initializeComponents(view);
            setupRecyclerView();
            setupSpinners();
            setupEventListeners();
            loadUserData();
            loadFinancialData();

            Log.d(TAG, "Fragment setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
        }

        return view;
    }

    private void initializeComponents(View view) {
        Log.d(TAG, "Initializing components...");

        // Budget Overview Components
        tvAnnualBudget = view.findViewById(R.id.tv_annual_budget);
        tvUtilizedAmount = view.findViewById(R.id.tv_utilized_amount);
        tvRemainingAmount = view.findViewById(R.id.tv_remaining_amount);
        tvUtilizationPercentage = view.findViewById(R.id.tv_utilization_percentage);
        progressBudgetUtilization = view.findViewById(R.id.progress_budget_utilization);

        // Controls

        btnGenerateReport = view.findViewById(R.id.btn_generate_report);

        // Quick Action Buttons
        btnManageBudget = view.findViewById(R.id.btn_manage_budget);
        btnExpenseTracker = view.findViewById(R.id.btn_expense_tracker);
        btnBudgetAlerts = view.findViewById(R.id.btn_budget_alerts);
        btnFinancialAudit = view.findViewById(R.id.btn_financial_audit);

        // RecyclerView
        recyclerBudgetCategories = view.findViewById(R.id.recycler_budget_categories);

        // Data Management
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());

        // Initialize lists
        categoryBudgets = new ArrayList<>();

        // Currency formatter for Indian Rupees
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView...");

        if (recyclerBudgetCategories == null) {
            Log.e(TAG, "RecyclerView is null!");
            return;
        }

        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            recyclerBudgetCategories.setLayoutManager(layoutManager);

            budgetCategoryAdapter = new BudgetCategoryAdapter(categoryBudgets, this);
            recyclerBudgetCategories.setAdapter(budgetCategoryAdapter);

            Log.d(TAG, "RecyclerView setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");



        try {
            // Financial Period Spinner
            List<String> periods = Arrays.asList(
                    "Current Year (2024-25)", "Previous Year (2023-24)",
                    "Q1 2024-25", "Q2 2024-25", "Q3 2024-25", "Q4 2024-25"
            );
            ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    periods
            );
            periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            Log.d(TAG, "Spinners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    private void setupEventListeners() {
        Log.d(TAG, "Setting up event listeners...");

        try {
            // Generate Report button
            if (btnGenerateReport != null) {
                btnGenerateReport.setOnClickListener(v -> generateFinancialReport());
            }

            // Quick Action buttons
            if (btnManageBudget != null) {
                btnManageBudget.setOnClickListener(v -> showBudgetManagementDialog());
            }

            if (btnExpenseTracker != null) {
                btnExpenseTracker.setOnClickListener(v -> showExpenseTrackerDialog());
            }

            if (btnBudgetAlerts != null) {
                btnBudgetAlerts.setOnClickListener(v -> showBudgetAlertsDialog());
            }

            if (btnFinancialAudit != null) {
                btnFinancialAudit.setOnClickListener(v -> showFinancialAuditDialog());
            }

            Log.d(TAG, "Event listeners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up event listeners", e);
        }
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

            // Verify user has financial access
            if (!"phcadmin".equals(currentUser.getRole())) {
                Log.w(TAG, "User does not have financial access");
                Toast.makeText(requireContext(), "Access denied - Admin only", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "Current user is null");
        }
    }

    private void loadFinancialData() {

        Log.d(TAG, "Loading financial data...");

        try {
            financialData = dataManager.getFinancialData();

            if (financialData != null) {
                Log.d(TAG, "Financial data loaded successfully");
                updateBudgetOverview();
                updateCategoryBudgets();
            } else {
                Log.e(TAG, "Financial data is null");
                Toast.makeText(requireContext(), "Error loading financial data", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading financial data", e);
            Toast.makeText(requireContext(), "Error loading financial data", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBudgetOverview() {
        if (financialData == null || financialData.getBudgetOverview() == null) {
            Log.w(TAG, "Budget overview data is null");
            return;
        }

        try {
            FinancialData.BudgetOverview budget = financialData.getBudgetOverview();

            // Update budget amounts
            tvAnnualBudget.setText(formatCurrency(budget.getAnnualBudget()));
            tvUtilizedAmount.setText(formatCurrency(budget.getUtilized()));
            tvRemainingAmount.setText(formatCurrency(budget.getRemaining()));

            // Update utilization percentage
            int utilizationRate = (int) budget.getUtilizationRate();
            tvUtilizationPercentage.setText(utilizationRate + "% Utilized");
            progressBudgetUtilization.setProgress(utilizationRate);

            Log.d(TAG, "Budget overview updated - Utilization: " + utilizationRate + "%");

        } catch (Exception e) {
            Log.e(TAG, "Error updating budget overview", e);
        }
    }

    private void updateCategoryBudgets() {
        if (financialData == null || financialData.getCategoryBudgets() == null) {
            Log.w(TAG, "Category budgets data is null");
            return;
        }

        try {
            categoryBudgets.clear();

            Map<String, FinancialData.CategoryBudget> budgets = financialData.getCategoryBudgets();
            for (FinancialData.CategoryBudget category : budgets.values()) {
                categoryBudgets.add(category);
            }

            if (budgetCategoryAdapter != null) {
                Log.d(TAG, "Calling adapter.notifyDataSetChanged()");
                budgetCategoryAdapter.notifyDataSetChanged();
                Log.d(TAG, "Adapter item count after notify: " + budgetCategoryAdapter.getItemCount());
            }
            else {
                Log.e(TAG, "budgetCategoryAdapter is null!");
            }

            Log.d(TAG, "=== END CATEGORY BUDGETS UPDATE ===");


        } catch (Exception e) {
            Log.e(TAG, "Error updating category budgets", e);
        }
    }

    private String formatCurrency(double amount) {
        // Convert to Lakhs/Crores for better readability
        if (amount >= 10000000) { // 1 Crore
            return String.format(Locale.getDefault(), "₹%.2f Cr", amount / 10000000);
        } else if (amount >= 100000) { // 1 Lakh
            return String.format(Locale.getDefault(), "₹%.2f L", amount / 100000);
        } else {
            return String.format(Locale.getDefault(), "₹%.0f", amount);
        }
    }

    // Quick Action Methods
    private void generateFinancialReport() {
        Log.d(TAG, "Generating financial report...");



        StringBuilder reportContent = new StringBuilder();
        reportContent.append("FINANCIAL REPORT\n");


        if (financialData != null && financialData.getBudgetOverview() != null) {
            FinancialData.BudgetOverview budget = financialData.getBudgetOverview();
            reportContent.append("BUDGET OVERVIEW:\n");
            reportContent.append("Annual Budget: ").append(formatCurrency(budget.getAnnualBudget())).append("\n");
            reportContent.append("Utilized: ").append(formatCurrency(budget.getUtilized())).append("\n");
            reportContent.append("Remaining: ").append(formatCurrency(budget.getRemaining())).append("\n");
            reportContent.append("Utilization Rate: ").append(String.format("%.1f%%", budget.getUtilizationRate())).append("\n\n");

            reportContent.append("CATEGORY BREAKDOWN:\n");
            for (FinancialData.CategoryBudget category : categoryBudgets) {
                reportContent.append("• ").append(category.getCategoryName()).append(":\n");
                reportContent.append("  Allocated: ").append(formatCurrency(category.getAllocated())).append("\n");
                reportContent.append("  Spent: ").append(formatCurrency(category.getSpent())).append("\n");
                reportContent.append("  Remaining: ").append(formatCurrency(category.getRemaining())).append("\n\n");
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Financial Report ")
                .setMessage(reportContent.toString())
                .setPositiveButton("Export PDF", (dialog, which) -> {
                    Toast.makeText(requireContext(), "PDF export feature coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showBudgetManagementDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Budget Management")
                .setMessage("Budget management features:\n\n• Reallocate funds between categories\n• Set budget limits\n• Approve budget increases\n• Budget planning for next fiscal year")
                .setPositiveButton("Manage Budget", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Budget management coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showExpenseTrackerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Expense Tracker")
                .setMessage("Track and monitor expenses:\n\n• Daily expense logs\n• Monthly spending patterns\n• Expense approvals\n• Vendor payment tracking")
                .setPositiveButton("Open Tracker", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Expense tracker coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBudgetAlertsDialog() {
        StringBuilder alertsContent = new StringBuilder();
        alertsContent.append("BUDGET ALERTS:\n\n");

        boolean hasAlerts = false;
        for (FinancialData.CategoryBudget category : categoryBudgets) {
            if (category.getPercentage() > 80) {
                alertsContent.append("⚠️ ").append(category.getCategoryName()).append("\n");
                alertsContent.append("   ").append(String.format("%.1f%% utilized", category.getPercentage())).append("\n\n");
                hasAlerts = true;
            }
        }

        if (!hasAlerts) {
            alertsContent.append("✅ All budgets are within safe limits.\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Budget Alerts")
                .setMessage(alertsContent.toString())
                .setPositiveButton("Set Alert Preferences", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Alert preferences coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showFinancialAuditDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Financial Audit")
                .setMessage("Financial audit features:\n\n• Schedule internal audits\n• External audit preparation\n• Compliance checks\n• Financial documentation review\n\nLast Audit: March 2024\nNext Scheduled: December 2024")
                .setPositiveButton("Schedule Audit", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Audit scheduling coming soon!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // BudgetCategoryAdapter.OnBudgetCategoryClickListener implementation
    @Override
    public void onBudgetCategoryClick(FinancialData.CategoryBudget category) {
        Log.d(TAG, "Budget category clicked: " + category.getCategoryName());
        showCategoryDetailDialog(category);
    }

    @Override
    public void onBudgetCategoryLongClick(FinancialData.CategoryBudget category) {
        Log.d(TAG, "Budget category long clicked: " + category.getCategoryName());
        showCategoryManagementDialog(category);
    }

    private void showCategoryDetailDialog(FinancialData.CategoryBudget category) {
        String details = "Category: " + category.getCategoryName() + "\n\n" +
                "Allocated Amount: " + formatCurrency(category.getAllocated()) + "\n" +
                "Amount Spent: " + formatCurrency(category.getSpent()) + "\n" +
                "Remaining Amount: " + formatCurrency(category.getRemaining()) + "\n" +
                "Utilization: " + String.format("%.1f%%", category.getPercentage()) + "\n\n" +
                "Description: " + category.getDescription();

        new AlertDialog.Builder(requireContext())
                .setTitle("Budget Category Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showCategoryManagementDialog(FinancialData.CategoryBudget category) {
        String[] options = {"Update Spent Amount", "Reallocate Budget", "View Transaction History", "Export Category Report"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Manage: " + category.getCategoryName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Update Spent Amount
                            showUpdateSpentDialog(category);
                            break;
                        case 1: // Reallocate Budget
                            Toast.makeText(requireContext(), "Budget reallocation coming soon!", Toast.LENGTH_SHORT).show();
                            break;
                        case 2: // View Transaction History
                            Toast.makeText(requireContext(), "Transaction history coming soon!", Toast.LENGTH_SHORT).show();
                            break;
                        case 3: // Export Category Report
                            Toast.makeText(requireContext(), "Category report export coming soon!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showUpdateSpentDialog(FinancialData.CategoryBudget category) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount spent");
        input.setText(String.valueOf((int) category.getSpent()));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 40);

        TextView currentLabel = new TextView(requireContext());
        currentLabel.setText("Current Spent: " + formatCurrency(category.getSpent()));
        currentLabel.setTextSize(14);
        currentLabel.setPadding(0, 0, 0, 20);

        container.addView(currentLabel);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Update Spent Amount")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    try {
                        double newSpent = Double.parseDouble(input.getText().toString().trim());
                        if (newSpent >= 0 && newSpent <= category.getAllocated()) {
                            // Update the category budget
                            String categoryKey = getCategoryKey(category.getCategoryName());
                            boolean success = dataManager.updateBudgetCategory(categoryKey, newSpent);

                            if (success) {
                                Toast.makeText(requireContext(), "Spent amount updated successfully", Toast.LENGTH_SHORT).show();
                                loadFinancialData(); // Refresh the data
                            } else {
                                Toast.makeText(requireContext(), "Failed to update spent amount", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Amount must be between 0 and allocated budget", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getCategoryKey(String categoryName) {
        switch (categoryName) {
            case "Staff Salaries": return "staff";
            case "Medicines & Supplies": return "medicines";
            case "Medical Equipment": return "equipment";
            case "Infrastructure": return "infrastructure";
            case "Training & Development": return "training";
            default: return "other";
        }
    }

    // Public methods for external access
    public void refreshFinancialData() {
        loadFinancialData();
    }
}
