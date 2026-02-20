package com.macrovision.sihasha.fragments;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.activities.AddInventoryActivity;
import com.macrovision.sihasha.adapters.InventoryAdapter;
import com.macrovision.sihasha.models.InventoryItem;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InventoryFragment extends Fragment implements InventoryAdapter.OnInventoryClickListener {

    private static final String TAG = "InventoryFragment";

    // UI Components
    private RecyclerView recyclerInventory;
    private InventoryAdapter inventoryAdapter;
    private TextInputEditText editSearch;
    private Spinner spinnerCategoryFilter, spinnerStockFilter;
    private TextView tvTotalItems, tvLowStockCount;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabAddItem;
    private Button btnAddItem, btnClearFilters;

    // Filter Buttons
    private Button btnFilterAll, btnFilterMedicines, btnFilterVaccines,
            btnFilterSupplies, btnFilterLowStock;

    // Data Management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<InventoryItem> allItems;
    private List<InventoryItem> filteredItems;

    // Filter States
    private String currentFilter = "all";
    private String currentSearchTerm = "";
    private String currentCategoryFilter = "all";
    private String currentStockFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        try {
            initializeComponents(view);
            setupRecyclerView();
            setupSpinners();
            setupEventListeners();
            setupFilterTabs();
            loadUserData();
            loadInventory();

            Log.d(TAG, "Fragment setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
        }

        return view;
    }

    private void initializeComponents(View view) {
        Log.d(TAG, "Initializing components...");

        // UI Components
        recyclerInventory = view.findViewById(R.id.recycler_inventory);
        editSearch = view.findViewById(R.id.edit_search);
        spinnerCategoryFilter = view.findViewById(R.id.spinner_category_filter);
        spinnerStockFilter = view.findViewById(R.id.spinner_stock_filter);
        tvTotalItems = view.findViewById(R.id.tv_total_items);
        tvLowStockCount = view.findViewById(R.id.tv_low_stock_count);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        fabAddItem = view.findViewById(R.id.fab_add_item);
        btnAddItem = view.findViewById(R.id.btn_add_item);
        btnClearFilters = view.findViewById(R.id.btn_clear_filters);

        // Filter Buttons
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterMedicines = view.findViewById(R.id.btn_filter_medicines);
        btnFilterVaccines = view.findViewById(R.id.btn_filter_vaccines);
        btnFilterSupplies = view.findViewById(R.id.btn_filter_supplies);
        btnFilterLowStock = view.findViewById(R.id.btn_filter_low_stock);

        // Data Management
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());

        // Initialize lists
        allItems = new ArrayList<>();
        filteredItems = new ArrayList<>();

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView...");

        if (recyclerInventory == null) {
            Log.e(TAG, "RecyclerView is null!");
            return;
        }

        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            recyclerInventory.setLayoutManager(layoutManager);

            inventoryAdapter = new InventoryAdapter(filteredItems, this);
            recyclerInventory.setAdapter(inventoryAdapter);

            Log.d(TAG, "RecyclerView setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");

        if (spinnerCategoryFilter == null || spinnerStockFilter == null) {
            Log.e(TAG, "Spinners are null!");
            return;
        }

        try {
            // Category Filter Spinner
            List<String> categories = Arrays.asList(
                    "All Categories", "Medicine", "Vaccine", "Supply"
            );
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categories
            );
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategoryFilter.setAdapter(categoryAdapter);

            // Stock Filter Spinner
            List<String> stockFilters = Arrays.asList(
                    "All Stock Levels", "In Stock", "Low Stock", "Out of Stock"
            );
            ArrayAdapter<String> stockAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    stockFilters
            );
            stockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStockFilter.setAdapter(stockAdapter);

            Log.d(TAG, "Spinners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    private void setupEventListeners() {
        Log.d(TAG, "Setting up event listeners...");

        try {
            // Search functionality
            if (editSearch != null) {
                editSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        currentSearchTerm = s.toString().toLowerCase().trim();
                        Log.d(TAG, "Search term changed: " + currentSearchTerm);
                        applyFilters();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }

            // Category filter spinner
            if (spinnerCategoryFilter != null) {
                spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selected = parent.getItemAtPosition(position).toString();
                        currentCategoryFilter = convertCategoryFilterToKey(selected);
                        Log.d(TAG, "Category filter changed: " + currentCategoryFilter);
                        applyFilters();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            // Stock filter spinner
            if (spinnerStockFilter != null) {
                spinnerStockFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selected = parent.getItemAtPosition(position).toString();
                        currentStockFilter = convertStockFilterToKey(selected);
                        Log.d(TAG, "Stock filter changed: " + currentStockFilter);
                        applyFilters();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            // Add item buttons
            if (btnAddItem != null) {
                btnAddItem.setOnClickListener(v -> showAddInventoryDialog());
            }
            if (fabAddItem != null) {
                fabAddItem.setOnClickListener(v -> showAddInventoryDialog());
            }

            // Clear filters button
            if (btnClearFilters != null) {
                btnClearFilters.setOnClickListener(v -> clearAllFilters());
            }

            Log.d(TAG, "Event listeners setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up event listeners", e);
        }
    }

    private void setupFilterTabs() {
        Log.d(TAG, "Setting up filter tabs...");

        try {
            View.OnClickListener filterClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetFilterTabStates();
                    v.setSelected(true);

                    if (v.getId() == R.id.btn_filter_all) {
                        currentFilter = "all";
                    } else if (v.getId() == R.id.btn_filter_medicines) {
                        currentFilter = "medicines";
                    } else if (v.getId() == R.id.btn_filter_vaccines) {
                        currentFilter = "vaccines";
                    } else if (v.getId() == R.id.btn_filter_supplies) {
                        currentFilter = "supplies";
                    } else if (v.getId() == R.id.btn_filter_low_stock) {
                        currentFilter = "lowstock";
                    }

                    Log.d(TAG, "Filter tab changed to: " + currentFilter);
                    applyFilters();
                }
            };

            if (btnFilterAll != null) btnFilterAll.setOnClickListener(filterClickListener);
            if (btnFilterMedicines != null) btnFilterMedicines.setOnClickListener(filterClickListener);
            if (btnFilterVaccines != null) btnFilterVaccines.setOnClickListener(filterClickListener);
            if (btnFilterSupplies != null) btnFilterSupplies.setOnClickListener(filterClickListener);
            if (btnFilterLowStock != null) btnFilterLowStock.setOnClickListener(filterClickListener);

            // Set default selected state
            if (btnFilterAll != null) {
                btnFilterAll.setSelected(true);
            }

            Log.d(TAG, "Filter tabs setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter tabs", e);
        }
    }

    private void loadUserData() {
        Log.d(TAG, "Loading user data...");

        try {
            currentUser = prefsManager.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

                // Hide add buttons for ASHA workers
                if ("asha".equals(currentUser.getRole())) {
                    if (btnAddItem != null) btnAddItem.setVisibility(View.GONE);
                    if (fabAddItem != null) fabAddItem.setVisibility(View.GONE);
                }
            } else {
                Log.w(TAG, "Current user is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
        }
    }

    private void loadInventory() {
        Log.d(TAG, "Loading inventory...");

        try {
            Log.d(TAG, "Current user: " + (currentUser != null ? currentUser.getName() : "null"));

            // Load all inventory items (access control handled by UI visibility)
            allItems = dataManager.getAllInventoryItems();
            Log.d(TAG, "All inventory items loaded: " + allItems.size());

            // Debug each item
            for (int i = 0; i < allItems.size(); i++) {
                InventoryItem item = allItems.get(i);
                Log.d(TAG, "Item " + i + ": " + item.getName() + ", Stock: " + item.getCurrentStock() + ", Status: " + item.getStockStatus());
            }

            applyFilters();
            updateSummaryCards();

        } catch (Exception e) {
            Log.e(TAG, "Error loading inventory", e);
        }
    }

    private void applyFilters() {
        Log.d(TAG, "Applying filters...");

        try {
            filteredItems.clear();

            // Start with all items
            List<InventoryItem> workingList = new ArrayList<>(allItems);
            Log.d(TAG, "Starting with " + workingList.size() + " items");

            // Apply category filter (from tabs)
            workingList = applyCategoryFilter(workingList);
            Log.d(TAG, "After category filter: " + workingList.size() + " items");

            // Apply category filter (from spinner)
            workingList = applySpinnerCategoryFilter(workingList);
            Log.d(TAG, "After spinner category filter: " + workingList.size() + " items");

            // Apply stock filter
            workingList = applyStockFilter(workingList);
            Log.d(TAG, "After stock filter: " + workingList.size() + " items");

            // Apply search filter
            workingList = applySearchFilter(workingList);
            Log.d(TAG, "After search filter: " + workingList.size() + " items");

            // Update filtered list
            filteredItems.addAll(workingList);

            // Update UI
            updateUI();

        } catch (Exception e) {
            Log.e(TAG, "Error applying filters", e);
        }
    }

    private List<InventoryItem> applyCategoryFilter(List<InventoryItem> items) {
        if ("all".equals(currentFilter)) {
            return items;
        }

        List<InventoryItem> filtered = new ArrayList<>();
        for (InventoryItem item : items) {
            switch (currentFilter) {
                case "medicines":
                    if ("Medicine".equalsIgnoreCase(item.getCategory())) {
                        filtered.add(item);
                    }
                    break;
                case "vaccines":
                    if ("Vaccine".equalsIgnoreCase(item.getCategory())) {
                        filtered.add(item);
                    }
                    break;
                case "supplies":
                    if ("Supply".equalsIgnoreCase(item.getCategory())) {
                        filtered.add(item);
                    }
                    break;
                case "lowstock":
                    if (item.isLowStock()) {
                        filtered.add(item);
                    }
                    break;
                default:
                    filtered.add(item);
                    break;
            }
        }
        return filtered;
    }

    private List<InventoryItem> applySpinnerCategoryFilter(List<InventoryItem> items) {
        if ("all".equals(currentCategoryFilter)) {
            return items;
        }

        List<InventoryItem> filtered = new ArrayList<>();
        for (InventoryItem item : items) {
            if (currentCategoryFilter.equalsIgnoreCase(item.getCategory())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private List<InventoryItem> applyStockFilter(List<InventoryItem> items) {
        if ("all".equals(currentStockFilter)) {
            return items;
        }

        List<InventoryItem> filtered = new ArrayList<>();
        for (InventoryItem item : items) {
            switch (currentStockFilter) {
                case "instock":
                    if (!item.isLowStock() && !item.isOutOfStock()) {
                        filtered.add(item);
                    }
                    break;
                case "lowstock":
                    if (item.isLowStock() && !item.isOutOfStock()) {
                        filtered.add(item);
                    }
                    break;
                case "outofstock":
                    if (item.isOutOfStock()) {
                        filtered.add(item);
                    }
                    break;
            }
        }
        return filtered;
    }

    private List<InventoryItem> applySearchFilter(List<InventoryItem> items) {
        if (currentSearchTerm.isEmpty()) {
            return items;
        }

        List<InventoryItem> filtered = new ArrayList<>();
        for (InventoryItem item : items) {
            if (matchesSearchTerm(item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean matchesSearchTerm(InventoryItem item) {
        String searchLower = currentSearchTerm.toLowerCase();

        return (item.getName() != null && item.getName().toLowerCase().contains(searchLower)) ||
                (item.getCategory() != null && item.getCategory().toLowerCase().contains(searchLower)) ||
                (item.getBatchNumber() != null && item.getBatchNumber().toLowerCase().contains(searchLower)) ||
                (item.getSupplier() != null && item.getSupplier().toLowerCase().contains(searchLower));
    }

    private String convertCategoryFilterToKey(String displayName) {
        switch (displayName) {
            case "Medicine": return "medicine";
            case "Vaccine": return "vaccine";
            case "Supply": return "supply";
            default: return "all";
        }
    }

    private String convertStockFilterToKey(String displayName) {
        switch (displayName) {
            case "In Stock": return "instock";
            case "Low Stock": return "lowstock";
            case "Out of Stock": return "outofstock";
            default: return "all";
        }
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI with " + filteredItems.size() + " filtered items");

        try {
            if (filteredItems.isEmpty()) {
                Log.d(TAG, "Showing empty state");
                showEmptyState();
            } else {
                Log.d(TAG, "Showing inventory list");
                showInventoryList();
                if (inventoryAdapter != null) {
                    inventoryAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Adapter notified of data change");
                } else {
                    Log.e(TAG, "Inventory adapter is null!");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    private void updateSummaryCards() {
        try {
            int totalItems = allItems.size();
            int lowStockCount = dataManager.getLowStockItems().size();

            if (tvTotalItems != null) {
                tvTotalItems.setText(String.valueOf(totalItems));
            }

            if (tvLowStockCount != null) {
                tvLowStockCount.setText(String.valueOf(lowStockCount));
            }

            Log.d(TAG, "Summary cards updated - Total: " + totalItems + ", Low Stock: " + lowStockCount);

        } catch (Exception e) {
            Log.e(TAG, "Error updating summary cards", e);
        }
    }

    private void resetFilterTabStates() {
        if (btnFilterAll != null) btnFilterAll.setSelected(false);
        if (btnFilterMedicines != null) btnFilterMedicines.setSelected(false);
        if (btnFilterVaccines != null) btnFilterVaccines.setSelected(false);
        if (btnFilterSupplies != null) btnFilterSupplies.setSelected(false);
        if (btnFilterLowStock != null) btnFilterLowStock.setSelected(false);
    }

    private void clearAllFilters() {
        Log.d(TAG, "Clearing all filters");

        // Reset filter states
        currentFilter = "all";
        currentCategoryFilter = "all";
        currentStockFilter = "all";
        currentSearchTerm = "";

        // Reset UI controls
        if (editSearch != null) editSearch.setText("");
        if (spinnerCategoryFilter != null) spinnerCategoryFilter.setSelection(0);
        if (spinnerStockFilter != null) spinnerStockFilter.setSelection(0);

        // Reset filter tabs
        resetFilterTabStates();
        if (btnFilterAll != null) btnFilterAll.setSelected(true);

        // Apply filters
        applyFilters();
    }

    private void showAddInventoryDialog() {
        Intent intent = new Intent(requireContext(), AddInventoryActivity.class);
        startActivityForResult(intent, 2001); // Request code for add
    }

    // Add method to handle edit
    private void editInventoryItem(InventoryItem item) {
        Log.d(TAG, "Editing inventory item: " + item.getName());

        Intent intent = new Intent(requireContext(), AddInventoryActivity.class);
        intent.putExtra(AddInventoryActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(AddInventoryActivity.EXTRA_ITEM_ID, item.getId());
        startActivityForResult(intent, 2002); // Request code for edit
    }

    // Update the context menu method
    private void showItemContextMenu(InventoryItem item) {
        String[] options;
        if ("asha".equals(currentUser.getRole())) {
            options = new String[]{"View Details", "Request Stock"};
        } else {
            options = new String[]{"View Details", "Edit Item", "Update Stock", "Delete Item"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Inventory Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // View Details
                            showItemDetail(item);
                            break;
                        case 1: // Edit Item / Request Stock
                            if ("asha".equals(currentUser.getRole())) {
                                Toast.makeText(requireContext(), "Stock request for: " + item.getName(), Toast.LENGTH_SHORT).show();
                            } else {
                                editInventoryItem(item); // ✅ Use the edit activity
                            }
                            break;
                        case 2: // Update Stock (non-ASHA only)
                            editInventoryItem(item); // ✅ Also use edit for stock updates
                            break;
                        case 3: // Delete Item (non-ASHA only)
                            confirmDeleteItem(item);
                            break;
                    }
                })
                .show();
    }

    // Handle activity results
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 2001) {
                // Item was added successfully
                Toast.makeText(requireContext(), "Item added successfully!", Toast.LENGTH_SHORT).show();
                loadInventory();
            } else if (requestCode == 2002) {
                // Item was updated successfully
                Toast.makeText(requireContext(), "Item updated successfully!", Toast.LENGTH_SHORT).show();
                loadInventory();
            }
        }
    }


    // UI State Management Methods
    private void showInventoryList() {
        try {
            if (recyclerInventory != null) {
                recyclerInventory.setVisibility(View.VISIBLE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.GONE);
            }

            Log.d(TAG, "Showing inventory list");
        } catch (Exception e) {
            Log.e(TAG, "Error in showInventoryList", e);
        }
    }

    private void showEmptyState() {
        try {
            if (recyclerInventory != null) {
                recyclerInventory.setVisibility(View.GONE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            }

            // Update empty state message based on filters
            TextView emptyMessage = layoutEmptyState.findViewById(R.id.tv_empty_message);
            if (emptyMessage != null) {
                if (hasActiveFilters()) {
                    emptyMessage.setText("Try adjusting your filters or search terms");
                    if (btnClearFilters != null) {
                        btnClearFilters.setVisibility(View.VISIBLE);
                    }
                } else {
                    emptyMessage.setText("No inventory items found");
                    if (btnClearFilters != null) {
                        btnClearFilters.setVisibility(View.GONE);
                    }
                }
            }

            Log.d(TAG, "Showing empty state");
        } catch (Exception e) {
            Log.e(TAG, "Error in showEmptyState", e);
        }
    }

    private boolean hasActiveFilters() {
        return !currentFilter.equals("all") ||
                !currentCategoryFilter.equals("all") ||
                !currentStockFilter.equals("all") ||
                !currentSearchTerm.isEmpty();
    }

    // InventoryAdapter.OnInventoryClickListener implementation
    @Override
    public void onInventoryItemClick(InventoryItem item) {
        Log.d(TAG, "Inventory item clicked: " + item.getName());
        showItemDetail(item);
    }

    @Override
    public void onInventoryItemLongClick(InventoryItem item) {
        Log.d(TAG, "Inventory item long clicked: " + item.getName());
        showItemContextMenu(item);
    }

    private void showItemDetail(InventoryItem item) {
        String details = "Inventory Item Details:\n\n" +
                "Name: " + item.getName() + "\n" +
                "Category: " + item.getCategory() + "\n" +
                "Current Stock: " + item.getCurrentStock() + "\n" +
                "Minimum Stock: " + item.getMinimumStock() + "\n" +
                "Status: " + item.getStockStatus() + "\n" +
                "Batch Number: " + item.getBatchNumber() + "\n" +
                "Expiry Date: " + item.getExpiryDate() + "\n" +
                "Supplier: " + item.getSupplier() + "\n" +
                "Cost per Unit: ₹" + item.getCostPerUnit();

        new AlertDialog.Builder(requireContext())
                .setTitle("Inventory Item Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }


    private void confirmDeleteItem(InventoryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Inventory Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteItem(item);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteItem(InventoryItem item) {
        boolean success = dataManager.deleteInventoryItem(item.getId());

        if (success) {
            Toast.makeText(requireContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show();
            loadInventory(); // Refresh the list
        } else {
            Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
        }
    }

    // Public methods for external access
    public void refreshInventory() {
        loadInventory();
    }
}