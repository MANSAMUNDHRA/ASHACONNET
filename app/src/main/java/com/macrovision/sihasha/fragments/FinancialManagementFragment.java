package com.macrovision.sihasha.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinancialManagementFragment extends Fragment
        implements BudgetCategoryAdapter.OnBudgetCategoryClickListener {

    private static final String TAG = "FinancialFragment";

    private TextView tvAnnualBudget, tvUtilizedAmount, tvRemainingAmount, tvUtilizationPercentage;
    private ProgressBar progressBudgetUtilization;
    private Button btnGenerateReport, btnManageBudget, btnExpenseTracker, btnBudgetAlerts, btnFinancialAudit;
    private RecyclerView recyclerBudgetCategories;

    private BudgetCategoryAdapter budgetCategoryAdapter;
    private List<FinancialData.CategoryBudget> categoryBudgets = new ArrayList<>();

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private FinancialData financialData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_financial_management, container, false);
        try {
            initializeComponents(view);
            setupRecyclerView();
            setupEventListeners();
            currentUser = prefsManager.getCurrentUser();
            loadFinancialData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
        }
        return view;
    }

    private void initializeComponents(View view) {
        tvAnnualBudget = view.findViewById(R.id.tv_annual_budget);
        tvUtilizedAmount = view.findViewById(R.id.tv_utilized_amount);
        tvRemainingAmount = view.findViewById(R.id.tv_remaining_amount);
        tvUtilizationPercentage = view.findViewById(R.id.tv_utilization_percentage);
        progressBudgetUtilization = view.findViewById(R.id.progress_budget_utilization);
        btnGenerateReport = view.findViewById(R.id.btn_generate_report);
        btnManageBudget = view.findViewById(R.id.btn_manage_budget);
        btnExpenseTracker = view.findViewById(R.id.btn_expense_tracker);
        btnBudgetAlerts = view.findViewById(R.id.btn_budget_alerts);
        btnFinancialAudit = view.findViewById(R.id.btn_financial_audit);
        recyclerBudgetCategories = view.findViewById(R.id.recycler_budget_categories);
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());
    }

    private void setupRecyclerView() {
        if (recyclerBudgetCategories == null) return;
        recyclerBudgetCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        budgetCategoryAdapter = new BudgetCategoryAdapter(categoryBudgets, this);
        recyclerBudgetCategories.setAdapter(budgetCategoryAdapter);
    }

    private void setupEventListeners() {
        if (btnGenerateReport != null)
            btnGenerateReport.setOnClickListener(v -> generateAndShareReport());
        if (btnManageBudget != null)
            btnManageBudget.setOnClickListener(v -> showSetBudgetDialog());
        if (btnExpenseTracker != null)
            btnExpenseTracker.setOnClickListener(v -> showUpdateExpenseDialog());
        if (btnBudgetAlerts != null)
            btnBudgetAlerts.setOnClickListener(v -> showBudgetAlertsDialog());
        if (btnFinancialAudit != null)
            btnFinancialAudit.setOnClickListener(v -> Toast.makeText(requireContext(),
                    "Financial audit coming soon", Toast.LENGTH_SHORT).show());
    }

    private void loadFinancialData() {
        try {
            financialData = dataManager.getFinancialData();

            // If no real data entered yet, show zeros — NOT fake numbers
            if (financialData == null) financialData = new FinancialData();

            if (financialData.getBudgetOverview() == null) {
                // First time — show all zeros, prompt admin to set budget
                showZeroState();
            } else {
                updateBudgetOverview();
            }

            updateCategoryBudgets();
        } catch (Exception e) {
            Log.e(TAG, "Error loading financial data", e);
            showZeroState();
        }
    }

    private void showZeroState() {
        if (tvAnnualBudget != null) tvAnnualBudget.setText("₹0");
        if (tvUtilizedAmount != null) tvUtilizedAmount.setText("₹0");
        if (tvRemainingAmount != null) tvRemainingAmount.setText("₹0");
        if (tvUtilizationPercentage != null) tvUtilizationPercentage.setText("0% Utilized");
        if (progressBudgetUtilization != null) progressBudgetUtilization.setProgress(0);
        Toast.makeText(requireContext(),
                "No budget set yet. Use 'Manage Budget' to set annual budget.",
                Toast.LENGTH_LONG).show();
    }

    private void updateBudgetOverview() {
        if (financialData == null || financialData.getBudgetOverview() == null) {
            showZeroState();
            return;
        }
        FinancialData.BudgetOverview budget = financialData.getBudgetOverview();
        if (tvAnnualBudget != null) tvAnnualBudget.setText(formatCurrency(budget.getAnnualBudget()));
        if (tvUtilizedAmount != null) tvUtilizedAmount.setText(formatCurrency(budget.getUtilized()));
        if (tvRemainingAmount != null) tvRemainingAmount.setText(formatCurrency(budget.getRemaining()));
        int rate = (int) budget.getUtilizationRate();
        if (tvUtilizationPercentage != null) tvUtilizationPercentage.setText(rate + "% Utilized");
        if (progressBudgetUtilization != null) progressBudgetUtilization.setProgress(rate);
    }

    private void updateCategoryBudgets() {
        categoryBudgets.clear();
        if (financialData != null && financialData.getCategoryBudgets() != null) {
            categoryBudgets.addAll(financialData.getCategoryBudgets().values());
        }
        if (budgetCategoryAdapter != null) budgetCategoryAdapter.notifyDataSetChanged();
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) return String.format(Locale.getDefault(), "₹%.2f Cr", amount / 10000000);
        else if (amount >= 100000) return String.format(Locale.getDefault(), "₹%.2f L", amount / 100000);
        else return String.format(Locale.getDefault(), "₹%.0f", amount);
    }

    // ─── SET BUDGET DIALOG ───────────────────────────────────────────────────
    private void showSetBudgetDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter annual budget (₹)");

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 10);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Set Annual Budget")
                .setMessage("Enter the total annual budget for this PHC:")
                .setView(container)
                .setPositiveButton("Set Budget", (d, w) -> {
                    try {
                        double budget = Double.parseDouble(input.getText().toString().trim());
                        if (budget <= 0) {
                            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setBudget(budget);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setBudget(double annualBudget) {
        if (financialData == null) financialData = new FinancialData();

        FinancialData.BudgetOverview overview = new FinancialData.BudgetOverview();
        overview.setAnnualBudget(annualBudget);
        overview.setUtilized(0);
        overview.setRemaining(annualBudget);
        overview.setUtilizationRate(0);
        financialData.setBudgetOverview(overview);

        dataManager.saveFinancialData(financialData);
        updateBudgetOverview();
        Toast.makeText(requireContext(), "Budget set to " + formatCurrency(annualBudget), Toast.LENGTH_SHORT).show();
    }

    // ─── UPDATE EXPENSE DIALOG ───────────────────────────────────────────────
    private void showUpdateExpenseDialog() {
        if (financialData == null || financialData.getBudgetOverview() == null) {
            Toast.makeText(requireContext(), "Set annual budget first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] categories = {"Staff Salaries", "Medicines & Supplies", "Medical Equipment",
                "Infrastructure", "Training & Development", "Other"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Record Expense")
                .setItems(categories, (d, which) -> showExpenseAmountDialog(categories[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showExpenseAmountDialog(String categoryName) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter expense amount (₹)");

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 10);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Expense: " + categoryName)
                .setView(container)
                .setPositiveButton("Record", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString().trim());
                        if (amount <= 0) {
                            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        recordExpense(categoryName, amount);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recordExpense(String categoryName, double amount) {
        if (financialData == null) return;

        // Update or create category budget
        java.util.HashMap<String, FinancialData.CategoryBudget> cats = new java.util.HashMap<>();
        if (financialData.getCategoryBudgets() != null)
            cats.putAll(financialData.getCategoryBudgets());

        String key = categoryName.toLowerCase().replace(" & ", "_").replace(" ", "_");
        FinancialData.CategoryBudget cat = cats.getOrDefault(key, new FinancialData.CategoryBudget());
        cat.setCategoryName(categoryName);
        cat.setSpent(cat.getSpent() + amount);
        cats.put(key, cat);
        financialData.setCategoryBudgets(cats);

        // Update totals
        FinancialData.BudgetOverview overview = financialData.getBudgetOverview();
        double newUtilized = overview.getUtilized() + amount;
        overview.setUtilized(newUtilized);
        overview.setRemaining(overview.getAnnualBudget() - newUtilized);
        if (overview.getAnnualBudget() > 0)
            overview.setUtilizationRate((newUtilized / overview.getAnnualBudget()) * 100);

        dataManager.saveFinancialData(financialData);
        updateBudgetOverview();
        updateCategoryBudgets();
        Toast.makeText(requireContext(), "Expense of " + formatCurrency(amount) + " recorded under " + categoryName,
                Toast.LENGTH_SHORT).show();
    }

    // ─── BUDGET ALERTS ───────────────────────────────────────────────────────
    private void showBudgetAlertsDialog() {
        StringBuilder alerts = new StringBuilder("BUDGET ALERTS\n\n");
        boolean hasAlerts = false;

        if (financialData != null && financialData.getBudgetOverview() != null) {
            double rate = financialData.getBudgetOverview().getUtilizationRate();
            if (rate >= 90) { alerts.append("🚨 CRITICAL: Overall budget is ").append((int)rate).append("% utilized!\n\n"); hasAlerts = true; }
            else if (rate >= 75) { alerts.append("⚠️ WARNING: Overall budget is ").append((int)rate).append("% utilized.\n\n"); hasAlerts = true; }
        }

        for (FinancialData.CategoryBudget cat : categoryBudgets) {
            if (cat.getAllocated() > 0 && cat.getPercentage() >= 80) {
                alerts.append("⚠️ ").append(cat.getCategoryName()).append(": ")
                        .append(String.format("%.0f%%", cat.getPercentage())).append(" utilized\n");
                hasAlerts = true;
            }
        }

        if (!hasAlerts) alerts.append("✅ All budgets are within safe limits.");

        new AlertDialog.Builder(requireContext())
                .setTitle("Budget Alerts")
                .setMessage(alerts.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // ─── GENERATE & SHARE REPORT ─────────────────────────────────────────────
    private void generateAndShareReport() {
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        String admin = currentUser != null ? currentUser.getName() : "PHC Admin";

        StringBuilder report = new StringBuilder();
        report.append("========================================\n");
        report.append("    ASHA CONNECT - FINANCIAL REPORT\n");
        report.append("========================================\n");
        report.append("Generated by: ").append(admin).append("\n");
        report.append("Date: ").append(date).append("\n");
        report.append("========================================\n\n");

        if (financialData != null && financialData.getBudgetOverview() != null) {
            FinancialData.BudgetOverview budget = financialData.getBudgetOverview();
            report.append("BUDGET OVERVIEW\n---------------\n");
            report.append("Annual Budget:     ").append(formatCurrency(budget.getAnnualBudget())).append("\n");
            report.append("Utilized:          ").append(formatCurrency(budget.getUtilized())).append("\n");
            report.append("Remaining:         ").append(formatCurrency(budget.getRemaining())).append("\n");
            report.append("Utilization Rate:  ").append(String.format("%.1f%%", budget.getUtilizationRate())).append("\n\n");

            if (!categoryBudgets.isEmpty()) {
                report.append("EXPENSE BREAKDOWN\n-----------------\n");
                for (FinancialData.CategoryBudget cat : categoryBudgets) {
                    report.append("• ").append(cat.getCategoryName()).append("\n");
                    report.append("  Spent: ").append(formatCurrency(cat.getSpent())).append("\n\n");
                }
            } else {
                report.append("No expenses recorded yet.\n");
            }
        } else {
            report.append("No budget data available.\n");
            report.append("Use 'Manage Budget' to set an annual budget first.\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Financial Report - ASHA Connect");
        shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Financial Report via"));
    }

    @Override
    public void onBudgetCategoryClick(FinancialData.CategoryBudget category) {
        new AlertDialog.Builder(requireContext())
                .setTitle(category.getCategoryName())
                .setMessage("Amount Spent: " + formatCurrency(category.getSpent()) +
                        (category.getAllocated() > 0 ? "\nAllocated: " + formatCurrency(category.getAllocated()) : ""))
                .setPositiveButton("OK", null)
                .setNeutralButton("Update Expense", (d, w) -> showExpenseAmountDialog(category.getCategoryName()))
                .show();
    }

    @Override
    public void onBudgetCategoryLongClick(FinancialData.CategoryBudget category) {
        showExpenseAmountDialog(category.getCategoryName());
    }

    public void refreshFinancialData() { loadFinancialData(); }
}