package com.macrovision.sihasha.adapters;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.FinancialData;
import java.util.List;
import java.util.Locale;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

    private List<FinancialData.CategoryBudget> categories;
    private OnBudgetCategoryClickListener listener;

    public interface OnBudgetCategoryClickListener {
        void onBudgetCategoryClick(FinancialData.CategoryBudget category);
        void onBudgetCategoryLongClick(FinancialData.CategoryBudget category);
    }

    public BudgetCategoryAdapter(List<FinancialData.CategoryBudget> categories, OnBudgetCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;

        Log.d(TAG, "Adapter created with " + categories.size() + " categories");
        for (int i = 0; i < categories.size(); i++) {
            Log.d(TAG, "Category " + i + ": " + categories.get(i).getCategoryName());
        }


    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FinancialData.CategoryBudget category = categories.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        int count = categories.size();
        Log.d(TAG, "getItemCount() returning: " + count); // ✅ ADD DEBUG LOG
        return count;

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName, tvCategoryPercentage;
        private TextView tvAllocatedAmount, tvSpentAmount, tvRemainingAmount;
        private TextView tvCategoryDescription;
        private ProgressBar progressCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvCategoryPercentage = itemView.findViewById(R.id.tv_category_percentage);
            tvAllocatedAmount = itemView.findViewById(R.id.tv_allocated_amount);
            tvSpentAmount = itemView.findViewById(R.id.tv_spent_amount);
            tvRemainingAmount = itemView.findViewById(R.id.tv_remaining_amount);
            tvCategoryDescription = itemView.findViewById(R.id.tv_category_description);
            progressCategory = itemView.findViewById(R.id.progress_category);
        }

        public void bind(FinancialData.CategoryBudget category, OnBudgetCategoryClickListener listener) {
            tvCategoryName.setText(category.getCategoryName());
            tvCategoryPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", category.getPercentage()));

            tvAllocatedAmount.setText(formatCurrency(category.getAllocated()));
            tvSpentAmount.setText(formatCurrency(category.getSpent()));
            tvRemainingAmount.setText(formatCurrency(category.getRemaining()));
            tvCategoryDescription.setText(category.getDescription());

            // Set progress bar
            int progress = (int) category.getPercentage();
            progressCategory.setProgress(progress);

            // Set progress bar color based on utilization
            int progressColor;
            if (progress >= 90) {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.color_error);
            } else if (progress >= 70) {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.color_warning);
            } else {
                progressColor = ContextCompat.getColor(itemView.getContext(), R.color.color_success);
            }
            progressCategory.setProgressTintList(ColorStateList.valueOf(progressColor));

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBudgetCategoryClick(category);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onBudgetCategoryLongClick(category);
                }
                return true;
            });
        }

        private String formatCurrency(double amount) {
            if (amount >= 10000000) {
                return String.format(Locale.getDefault(), "₹%.2f Cr", amount / 10000000);
            } else if (amount >= 100000) {
                return String.format(Locale.getDefault(), "₹%.2f L", amount / 100000);
            } else {
                return String.format(Locale.getDefault(), "₹%.0f", amount);
            }
        }
    }
}
