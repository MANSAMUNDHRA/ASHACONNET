package com.macrovision.sihasha.models;

import java.util.Map;

public class FinancialData {
    private BudgetOverview budgetOverview;
    private Map<String, CategoryBudget> categoryBudgets;
    private String financialYear;
    private double totalRevenue;
    private double totalExpenses;

    public static class BudgetOverview {
        private double annualBudget;
        private double utilized;
        private double remaining;
        private double utilizationRate;

        // Constructors
        public BudgetOverview() {}

        public BudgetOverview(double annualBudget, double utilized, double remaining, double utilizationRate) {
            this.annualBudget = annualBudget;
            this.utilized = utilized;
            this.remaining = remaining;
            this.utilizationRate = utilizationRate;
        }

        // Getters and Setters
        public double getAnnualBudget() { return annualBudget; }
        public void setAnnualBudget(double annualBudget) { this.annualBudget = annualBudget; }

        public double getUtilized() { return utilized; }
        public void setUtilized(double utilized) { this.utilized = utilized; }

        public double getRemaining() { return remaining; }
        public void setRemaining(double remaining) { this.remaining = remaining; }

        public double getUtilizationRate() { return utilizationRate; }
        public void setUtilizationRate(double utilizationRate) { this.utilizationRate = utilizationRate; }
    }

    public static class CategoryBudget {
        private String categoryName;
        private double allocated;
        private double spent;
        private double percentage;
        private String description;

        // Constructors
        public CategoryBudget() {}

        public CategoryBudget(String categoryName, double allocated, double spent, double percentage, String description) {
            this.categoryName = categoryName;
            this.allocated = allocated;
            this.spent = spent;
            this.percentage = percentage;
            this.description = description;
        }

        // Getters and Setters
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public double getAllocated() { return allocated; }
        public void setAllocated(double allocated) { this.allocated = allocated; }

        public double getSpent() { return spent; }
        public void setSpent(double spent) { this.spent = spent; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getRemaining() { return allocated - spent; }
    }

    // Main class getters and setters
    public BudgetOverview getBudgetOverview() { return budgetOverview; }
    public void setBudgetOverview(BudgetOverview budgetOverview) { this.budgetOverview = budgetOverview; }

    public Map<String, CategoryBudget> getCategoryBudgets() { return categoryBudgets; }
    public void setCategoryBudgets(Map<String, CategoryBudget> categoryBudgets) { this.categoryBudgets = categoryBudgets; }

    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }
}
