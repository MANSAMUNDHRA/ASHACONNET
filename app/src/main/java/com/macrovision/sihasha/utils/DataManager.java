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
    private FinancialData financialData;
    private static final String FINANCIAL_KEY = "financial_data";

    private Context context;
    private List<User> users;
    private List<Patient> patients;
    private static DataManager instance;
    private Gson gson;
    private SharedPreferences prefs;

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize users (these don't change much, so keep hardcoded)
        initializeUsers();

        // Load patients from persistent storage or initialize with demo data
        loadOrInitializePatients();

        loadOrInitializeInventory();
        loadOrInitializeFinancial();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    private void initializeUsers() {
        users = new ArrayList<>();

        // ASHA Worker
        User asha = new User("ASHA001", "Priya Devi", "asha", "9876543210",
                "Rampur", "Baruipur", "South 24 Parganas", "West Bengal",
                "PHC001", "password123");
        asha.setPerformance(new User.Performance(25, 23, 92));
        users.add(asha);

        // PHC Doctor
        User doctor = new User("PHC001", "Dr. Rajesh Kumar", "phcdoctor", "9876543200",
                "", "Baruipur", "South 24 Parganas", "West Bengal",
                "PHC001", "password123");
        users.add(doctor);

        // PHC Nurse
        User nurse = new User("PHC002", "Sister Meena Roy", "phcnurse", "9876543201",
                "", "Baruipur", "South 24 Parganas", "West Bengal",
                "PHC001", "password123");
        users.add(nurse);

        // PHC Admin
        User admin = new User("ADMIN001", "Dr. Amit Sharma", "phcadmin", "9876543100",
                "", "", "South 24 Parganas", "West Bengal",
                "", "admin123");
        users.add(admin);
    }

    private void loadOrInitializePatients() {
        Log.d(TAG, "Loading patients from persistent storage...");

        try {
            // Try to load patients from SharedPreferences
            patients = loadPatientsFromStorage();

            if (patients == null || patients.isEmpty()) {
                Log.d(TAG, "No saved patients found, initializing with demo data");
                initializeDemoPatients();
                savePatientsToStorage(); // Save demo patients as initial data
            } else {
                Log.d(TAG, "Loaded " + patients.size() + " patients from storage");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading patients, falling back to demo data", e);
            initializeDemoPatients();
            savePatientsToStorage();
        }
    }

    private void loadOrInitializeFinancial() {
        Log.d(TAG, "Loading financial data from persistent storage...");

        try {
            financialData = loadFinancialFromStorage();

            if (financialData == null) {
                Log.d(TAG, "No saved financial data found, initializing with demo data");
                initializeDemoFinancialData();
                saveFinancialToStorage();
            } else {
                Log.d(TAG, "Financial data loaded successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading financial data, falling back to demo data", e);
            initializeDemoFinancialData();
            saveFinancialToStorage();
        }
    }

    // Initialize demo financial data
    private void initializeDemoFinancialData() {
        Log.d(TAG, "Initializing demo financial data...");

        financialData = new FinancialData();

        // Budget Overview
        FinancialData.BudgetOverview budgetOverview = new FinancialData.BudgetOverview(
                15000000, // Annual budget: ₹1.5 Crore
                8500000,  // Utilized: ₹85 Lakh
                6500000,  // Remaining: ₹65 Lakh
                56.7      // Utilization rate: 56.7%
        );
        financialData.setBudgetOverview(budgetOverview);

        // Category Budgets
        Map<String, FinancialData.CategoryBudget> categoryBudgets = new HashMap<>();

        categoryBudgets.put("staff", new FinancialData.CategoryBudget(
                "Staff Salaries", 8000000, 4800000, 60, "Salaries and wages for all staff members"));

        categoryBudgets.put("medicines", new FinancialData.CategoryBudget(
                "Medicines & Supplies", 3000000, 1800000, 60, "Medical supplies, drugs, and consumables"));

        categoryBudgets.put("equipment", new FinancialData.CategoryBudget(
                "Medical Equipment", 2000000, 900000, 45, "Medical devices and equipment maintenance"));

        categoryBudgets.put("infrastructure", new FinancialData.CategoryBudget(
                "Infrastructure", 1500000, 700000, 47, "Building maintenance and utilities"));

        categoryBudgets.put("training", new FinancialData.CategoryBudget(
                "Training & Development", 500000, 300000, 60, "Staff training and capacity building"));

        financialData.setCategoryBudgets(categoryBudgets);
        financialData.setFinancialYear("2025-26");

        Log.d(TAG, "Created " + categoryBudgets.size() + " budget categories:");
        for (Map.Entry<String, FinancialData.CategoryBudget> entry : categoryBudgets.entrySet()) {
            FinancialData.CategoryBudget cat = entry.getValue();
            Log.d(TAG, "  - " + entry.getKey() + ": " + cat.getCategoryName() +
                    " (₹" + (cat.getAllocated()/100000) + "L allocated)");
        }
    }

    // Staff Management Methods
    public List<Staff> getStaffList() {
        try {
            String staffJson = prefs.getString("staff_data", null);
            if (staffJson != null) {
                Type listType = new TypeToken<List<Staff>>(){}.getType();
                return gson.fromJson(staffJson, listType);
            } else {
                return initializeDemoStaffData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading staff data", e);
            return new ArrayList<>();
        }
    }

    public void addStaffMember(Staff staff) {
        try {
            List<Staff> staffList = getStaffList();
            staffList.add(staff);
            saveStaffToStorage(staffList);
            Log.d(TAG, "Staff member added: " + staff.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error adding staff member", e);
        }
    }

    public void updateStaffMember(Staff staff) {
        try {
            List<Staff> staffList = getStaffList();
            for (int i = 0; i < staffList.size(); i++) {
                if (staffList.get(i).getId().equals(staff.getId())) {
                    staffList.set(i, staff);
                    break;
                }
            }
            saveStaffToStorage(staffList);
            Log.d(TAG, "Staff member updated: " + staff.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error updating staff member", e);
        }
    }

    private void saveStaffToStorage(List<Staff> staffList) {
        try {
            String staffJson = gson.toJson(staffList);
            prefs.edit().putString("staff_data", staffJson).apply();
            Log.d(TAG, "Staff data saved to storage");
        } catch (Exception e) {
            Log.e(TAG, "Error saving staff data", e);
        }
    }

    private List<Staff> initializeDemoStaffData() {
        Log.d(TAG, "Initializing demo staff data...");

        List<Staff> demoStaff = new ArrayList<>();

        // ASHA Workers
        Staff asha1 = new Staff("ASHA001", "Priya Devi", "asha", "9876543210", "Rampur", "South 24 Parganas");
        asha1.setBlock("Baruipur");
        asha1.setState("West Bengal");
        asha1.setPhcId("PHC001");
        asha1.setPhcName("Baruipur PHC");
        asha1.setAssignedPopulation("1200");
        asha1.setAssignedFamilies("240");
        asha1.setJoiningDate("2020-01-15");
        asha1.setLastTraining("2024-08-15");
        asha1.setPerformance(new Staff.Performance(25, 23, 92.0, 45, 3));
        demoStaff.add(asha1);

        // PHC Doctor
        Staff doctor1 = new Staff("PHC001", "Dr. Rajesh Kumar", "phcdoctor", "9876543200", "", "South 24 Parganas");
        doctor1.setPhcName("Baruipur PHC");
        doctor1.setQualification("MBBS");
        doctor1.setExperience("8 years");
        doctor1.setSpecialization("General Medicine");
        doctor1.setPerformance(new Staff.Performance(150, 145, 96.7, 180, 2));
        demoStaff.add(doctor1);

        // PHC Nurse
        Staff nurse1 = new Staff("PHC002", "Sister Meena Roy", "phcnurse", "9876543201", "", "South 24 Parganas");
        nurse1.setPhcName("Baruipur PHC");
        nurse1.setQualification("GNM");
        nurse1.setExperience("12 years");
        nurse1.setResponsibilities("Immunization, ANC, PNC, Training");
        nurse1.setPerformance(new Staff.Performance(100, 98, 98.0, 120, 4));
        demoStaff.add(nurse1);

        // PHC Admin
        Staff admin1 = new Staff("ADMIN001", "Dr. Amit Sharma", "phcadmin", "9876543100", "", "South 24 Parganas");
        admin1.setEmail("amit.sharma@health.wb.gov.in");
        admin1.setDesignation("Chief Medical Officer");
        admin1.setQualification("MBBS, MPH");
        admin1.setExperience("15 years");
        admin1.setManagedPHCs("PHC001, PHC002, PHC003");
        demoStaff.add(admin1);

        saveStaffToStorage(demoStaff);
        return demoStaff;
    }

    // Storage methods
    private FinancialData loadFinancialFromStorage() {
        try {
            String json = prefs.getString(FINANCIAL_KEY, null);

            if (json != null && !json.isEmpty()) {
                Log.d(TAG, "Found saved financial data, deserializing...");
                FinancialData loadedData = gson.fromJson(json, FinancialData.class);

                if (loadedData != null) {
                    Log.d(TAG, "Successfully loaded financial data");
                    return loadedData;
                }
            }

            Log.d(TAG, "No valid financial data found in storage");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error loading financial data from storage", e);
            return null;
        }
    }

    private void saveFinancialToStorage() {
        try {
            Log.d(TAG, "Saving financial data to storage...");

            if (financialData != null) {
                String json = gson.toJson(financialData);
                prefs.edit().putString(FINANCIAL_KEY, json).apply();
                Log.d(TAG, "Financial data saved successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving financial data to storage", e);
        }
    }

    // Public methods for accessing financial data
    public FinancialData getFinancialData() {
        if (financialData == null) {
            initializeDemoFinancialData();
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

                    // Update overall budget
                    updateOverallBudgetUtilization();
                    saveFinancialToStorage();

                    Log.d(TAG, "Budget category updated: " + categoryKey);
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
            budget.setUtilized(totalSpent);
            budget.setRemaining(budget.getAnnualBudget() - totalSpent);
            budget.setUtilizationRate((totalSpent / budget.getAnnualBudget()) * 100);
        }
    }


    private void initializeDemoPatients() {
        Log.d(TAG, "Initializing demo patients...");
        patients = new ArrayList<>();

        Patient patient1 = new Patient("PAT001", "Meera Devi", 25, "9876543210",
                "Rampur", "pregnant", "ASHA001", "PHC001", false);
        patient1.setHusbandName("Rajesh Kumar");
        patient1.setEddDate("2025-01-06");
        patient1.setBloodGroup("O+");
        patient1.setRegistrationDate("2024-10-01");
        patients.add(patient1);

        Patient patient2 = new Patient("PAT002", "Kavita Singh", 28, "9876543212",
                "Shyampur", "delivered", "ASHA001", "PHC001", false);
        patient2.setHusbandName("Amit Singh");
        patient2.setBloodGroup("A+");
        patient2.setRegistrationDate("2024-09-15");
        patients.add(patient2);

        Patient patient3 = new Patient("PAT003", "Sunita Kumari", 22, "9876543213",
                "Rampur", "pregnant", "ASHA001", "PHC001", true);
        patient3.setHusbandName("Ravi Kumar");
        patient3.setEddDate("2025-02-15");
        patient3.setBloodGroup("B+");
        patient3.setRegistrationDate("2024-10-05");
        patients.add(patient3);

        Log.d(TAG, "Demo patients initialized: " + patients.size());
    }

    // ===== PERSISTENCE METHODS =====

    private List<Patient> loadPatientsFromStorage() {
        try {
            String json = prefs.getString(PATIENTS_KEY, null);

            if (json != null && !json.isEmpty()) {
                Log.d(TAG, "Found saved patient data, deserializing...");
                Type listType = new TypeToken<List<Patient>>(){}.getType();
                List<Patient> loadedPatients = gson.fromJson(json, listType);

                if (loadedPatients != null) {
                    Log.d(TAG, "Successfully loaded " + loadedPatients.size() + " patients");
                    return loadedPatients;
                }
            }

            Log.d(TAG, "No valid patient data found in storage");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error loading patients from storage", e);
            return null;
        }
    }

    private void savePatientsToStorage() {
        try {
            Log.d(TAG, "Saving " + (patients != null ? patients.size() : 0) + " patients to storage...");

            if (patients != null) {
                String json = gson.toJson(patients);
                prefs.edit().putString(PATIENTS_KEY, json).apply();
                Log.d(TAG, "Patients saved successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving patients to storage", e);
        }
    }

    // ===== PATIENT CRUD METHODS =====

    public boolean addPatient(Patient patient) {
        try {
            Log.d(TAG, "Adding patient: " + patient.getName());

            if (patients == null) {
                patients = new ArrayList<>();
            }

            patients.add(patient);
            savePatientsToStorage(); // ✅ SAVE AFTER ADDING

            Log.d(TAG, "Patient added successfully. Total patients: " + patients.size());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error adding patient", e);
            return false;
        }
    }

    public boolean updatePatient(Patient updatedPatient) {
        try {
            Log.d(TAG, "Updating patient: " + updatedPatient.getName());

            if (patients == null) {
                Log.w(TAG, "Patients list is null");
                return false;
            }

            // Find and replace the patient
            for (int i = 0; i < patients.size(); i++) {
                if (patients.get(i).getId().equals(updatedPatient.getId())) {
                    patients.set(i, updatedPatient);
                    savePatientsToStorage(); // ✅ SAVE AFTER UPDATING

                    Log.d(TAG, "Patient updated successfully: " + updatedPatient.getName());
                    return true;
                }
            }

            Log.w(TAG, "Patient not found for update: " + updatedPatient.getId());
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error updating patient", e);
            return false;
        }
    }

    public boolean deletePatient(String patientId) {
        try {
            Log.d(TAG, "Deleting patient by ID: " + patientId);

            if (patients == null) {
                Log.w(TAG, "Patients list is null");
                return false;
            }

            for (int i = 0; i < patients.size(); i++) {
                if (patients.get(i).getId().equals(patientId)) {
                    Patient deletedPatient = patients.remove(i);
                    savePatientsToStorage(); // ✅ SAVE AFTER DELETING

                    Log.d(TAG, "Patient deleted successfully: " + deletedPatient.getName());
                    return true;
                }
            }

            Log.w(TAG, "Patient not found for deletion: " + patientId);
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting patient", e);
            return false;
        }
    }

    public Patient getPatientById(String patientId) {
        try {
            Log.d(TAG, "Getting patient by ID: " + patientId);

            if (patients == null) {
                Log.w(TAG, "Patients list is null");
                return null;
            }

            for (Patient patient : patients) {
                if (patient.getId().equals(patientId)) {
                    Log.d(TAG, "Patient found: " + patient.getName());
                    return patient;
                }
            }

            Log.w(TAG, "Patient not found with ID: " + patientId);
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting patient by ID", e);
            return null;
        }
    }

    // ===== EXISTING METHODS (Updated to handle null checks) =====

    public User authenticateUser(String userId, String role, String password) {
        if (users == null) return null;

        for (User user : users) {
            if (user.getId().equals(userId) && user.getRole().equals(role) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public int getPatientCountForUser(User user) {
        if (user == null || patients == null) return 0;

        if (user.getRole().equals("asha")) {
            return getPatientsForASHA(user.getId()).size();
        }
        return patients.size();
    }

    public List<Patient> getPatientsForASHA(String ashaId) {
        List<Patient> ashaPatients = new ArrayList<>();

        if (patients == null || ashaId == null) {
            return ashaPatients;
        }

        for (Patient patient : patients) {
            if (ashaId.equals(patient.getAshaId())) {
                ashaPatients.add(patient);
            }
        }
        return ashaPatients;
    }

    public List<Patient> getPregnantPatientsForASHA(String ashaId) {
        List<Patient> pregnantPatients = new ArrayList<>();

        if (patients == null || ashaId == null) {
            return pregnantPatients;
        }

        for (Patient patient : patients) {
            if (ashaId.equals(patient.getAshaId()) && "pregnant".equals(patient.getPregnancyStatus())) {
                pregnantPatients.add(patient);
            }
        }
        return pregnantPatients;
    }

    public List<Patient> getHighRiskPatientsForASHA(String ashaId) {
        List<Patient> highRiskPatients = new ArrayList<>();

        if (patients == null || ashaId == null) {
            return highRiskPatients;
        }

        for (Patient patient : patients) {
            if (ashaId.equals(patient.getAshaId()) && patient.isHighRisk()) {
                highRiskPatients.add(patient);
            }
        }
        return highRiskPatients;
    }

    public List<Patient> getAllPatients() {
        if (patients == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(patients);
    }

    public List<User> getASHAWorkers() {
        List<User> ashaWorkers = new ArrayList<>();

        if (users == null) {
            return ashaWorkers;
        }

        for (User user : users) {
            if ("asha".equals(user.getRole())) {
                ashaWorkers.add(user);
            }
        }
        return ashaWorkers;
    }

    // Placeholder methods for other data operations
    public List<Object> getChildrenForASHA(String ashaId) {
        return new ArrayList<>();
    }

    public List<Object> getScheduledVisitsForUser(String userId) {
        return new ArrayList<>();
    }

    public List<Object> getPendingReferrals() {
        return new ArrayList<>();
    }

    public List<Object> getScheduledTrainingSessions() {
        return new ArrayList<>();
    }

    public List<Object> getHighPriorityReferrals() {
        return new ArrayList<>();
    }

    public List<Object> getAllVisits() {
        return new ArrayList<>();
    }

    public List<Object> getActiveEmergencyAlerts() {
        return new ArrayList<>();
    }

    // ===== UTILITY METHODS =====

    /**
     * Clear all saved patient data (useful for testing or reset functionality)
     */
    public void clearAllPatients() {
        Log.d(TAG, "Clearing all patient data");
        patients = new ArrayList<>();
        savePatientsToStorage();
    }

    /**
     * Reset to demo data (useful for testing)


    public void resetToDemo() {
        Log.d(TAG, "Resetting to demo data");
        initializeDemoPatients();
        savePatientsToStorage();
    }

    /**
     * Get storage stats for debugging
     */
    public void resetFinancialDataDemo() {
        Log.d(TAG, "Force resetting financial data to demo");

        // Clear any existing data
        financialData = null;
        prefs.edit().remove("financial_data").apply();

        // Re-initialize with demo data
        initializeDemoFinancialData();
        saveFinancialToStorage();

        Log.d(TAG, "Financial data reset complete - " +
                financialData.getCategoryBudgets().size() + " categories created");
    }

    public String getStorageInfo() {
        String json = prefs.getString(PATIENTS_KEY, "");
        return "Storage size: " + json.length() + " characters, Patients in memory: " +
                (patients != null ? patients.size() : 0);
    }

    // Add this to your DataManager.java class

    private List<InventoryItem> inventoryItems;
    private static final String INVENTORY_KEY = "inventory_data";

    // Initialize inventory in the constructor
    private void loadOrInitializeInventory() {
        Log.d(TAG, "Loading inventory from persistent storage...");

        try {
            inventoryItems = loadInventoryFromStorage();

            if (inventoryItems == null || inventoryItems.isEmpty()) {
                Log.d(TAG, "No saved inventory found, initializing with demo data");
                initializeDemoInventory();
                saveInventoryToStorage();
            } else {
                Log.d(TAG, "Loaded " + inventoryItems.size() + " inventory items from storage");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading inventory, falling back to demo data", e);
            initializeDemoInventory();
            saveInventoryToStorage();
        }
    }

    private void initializeDemoInventory() {
        Log.d(TAG, "Initializing demo inventory...");
        inventoryItems = new ArrayList<>();

        // Medicines
        InventoryItem ifa = new InventoryItem("MED001", "IFA Tablets", "Medicine",
                5000, 1000, "2025-12-31", "IFA2024001", "Government Supply", 2.5);
        inventoryItems.add(ifa);

        InventoryItem calcium = new InventoryItem("MED002", "Calcium Tablets", "Medicine",
                3000, 500, "2025-10-31", "CAL2024002", "Government Supply", 3.0);
        inventoryItems.add(calcium);

        InventoryItem paracetamol = new InventoryItem("MED003", "Paracetamol 500mg", "Medicine",
                2500, 300, "2025-11-30", "PARA2024003", "Government Supply", 1.2);
        inventoryItems.add(paracetamol);

        // Vaccines
        InventoryItem bcg = new InventoryItem("VAC001", "BCG Vaccine", "Vaccine",
                500, 50, "2025-03-31", "BCG2024001", "Serum Institute", 25.0);
        bcg.setStorageTemp("2-8°C");
        bcg.setManufacturer("Serum Institute");
        inventoryItems.add(bcg);

        InventoryItem dpt = new InventoryItem("VAC002", "DPT Vaccine", "Vaccine",
                300, 40, "2025-04-30", "DPT2024002", "Serum Institute", 35.0);
        dpt.setStorageTemp("2-8°C");
        dpt.setManufacturer("Serum Institute");
        inventoryItems.add(dpt);

        // Supplies
        InventoryItem syringes = new InventoryItem("SUP001", "Disposable Syringes", "Supply",
                10000, 2000, "2026-01-31", "SYR2024001", "Medical Supplies Co.", 0.5);
        inventoryItems.add(syringes);

        InventoryItem gloves = new InventoryItem("SUP002", "Surgical Gloves", "Supply",
                5000, 1000, "2025-08-31", "GLOVE2024001", "Medical Supplies Co.", 2.0);
        inventoryItems.add(gloves);

        // Add some low stock items for demonstration
        InventoryItem lowStock = new InventoryItem("MED004", "Folic Acid", "Medicine",
                25, 100, "2025-09-30", "FOLIC2024001", "Government Supply", 1.8);
        inventoryItems.add(lowStock);

        Log.d(TAG, "Demo inventory initialized: " + inventoryItems.size() + " items");
    }

    private List<InventoryItem> loadInventoryFromStorage() {
        try {
            String json = prefs.getString(INVENTORY_KEY, null);

            if (json != null && !json.isEmpty()) {
                Log.d(TAG, "Found saved inventory data, deserializing...");
                Type listType = new TypeToken<List<InventoryItem>>(){}.getType();
                List<InventoryItem> loadedItems = gson.fromJson(json, listType);

                if (loadedItems != null) {
                    Log.d(TAG, "Successfully loaded " + loadedItems.size() + " inventory items");
                    return loadedItems;
                }
            }

            Log.d(TAG, "No valid inventory data found in storage");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error loading inventory from storage", e);
            return null;
        }
    }

    private void saveInventoryToStorage() {
        try {
            Log.d(TAG, "Saving " + (inventoryItems != null ? inventoryItems.size() : 0) + " inventory items to storage...");

            if (inventoryItems != null) {
                String json = gson.toJson(inventoryItems);
                prefs.edit().putString(INVENTORY_KEY, json).apply();
                Log.d(TAG, "Inventory saved successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving inventory to storage", e);
        }
    }

    // Inventory CRUD methods
    public List<InventoryItem> getAllInventoryItems() {
        if (inventoryItems == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(inventoryItems);
    }

    public List<InventoryItem> getInventoryByCategory(String category) {
        List<InventoryItem> filtered = new ArrayList<>();
        if (inventoryItems != null) {
            for (InventoryItem item : inventoryItems) {
                if (category.equals(item.getCategory())) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }

    public List<InventoryItem> getLowStockItems() {
        List<InventoryItem> lowStock = new ArrayList<>();
        if (inventoryItems != null) {
            for (InventoryItem item : inventoryItems) {
                if (item.isLowStock()) {
                    lowStock.add(item);
                }
            }
        }
        return lowStock;
    }

    public boolean addInventoryItem(InventoryItem item) {
        try {
            Log.d(TAG, "Adding inventory item: " + item.getName());

            if (inventoryItems == null) {
                inventoryItems = new ArrayList<>();
            }

            inventoryItems.add(item);
            saveInventoryToStorage();

            Log.d(TAG, "Inventory item added successfully. Total items: " + inventoryItems.size());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error adding inventory item", e);
            return false;
        }
    }

    public boolean updateInventoryItem(InventoryItem updatedItem) {
        try {
            Log.d(TAG, "Updating inventory item: " + updatedItem.getName());

            if (inventoryItems == null) {
                Log.w(TAG, "Inventory list is null");
                return false;
            }

            for (int i = 0; i < inventoryItems.size(); i++) {
                if (inventoryItems.get(i).getId().equals(updatedItem.getId())) {
                    inventoryItems.set(i, updatedItem);
                    saveInventoryToStorage();

                    Log.d(TAG, "Inventory item updated successfully: " + updatedItem.getName());
                    return true;
                }
            }

            Log.w(TAG, "Inventory item not found for update: " + updatedItem.getId());
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error updating inventory item", e);
            return false;
        }
    }

    public boolean deleteInventoryItem(String itemId) {
        try {
            Log.d(TAG, "Deleting inventory item by ID: " + itemId);

            if (inventoryItems == null) {
                Log.w(TAG, "Inventory list is null");
                return false;
            }

            for (int i = 0; i < inventoryItems.size(); i++) {
                if (inventoryItems.get(i).getId().equals(itemId)) {
                    InventoryItem deletedItem = inventoryItems.remove(i);
                    saveInventoryToStorage();

                    Log.d(TAG, "Inventory item deleted successfully: " + deletedItem.getName());
                    return true;
                }
            }

            Log.w(TAG, "Inventory item not found for deletion: " + itemId);
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting inventory item", e);
            return false;
        }
    }

    public InventoryItem getInventoryItemById(String itemId) {
        if (inventoryItems != null) {
            for (InventoryItem item : inventoryItems) {
                if (item.getId().equals(itemId)) {
                    return item;
                }
            }
        }
        return null;
    }
}
