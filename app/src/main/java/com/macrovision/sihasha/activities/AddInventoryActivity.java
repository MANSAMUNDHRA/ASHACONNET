package com.macrovision.sihasha.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.InventoryItem;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddInventoryActivity extends AppCompatActivity {

    private static final String TAG = "AddInventoryActivity";
    public static final String EXTRA_ITEM_ID = "item_id";
    public static final String EXTRA_IS_EDIT_MODE = "is_edit_mode";

    // UI Components
    private ImageView btnBack;
    private Button btnSaveItem, btnCancel, btnSave;
    private TextView tvTitle;

    // Form Fields
    private TextInputEditText editItemName, editCurrentStock, editMinimumStock;
    private TextInputEditText editCostPerUnit, editBatchNumber, editExpiryDate;
    private TextInputEditText editSupplier, editManufacturer, editStorageTemp;
    private Spinner spinnerCategory;

    // Data Management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private SimpleDateFormat dateFormat;

    // Edit mode variables
    private boolean isEditMode = false;
    private String itemId = null;
    private InventoryItem currentItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_inventory);

        Log.d(TAG, "AddInventoryActivity started");

        // Check if this is edit mode
        checkEditMode();

        initializeComponents();
        setupSpinners();
        setupEventListeners();
        loadUserData();

        // Load item data if in edit mode
        if (isEditMode) {
            loadItemData();
        }
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null) {
            isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false);
            itemId = intent.getStringExtra(EXTRA_ITEM_ID);

            Log.d(TAG, "Edit mode: " + isEditMode + ", Item ID: " + itemId);
        }
    }

    private void initializeComponents() {
        Log.d(TAG, "Initializing components...");

        // Header components
        btnBack = findViewById(R.id.btn_back);
        btnSaveItem = findViewById(R.id.btn_save_item);
        tvTitle = findViewById(R.id.tv_title);

        // Update title based on mode
        if (isEditMode) {
            tvTitle.setText("Edit Inventory Item");
            btnSaveItem.setText("Update");
        } else {
            tvTitle.setText("Add Inventory Item");
            btnSaveItem.setText("Save");
        }

        // Form Fields
        editItemName = findViewById(R.id.edit_item_name);
        editCurrentStock = findViewById(R.id.edit_current_stock);
        editMinimumStock = findViewById(R.id.edit_minimum_stock);
        editCostPerUnit = findViewById(R.id.edit_cost_per_unit);
        editBatchNumber = findViewById(R.id.edit_batch_number);
        editExpiryDate = findViewById(R.id.edit_expiry_date);
        editSupplier = findViewById(R.id.edit_supplier);
        editManufacturer = findViewById(R.id.edit_manufacturer);
        editStorageTemp = findViewById(R.id.edit_storage_temp);
        spinnerCategory = findViewById(R.id.spinner_category);

        // Action Buttons
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        // Update save button text based on mode
        if (isEditMode) {
            btnSave.setText("Update Item");
        } else {
            btnSave.setText("Save Item");
        }

        // Data Management
        dataManager = DataManager.getInstance(this);
        prefsManager = new SharedPrefsManager(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");

        // Category Spinner
        List<String> categories = Arrays.asList(
                "Select Category", "Medicine", "Vaccine", "Supply"
        );
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        Log.d(TAG, "Spinners setup completed");
    }

    private void setupEventListeners() {
        Log.d(TAG, "Setting up event listeners...");

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Save buttons (header and bottom)
        btnSaveItem.setOnClickListener(v -> saveOrUpdateItem());
        btnSave.setOnClickListener(v -> saveOrUpdateItem());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Date picker for expiry date
        editExpiryDate.setOnClickListener(v -> showDatePicker());

        Log.d(TAG, "Event listeners setup completed");
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());
        } else {
            Log.w(TAG, "Current user is null");
        }
    }

    private void loadItemData() {
        if (!isEditMode || itemId == null) {
            return;
        }

        Log.d(TAG, "Loading item data for ID: " + itemId);

        try {
            currentItem = dataManager.getInventoryItemById(itemId);

            if (currentItem == null) {
                Log.e(TAG, "Item not found with ID: " + itemId);
                Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Loading data for item: " + currentItem.getName());
            populateFormWithItemData(currentItem);

        } catch (Exception e) {
            Log.e(TAG, "Error loading item data", e);
            Toast.makeText(this, "Error loading item data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateFormWithItemData(InventoryItem item) {
        Log.d(TAG, "Populating form with item data...");

        try {
            // Basic Information
            setText(editItemName, item.getName());
            setSpinnerSelection(spinnerCategory, item.getCategory());

            // Stock Information
            setText(editCurrentStock, String.valueOf(item.getCurrentStock()));
            setText(editMinimumStock, String.valueOf(item.getMinimumStock()));
            setText(editCostPerUnit, String.valueOf(item.getCostPerUnit()));

            // Batch and Expiry Information
            setText(editBatchNumber, item.getBatchNumber());
            setText(editExpiryDate, item.getExpiryDate());

            // Supplier Information
            setText(editSupplier, item.getSupplier());
            setText(editManufacturer, item.getManufacturer());
            setText(editStorageTemp, item.getStorageTemp());

            Log.d(TAG, "Form populated successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error populating form", e);
            Toast.makeText(this, "Error loading item details", Toast.LENGTH_SHORT).show();
        }
    }

    private void setText(TextInputEditText editText, String value) {
        if (editText != null && value != null && !value.isEmpty() && !value.equals("0") && !value.equals("0.0")) {
            editText.setText(value);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner != null && value != null && !value.isEmpty()) {
            ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // If editing and field has existing date, use it
        String existingDate = getText(editExpiryDate);
        if (!existingDate.isEmpty()) {
            try {
                calendar.setTime(dateFormat.parse(existingDate));
            } catch (Exception e) {
                Log.w(TAG, "Could not parse existing date: " + existingDate);
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String selectedDate = dateFormat.format(calendar.getTime());
                    editExpiryDate.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setTitle("Select Expiry Date");
        datePickerDialog.show();
    }

    private boolean validateInput() {
        Log.d(TAG, "Validating input...");

        // Required fields validation
        if (getText(editItemName).isEmpty()) {
            editItemName.setError("Item name is required");
            editItemName.requestFocus();
            return false;
        }

        if (spinnerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getText(editCurrentStock).isEmpty()) {
            editCurrentStock.setError("Current stock is required");
            editCurrentStock.requestFocus();
            return false;
        }

        if (getText(editMinimumStock).isEmpty()) {
            editMinimumStock.setError("Minimum stock is required");
            editMinimumStock.requestFocus();
            return false;
        }

        // Numeric validation
        try {
            int currentStock = Integer.parseInt(getText(editCurrentStock));
            if (currentStock < 0) {
                editCurrentStock.setError("Current stock cannot be negative");
                editCurrentStock.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            editCurrentStock.setError("Please enter a valid number");
            editCurrentStock.requestFocus();
            return false;
        }

        try {
            int minimumStock = Integer.parseInt(getText(editMinimumStock));
            if (minimumStock < 0) {
                editMinimumStock.setError("Minimum stock cannot be negative");
                editMinimumStock.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            editMinimumStock.setError("Please enter a valid number");
            editMinimumStock.requestFocus();
            return false;
        }

        // Cost validation (if provided)
        String costText = getText(editCostPerUnit);
        if (!costText.isEmpty()) {
            try {
                double cost = Double.parseDouble(costText);
                if (cost < 0) {
                    editCostPerUnit.setError("Cost cannot be negative");
                    editCostPerUnit.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                editCostPerUnit.setError("Please enter a valid cost");
                editCostPerUnit.requestFocus();
                return false;
            }
        }

        Log.d(TAG, "Input validation passed");
        return true;
    }

    private void saveOrUpdateItem() {
        Log.d(TAG, "Attempting to save/update item...");

        if (!validateInput()) {
            return;
        }

        try {
            InventoryItem item;

            if (isEditMode && currentItem != null) {
                // Update existing item
                item = currentItem;
                Log.d(TAG, "Updating existing item: " + item.getId());
            } else {
                // Create new item
                item = new InventoryItem();
                String itemId = generateItemId();
                item.setId(itemId);
                Log.d(TAG, "Creating new item with ID: " + itemId);
            }

            // Update all fields
            updateItemFromForm(item);

            // Save or update item in DataManager
            boolean success;
            if (isEditMode) {
                success = dataManager.updateInventoryItem(item);
            } else {
                success = dataManager.addInventoryItem(item);
            }

            if (success) {
                String message = isEditMode ? "Item updated successfully!" : "Item saved successfully!";
                Log.d(TAG, message + " - " + item.getName());
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Set result and finish
                setResult(RESULT_OK);
                finish();
            } else {
                String errorMessage = isEditMode ? "Failed to update item. Please try again." : "Failed to save item. Please try again.";
                Log.e(TAG, errorMessage);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            String errorMessage = "Error " + (isEditMode ? "updating" : "saving") + " item: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void updateItemFromForm(InventoryItem item) {
        // Basic Information
        item.setName(getText(editItemName));
        item.setCategory(spinnerCategory.getSelectedItem().toString());

        // Stock Information
        item.setCurrentStock(Integer.parseInt(getText(editCurrentStock)));
        item.setMinimumStock(Integer.parseInt(getText(editMinimumStock)));

        String costText = getText(editCostPerUnit);
        if (!costText.isEmpty()) {
            item.setCostPerUnit(Double.parseDouble(costText));
        }

        // Batch and Expiry Information
        item.setBatchNumber(getText(editBatchNumber));
        item.setExpiryDate(getText(editExpiryDate));

        // Supplier Information
        item.setSupplier(getText(editSupplier));
        item.setManufacturer(getText(editManufacturer));
        item.setStorageTemp(getText(editStorageTemp));
    }

    private String generateItemId() {
        String category = spinnerCategory.getSelectedItem().toString();
        String prefix;

        switch (category) {
            case "Medicine":
                prefix = "MED";
                break;
            case "Vaccine":
                prefix = "VAC";
                break;
            case "Supply":
                prefix = "SUP";
                break;
            default:
                prefix = "ITM";
                break;
        }

        return prefix + String.format("%03d", (int)(Math.random() * 1000));
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
