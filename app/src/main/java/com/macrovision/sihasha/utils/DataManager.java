package com.macrovision.sihasha.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.macrovision.sihasha.models.FinancialData;
import com.macrovision.sihasha.models.InventoryItem;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.Staff;
import com.macrovision.sihasha.models.User;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private static final String TAG = "DataManager";
    private static final String PREFS_NAME = "sihasha_app_prefs";
    private static final String PATIENTS_KEY = "patients_data";
    private static final String USERS_KEY = "users_data";
    private static final String FINANCIAL_KEY = "financial_data";
    private static final String INVENTORY_KEY = "inventory_data";
    private static final String STAFF_KEY = "staff_data";
    // Bump this number any time you want to wipe stale SharedPreferences data
    private static final int DATA_VERSION = 2;
    private static final String DATA_VERSION_KEY = "data_version";

    private Context context;
    private List<User> users;
    private List<Patient> patients;
    private List<InventoryItem> inventoryItems;
    private List<Staff> staffList;
    private FinancialData financialData;
    
    private static DataManager instance;
    private Gson gson;
    private SharedPreferences prefs;

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Wipe only stale non-user data when DATA_VERSION bumps.
        // NEVER clear users_data — that would delete all registered accounts.
        int savedVersion = prefs.getInt(DATA_VERSION_KEY, 0);
        if (savedVersion < DATA_VERSION) {
            android.util.Log.w(TAG, "Data version mismatch — clearing stale data (preserving users)");
            String usersJson = prefs.getString(USERS_KEY, null); // save users first
            prefs.edit().clear().apply();                         // wipe everything
            if (usersJson != null) {
                prefs.edit().putString(USERS_KEY, usersJson).apply(); // restore users
            }
            prefs.edit().putInt(DATA_VERSION_KEY, DATA_VERSION).apply();
        }

        // Load all data from storage (NO DEMO DATA)
        loadOrInitializeUsers();
        loadOrInitializePatients();
        loadOrInitializeInventory();
        loadOrInitializeStaff();
        loadOrInitializeFinancial();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    // ===== USER STORAGE METHODS =====

    private void loadOrInitializeUsers() {
        try {
            String json = prefs.getString(USERS_KEY, null);
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<User>>() {}.getType();
                users = gson.fromJson(json, listType);
                Log.d(TAG, "Loaded " + users.size() + " users from storage");
            } else {
                users = new ArrayList<>();
                Log.d(TAG, "No users found, initialized empty list");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading users", e);
            users = new ArrayList<>();
        }
    }

    private void saveUsersToStorage() {
        try {
            String json = gson.toJson(users);
            prefs.edit().putString(USERS_KEY, json).apply();
            Log.d(TAG, "Saved " + users.size() + " users to storage");
        } catch (Exception e) {
            Log.e(TAG, "Error saving users", e);
        }
    }

    public boolean registerUser(User user) {
    if (users == null) users = new ArrayList<>();

    // prevent duplicate ID
    for (User u : users) {
        if (u.getId().equals(user.getId())) {
            Log.w(TAG, "User already exists with ID: " + user.getId());
            return false;
        }
    }

    users.add(user);
    saveUsersToStorage();
    
    // ✅ ALSO ADD TO STAFF LIST
    addUserToStaff(user);
    
    Log.d(TAG, "User registered successfully: " + user.getName());
    return true;
}

