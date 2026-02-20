package com.macrovision.sihasha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.InventoryItem;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryItem> items;
    private OnInventoryClickListener listener;

    public interface OnInventoryClickListener {
        void onInventoryItemClick(InventoryItem item);
        void onInventoryItemLongClick(InventoryItem item);
    }

    public InventoryAdapter(List<InventoryItem> items, OnInventoryClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvItemName, tvItemCategory, tvCurrentStock, tvMinimumStock;
        private TextView tvExpiryDate, tvBatchNumber, tvSupplier, tvStockStatus;
        private ProgressBar progressStockLevel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvItemCategory = itemView.findViewById(R.id.tv_item_category);
            tvCurrentStock = itemView.findViewById(R.id.tv_current_stock);
            tvMinimumStock = itemView.findViewById(R.id.tv_minimum_stock);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            tvBatchNumber = itemView.findViewById(R.id.tv_batch_number);
            tvSupplier = itemView.findViewById(R.id.tv_supplier);
            tvStockStatus = itemView.findViewById(R.id.tv_stock_status);
            progressStockLevel = itemView.findViewById(R.id.progress_stock_level);
        }

        public void bind(InventoryItem item, OnInventoryClickListener listener) {
            tvItemName.setText(item.getName());
            tvItemCategory.setText(item.getCategory());
            tvCurrentStock.setText(String.valueOf(item.getCurrentStock()));
            tvMinimumStock.setText(String.valueOf(item.getMinimumStock()));
            tvExpiryDate.setText(item.getExpiryDate());
            tvBatchNumber.setText(item.getBatchNumber());
            tvSupplier.setText(item.getSupplier());

            // Set stock status
            String stockStatus = item.getStockStatus();
            tvStockStatus.setText(stockStatus);

            // Set stock status color
            int statusColor;
            if (item.isOutOfStock()) {
                statusColor = ContextCompat.getColor(itemView.getContext(), R.color.color_error);
            } else if (item.isLowStock()) {
                statusColor = ContextCompat.getColor(itemView.getContext(), R.color.color_warning);
            } else {
                statusColor = ContextCompat.getColor(itemView.getContext(), R.color.color_success);
            }
            tvStockStatus.setTextColor(statusColor);

            // Set progress bar
            int stockPercentage = item.getMinimumStock() > 0 ?
                    (item.getCurrentStock() * 100) / (item.getMinimumStock() * 4) : 100;
            stockPercentage = Math.min(100, Math.max(0, stockPercentage));

            progressStockLevel.setProgress(stockPercentage);
            progressStockLevel.setProgressTintList(
                    ContextCompat.getColorStateList(itemView.getContext(),
                            item.isLowStock() ? R.color.color_error : R.color.color_success));

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInventoryItemClick(item);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onInventoryItemLongClick(item);
                }
                return true;
            });
        }
    }
}