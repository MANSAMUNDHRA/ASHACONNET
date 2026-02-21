package com.macrovision.sihasha.fragments.doctors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.adapters.InventoryAdapter;
import com.macrovision.sihasha.models.InventoryItem;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class DoctorInventoryMonitoringFragment extends Fragment
        implements InventoryAdapter.OnInventoryClickListener {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private TextView tvTotalItems, tvLowStockCount, tvExpiringCount;
    private LinearLayout layoutNoData; // tv_no_data in XML is a LinearLayout — bind correctly
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipLowStock, chipExpiring;
    private Button btnRefresh;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<InventoryItem> allItems = new ArrayList<>();
    private List<InventoryItem> filteredItems = new ArrayList<>();
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_inventory_monitoring, container, false);
        try {
            initializeViews(view);
            setupDataManager();
            setupRecyclerView();
            setupChipGroup();
            loadInventoryData();
            setupClickListeners();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null)
                Toast.makeText(getContext(), "Error loading inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_inventory);
        tvTotalItems = view.findViewById(R.id.tv_total_items);
        tvLowStockCount = view.findViewById(R.id.tv_low_stock_count);
        tvExpiringCount = view.findViewById(R.id.tv_expiring_count);
        layoutNoData = view.findViewById(R.id.tv_no_data); // LinearLayout in XML, not TextView
        chipGroupFilter = view.findViewById(R.id.chip_group_filter);
        chipAll = view.findViewById(R.id.chip_all);
        chipLowStock = view.findViewById(R.id.chip_low_stock);
        chipExpiring = view.findViewById(R.id.chip_expiring);
        btnRefresh = view.findViewById(R.id.btn_refresh);
    }

    private void setupDataManager() {
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());
        currentUser = prefsManager.getCurrentUser();
    }

    private void setupRecyclerView() {
        adapter = new InventoryAdapter(filteredItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupChipGroup() {
        if (chipAll != null)
            chipAll.setOnCheckedChangeListener((b, c) -> { if (c) { currentFilter = "all"; filterItems(); } });
        if (chipLowStock != null)
            chipLowStock.setOnCheckedChangeListener((b, c) -> { if (c) { currentFilter = "lowstock"; filterItems(); } });
        if (chipExpiring != null)
            chipExpiring.setOnCheckedChangeListener((b, c) -> { if (c) { currentFilter = "expiring"; filterItems(); } });
    }

    private void setupClickListeners() {
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                dataManager.refreshInventoryFromStorage();
                loadInventoryData();
                Toast.makeText(requireContext(), "Inventory refreshed", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadInventoryData() {
        allItems = dataManager.getAllInventoryItems();

        if (tvTotalItems != null) tvTotalItems.setText(String.valueOf(allItems.size()));

        int lowStock = 0, expiring = 0;
        for (InventoryItem item : allItems) {
            if (item.isLowStock()) lowStock++;
            if (isExpiringSoon(item)) expiring++;
        }
        if (tvLowStockCount != null) tvLowStockCount.setText(String.valueOf(lowStock));
        if (tvExpiringCount != null) tvExpiringCount.setText(String.valueOf(expiring));

        filterItems();
    }

    private boolean isExpiringSoon(InventoryItem item) {
        if (item.getExpiryDate() == null || item.getExpiryDate().isEmpty()) return false;
        try {
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            String expiry = item.getExpiryDate();
            for (int y = 2020; y <= currentYear; y++) {
                if (expiry.contains(String.valueOf(y))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void filterItems() {
        filteredItems.clear();
        switch (currentFilter) {
            case "lowstock":
                for (InventoryItem item : allItems) { if (item.isLowStock()) filteredItems.add(item); }
                break;
            case "expiring":
                for (InventoryItem item : allItems) { if (isExpiringSoon(item)) filteredItems.add(item); }
                break;
            default:
                filteredItems.addAll(allItems);
                break;
        }

        if (filteredItems.isEmpty()) {
            if (layoutNoData != null) layoutNoData.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (layoutNoData != null) layoutNoData.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onInventoryItemClick(InventoryItem item) {
        if (item == null || getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Inventory Item Details")
                .setMessage("Item: " + item.getName() + "\nCategory: " + item.getCategory() +
                        "\nCurrent Stock: " + item.getCurrentStock() +
                        "\nMinimum Level: " + item.getMinimumStock() +
                        "\nExpiry Date: " + (item.getExpiryDate() != null ? item.getExpiryDate() : "N/A") +
                        "\nManufacturer: " + (item.getManufacturer() != null ? item.getManufacturer() : "N/A"))
                .setPositiveButton("OK", null)
                .setNeutralButton("Request Stock", (d, w) ->
                        Toast.makeText(requireContext(), "Request sent for " + item.getName(), Toast.LENGTH_SHORT).show())
                .show();
    }

    @Override
    public void onInventoryItemLongClick(InventoryItem item) {
        if (item == null) return;
        Toast.makeText(requireContext(), "Request more: " + item.getName(), Toast.LENGTH_SHORT).show();
    }

    public void refreshData() {
        dataManager.refreshInventoryFromStorage();
        loadInventoryData();
    }
}