// ✅ NEW METHOD: Add user to staff list
private void addUserToStaff(User user) {
    Staff staff = new Staff(
        user.getId(),
        user.getName(),
        user.getRole(),
        user.getPhone() != null ? user.getPhone() : "",
        user.getVillage() != null ? user.getVillage() : "",
        user.getDistrict() != null ? user.getDistrict() : ""
    );
    
    staff.setBlock(user.getBlock());
    staff.setState(user.getState());
    staff.setPhcId(user.getPhcId());
    staff.setJoiningDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
    staff.setStatus("active");
    
    // Add role-specific fields
    if ("phcdoctor".equals(user.getRole())) {
        staff.setQualification("MBBS"); // Default, can be edited later
        staff.setSpecialization("General Medicine");
    } else if ("phcnurse".equals(user.getRole())) {
        staff.setQualification("GNM");
    } else if ("asha".equals(user.getRole())) {
        staff.setAssignedPopulation("0");
        staff.setAssignedFamilies("0");
    } else if ("phcadmin".equals(user.getRole())) {
        staff.setDesignation("Administrator");
    }
    
    // Add to staff list
    if (staffList == null) {
        staffList = new ArrayList<>();
    }
    staffList.add(staff);
    saveStaffToStorage();
}

    // ===== AUTHENTICATION METHOD =====

    public User authenticateUser(String userId, String role, String password) {
        if (users == null || users.isEmpty()) {
            Log.d(TAG, "No users in system");
            return null;
        }

        for (User user : users) {
            if (user.getId().equals(userId)
                    && user.getRole().equals(role)
                    && user.getPassword().equals(password)) {
                Log.d(TAG, "User authenticated: " + user.getName());
                return user;
            }
        }

        Log.d(TAG, "Authentication failed for user: " + userId);
        return null;
    }

    // ===== PATIENT METHODS =====

    private void loadOrInitializePatients() {
        try {
            String json = prefs.getString(PATIENTS_KEY, null);
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<Patient>>() {}.getType();
                patients = gson.fromJson(json, listType);
                Log.d(TAG, "Loaded " + patients.size() + " patients from storage");
            } else {
                patients = new ArrayList<>();
                Log.d(TAG, "No patients found, initialized empty list");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading patients", e);
            patients = new ArrayList<>();
        }
    }

    private void savePatientsToStorage() {
        try {
            String json = gson.toJson(patients);
            prefs.edit().putString(PATIENTS_KEY, json).apply();
            Log.d(TAG, "Saved " + patients.size() + " patients to storage");
        } catch (Exception e) {
            Log.e(TAG, "Error saving patients", e);
        }
    }

    // Patient CRUD Operations
    public boolean addPatient(Patient patient) {
        try {
            if (patients == null) patients = new ArrayList<>();
            patients.add(patient);
            savePatientsToStorage();
            Log.d(TAG, "Patient added successfully: " + patient.getName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding patient", e);
            return false;
        }
    }

    public boolean updatePatient(Patient updatedPatient) {
        try {
            if (patients == null) return false;

            for (int i = 0; i < patients.size(); i++) {
                if (patients.get(i).getId().equals(updatedPatient.getId())) {
                    patients.set(i, updatedPatient);
                    savePatientsToStorage();
                    Log.d(TAG, "Patient updated successfully: " + updatedPatient.getName());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error updating patient", e);
            return false;
        }
    }

    public boolean deletePatient(String patientId) {
        try {
            if (patients == null) return false;

            for (int i = 0; i < patients.size(); i++) {
                if (patients.get(i).getId().equals(patientId)) {
                    patients.remove(i);
                    savePatientsToStorage();
                    Log.d(TAG, "Patient deleted successfully: " + patientId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting patient", e);
            return false;
        }
    }

    public Patient getPatientById(String patientId) {
        if (patients == null) return null;

        for (Patient patient : patients) {
            if (patient.getId().equals(patientId)) {
                return patient;
            }
        }
        return null;
    }

    public List<Patient> getAllPatients() {
        if (patients == null) return new ArrayList<>();
        return new ArrayList<>(patients);
    }

    // Role-based patient queries
    public List<Patient> getPatientsForASHA(String ashaId) {
        List<Patient> result = new ArrayList<>();
        if (patients == null || ashaId == null) return result;

        for (Patient patient : patients) {
            if (ashaId.equals(patient.getAshaId())) {
                result.add(patient);
            }
        }
        return result;
    }

    public List<Patient> getPatientsForDoctor(String doctorId) {
        List<Patient> result = new ArrayList<>();
        if (patients == null || doctorId == null) return result;

        for (Patient patient : patients) {
            if (doctorId.equals(patient.getAssignedDoctor())) {
                result.add(patient);
            }
        }
        return result;
    }

    public List<Patient> getPatientsForPHC(String phcId) {
        List<Patient> result = new ArrayList<>();
        if (patients == null || phcId == null) return result;

        for (Patient patient : patients) {
            if (phcId.equals(patient.getPhcId())) {
                result.add(patient);
            }
        }
        return result;
    }

    // Clinical queries for doctors
    public List<Patient> getHighRiskPatients() {
        List<Patient> result = new ArrayList<>();
        if (patients == null) return result;

        for (Patient patient : patients) {
            if (patient.isHighRisk()) {
                result.add(patient);
            }
        }
        return result;
    }

    public List<Patient> getHighRiskPatientsForDoctor(String doctorId) {
        List<Patient> result = new ArrayList<>();
        if (patients == null || doctorId == null) return result;

        for (Patient patient : patients) {
            if (patient.isHighRisk() && doctorId.equals(patient.getAssignedDoctor())) {
                result.add(patient);
            }
        }
        return result;
    }

    public List<Patient> getPregnantPatients() {
        List<Patient> result = new ArrayList<>();
        if (patients == null) return result;

        for (Patient patient : patients) {
            if ("pregnant".equals(patient.getPregnancyStatus())) {
                result.add(patient);
            }
        }
        return result;
    }

    public List<Patient> getPatientsByStatus(String status) {
        List<Patient> result = new ArrayList<>();
        if (patients == null) return result;

        for (Patient patient : patients) {
            if (status.equals(patient.getPregnancyStatus())) {
                result.add(patient);
            }
        }
        return result;
    }

    // ===== STAFF METHODS =====

    private void loadOrInitializeStaff() {
        try {
            String json = prefs.getString(STAFF_KEY, null);
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<Staff>>() {}.getType();
                staffList = gson.fromJson(json, listType);
                Log.d(TAG, "Loaded " + staffList.size() + " staff from storage");
            } else {
                staffList = new ArrayList<>();
                Log.d(TAG, "No staff found, initialized empty list");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading staff", e);
            staffList = new ArrayList<>();
        }
    }

    private void saveStaffToStorage() {
        try {
            String json = gson.toJson(staffList);
            prefs.edit().putString(STAFF_KEY, json).apply();
            Log.d(TAG, "Saved " + staffList.size() + " staff to storage");
        } catch (Exception e) {
            Log.e(TAG, "Error saving staff", e);
        }
    }

    public List<User> getAllUsers() {
        if (users == null) return new ArrayList<>();
        return new ArrayList<>(users);
    }

    public List<Staff> getStaffList() {
        if (staffList == null) return new ArrayList<>();
        return new ArrayList<>(staffList);
    }

    public void addStaffMember(Staff staff) {
        if (staffList == null) staffList = new ArrayList<>();
        staffList.add(staff);
        saveStaffToStorage();
    }

    public void updateStaffMember(Staff staff) {
        if (staffList == null) return;
        for (int i = 0; i < staffList.size(); i++) {
            if (staffList.get(i).getId().equals(staff.getId())) {
                staffList.set(i, staff);
                saveStaffToStorage();
                break;
            }
        }
    }

    public List<User> getASHAWorkers() {
        List<User> ashaWorkers = new ArrayList<>();
        if (users == null) return ashaWorkers;

        for (User user : users) {
            if ("asha".equals(user.getRole())) {
                ashaWorkers.add(user);
            }
        }
        return ashaWorkers;
    }

    public List<User> getDoctors() {
        List<User> doctors = new ArrayList<>();
        if (users == null) return doctors;

        for (User user : users) {
            if ("phcdoctor".equals(user.getRole())) {
                doctors.add(user);
            }
        }
        return doctors;
    }

    public User getUserById(String userId) {
        if (users == null) return null;
        for (User user : users) {
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        return null;
    }

    // ===== INVENTORY METHODS =====

    private void loadOrInitializeInventory() {
        try {
            String json = prefs.getString(INVENTORY_KEY, null);
            if (json != null && !json.isEmpty()) {
                Type listType = new TypeToken<List<InventoryItem>>() {}.getType();
                inventoryItems = gson.fromJson(json, listType);
                Log.d(TAG, "Loaded " + inventoryItems.size() + " inventory items from storage");
            } else {
                inventoryItems = new ArrayList<>();
                Log.d(TAG, "No inventory found, initialized empty list");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading inventory", e);
            inventoryItems = new ArrayList<>();
        }
    }

    private void saveInventoryToStorage() {
        try {
            String json = gson.toJson(inventoryItems);
            prefs.edit().putString(INVENTORY_KEY, json).apply();
            Log.d(TAG, "Saved " + inventoryItems.size() + " inventory items to storage");
        } catch (Exception e) {
            Log.e(TAG, "Error saving inventory", e);
        }
    }

    public List<InventoryItem> getAllInventoryItems() {
        if (inventoryItems == null) return new ArrayList<>();
        return new ArrayList<>(inventoryItems);
    }

    public List<InventoryItem> getLowStockItems() {
        List<InventoryItem> lowStock = new ArrayList<>();
        if (inventoryItems == null) return lowStock;

        for (InventoryItem item : inventoryItems) {
            if (item.isLowStock()) {
                lowStock.add(item);
            }
        }
        return lowStock;
    }

    public boolean addInventoryItem(InventoryItem item) {
        try {
            if (inventoryItems == null) inventoryItems = new ArrayList<>();
            inventoryItems.add(item);
            saveInventoryToStorage();
            Log.d(TAG, "Inventory item added: " + item.getName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding inventory item", e);
            return false;
        }
    }

    public boolean updateInventoryItem(InventoryItem updatedItem) {
        try {
            if (inventoryItems == null) return false;
            for (int i = 0; i < inventoryItems.size(); i++) {
                if (inventoryItems.get(i).getId().equals(updatedItem.getId())) {
                    inventoryItems.set(i, updatedItem);
                    saveInventoryToStorage();
                    Log.d(TAG, "Inventory item updated: " + updatedItem.getName());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error updating inventory item", e);
            return false;
        }
    }

    public boolean deleteInventoryItem(String itemId) {
        try {
            if (inventoryItems == null) return false;
            
            for (int i = 0; i < inventoryItems.size(); i++) {
                if (inventoryItems.get(i).getId().equals(itemId)) {
                    inventoryItems.remove(i);
                    saveInventoryToStorage();
                    Log.d(TAG, "Inventory item deleted: " + itemId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting inventory item", e);
            return false;
        }
    }

    public InventoryItem getInventoryItemById(String itemId) {
        if (inventoryItems == null || itemId == null) {
            Log.w(TAG, "Inventory items list is null or itemId is null");
            return null;
        }
        
        for (InventoryItem item : inventoryItems) {
            if (item.getId() != null && item.getId().equals(itemId)) {
                Log.d(TAG, "Found inventory item: " + item.getName() + " with ID: " + itemId);
                return item;
            }
        }
        
        Log.w(TAG, "Inventory item not found with ID: " + itemId);
        return null;
    }

    // ===== FINANCIAL METHODS =====

    private void loadOrInitializeFinancial() {
        try {
            String json = prefs.getString(FINANCIAL_KEY, null);
            if (json != null && !json.isEmpty()) {
                financialData = gson.fromJson(json, FinancialData.class);
                Log.d(TAG, "Financial data loaded from storage");
            } else {
                financialData = new FinancialData();
                Log.d(TAG, "No financial data found, initialized empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading financial data", e);
            financialData = new FinancialData();
        }
    }

    private void saveFinancialToStorage() {
        try {
            String json = gson.toJson(financialData);
            prefs.edit().putString(FINANCIAL_KEY, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving financial data", e);
        }
    }

    public void saveFinancialData(FinancialData data) {
        this.financialData = data;
        saveFinancialToStorage();
    }

    public FinancialData getFinancialData() {
        if (financialData == null) {
            financialData = new FinancialData();
        }
        return financialData;
    }

    public boolean updateBudgetCategory(String categoryKey, double spent) {
        try {
            if (financialData != null && financialData.getCategoryBudgets() != null) {
                FinancialData.CategoryBudget category = financialData.getCategoryBudgets().get(categoryKey);
                if (category != null) {
                    category.setSpent(spent);
                    category.setPercentage((spent / category.getAllocated()) * 100);
                    updateOverallBudgetUtilization();
                    saveFinancialToStorage();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error updating budget category", e);
            return false;
        }
    }

    private void updateOverallBudgetUtilization() {
        if (financialData != null && financialData.getCategoryBudgets() != null) {
            double totalSpent = 0;
            double totalAllocated = 0;

            for (FinancialData.CategoryBudget category : financialData.getCategoryBudgets().values()) {
                totalSpent += category.getSpent();
                totalAllocated += category.getAllocated();
            }

            FinancialData.BudgetOverview budget = financialData.getBudgetOverview();
            if (budget != null) {
                budget.setUtilized(totalSpent);
                budget.setRemaining(budget.getAnnualBudget() - totalSpent);
                budget.setUtilizationRate((totalSpent / budget.getAnnualBudget()) * 100);
            }
        }
    }

    // ===== ADDITIONAL HELPER METHODS =====

    public List<Patient> getHighRiskPatientsForASHA(String ashaId) {
        List<Patient> highRiskPatients = new ArrayList<>();
        if (patients == null || ashaId == null) return highRiskPatients;
        
        for (Patient patient : patients) {
            if (ashaId.equals(patient.getAshaId()) && patient.isHighRisk()) {
                highRiskPatients.add(patient);
            }
        }
        return highRiskPatients;
    }

    // ===== UTILITY METHODS =====

    public int getPatientCountForUser(User user) {
        if (user == null || patients == null) return 0;

        switch (user.getRole()) {
            case "asha":
                return getPatientsForASHA(user.getId()).size();
            case "phcdoctor":
                return getPatientsForDoctor(user.getId()).size();
            default:
                return patients.size();
        }
    }

    public void clearAllPatients() {
        patients = new ArrayList<>();
        savePatientsToStorage();
    }

    public void resetToEmpty() {
        users = new ArrayList<>();
        patients = new ArrayList<>();
        inventoryItems = new ArrayList<>();
        staffList = new ArrayList<>();
        financialData = new FinancialData();
        
        saveUsersToStorage();
        savePatientsToStorage();
        saveInventoryToStorage();
        saveStaffToStorage();
        saveFinancialToStorage();
        
        Log.d(TAG, "All data reset to empty");
    }

    public String getStorageInfo() {
        return "Users: " + (users != null ? users.size() : 0) +
               ", Patients: " + (patients != null ? patients.size() : 0) +
               ", Inventory: " + (inventoryItems != null ? inventoryItems.size() : 0) +
               ", Staff: " + (staffList != null ? staffList.size() : 0);
    }

    /**
     * Force reload inventory from SharedPreferences to avoid stale singleton cache.
     */
    public void refreshInventoryFromStorage() {
        loadOrInitializeInventory();
        Log.d(TAG, "Inventory refreshed from storage: " + (inventoryItems != null ? inventoryItems.size() : 0) + " items");
    }

    /**
     * Force reload all data from SharedPreferences.
     */
    public void refreshAllFromStorage() {
        loadOrInitializeUsers();
        loadOrInitializePatients();
        loadOrInitializeInventory();
        loadOrInitializeStaff();
        loadOrInitializeFinancial();
        Log.d(TAG, "All data refreshed. " + getStorageInfo());
    }

    // Placeholder methods (to be implemented with actual features)
    public List<Object> getPendingReferrals() { return new ArrayList<>(); }
    public List<Object> getScheduledVisitsForUser(String userId) { return new ArrayList<>(); }
    public List<Object> getChildrenForASHA(String ashaId) { return new ArrayList<>(); }
}