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
    private static final int DATA_VERSION = 3; // Incremented for Firebase
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
    
    // Firebase helper
    private FirebaseHelper firebaseHelper;
    private boolean isOnline = false;

    // ✅ Listener so UI can react when Firebase pushes updates
    public interface OnDataChangedListener {
        void onPatientsChanged();
        void onUsersChanged();
    }
    private final java.util.List<OnDataChangedListener> dataListeners = new java.util.ArrayList<>();

    public void addDataListener(OnDataChangedListener listener) {
        if (!dataListeners.contains(listener)) dataListeners.add(listener);
    }
    public void removeDataListener(OnDataChangedListener listener) {
        dataListeners.remove(listener);
    }
    private void notifyPatientsChanged() {
        for (OnDataChangedListener l : dataListeners) { try { l.onPatientsChanged(); } catch (Exception ignored) {} }
    }
    private void notifyUsersChanged() {
        for (OnDataChangedListener l : dataListeners) { try { l.onUsersChanged(); } catch (Exception ignored) {} }
    }

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize Firebase separately to avoid circular dependency
        try {
            this.firebaseHelper = FirebaseHelper.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage());
            this.firebaseHelper = null;
        }

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
        
        // Start syncing with Firebase
        startFirebaseSync();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }
    
    private void startFirebaseSync() {
        if (firebaseHelper == null) {
            Log.w(TAG, "FirebaseHelper is null, skipping sync");
            return;
        }
        
        try {
            // Sync patients from Firebase
            firebaseHelper.syncPatients(new FirebaseHelper.PatientsCallback() {
                @Override
                public void onSuccess(List<Patient> firebasePatients) {
                    if (firebasePatients != null) {
                        isOnline = true;
                        if (!firebasePatients.isEmpty()) {
                            mergePatients(firebasePatients);
                            notifyPatientsChanged(); // ✅ Tell UI to refresh
                        }
                    }
                }
                
                @Override
                public void onFailure(String error) {
                    isOnline = false;
                    Log.w(TAG, "Firebase sync failed, using offline data: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting Firebase sync: " + e.getMessage());
            isOnline = false;
        }
        
        try {
            // Sync users from Firebase
            firebaseHelper.syncUsers(new FirebaseHelper.UsersCallback() {
                @Override
                public void onSuccess(List<User> firebaseUsers) {
                    if (firebaseUsers != null) {
                        isOnline = true;
                        if (!firebaseUsers.isEmpty()) {
                            mergeUsers(firebaseUsers);
                            // Also update staff list
                            updateStaffListFromUsers(firebaseUsers);
                        }
                    }
                }
                
                @Override
                public void onFailure(String error) {
                    isOnline = false;
                    Log.w(TAG, "Firebase user sync failed: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting user sync: " + e.getMessage());
            isOnline = false;
        }
        
        // Refresh users after a delay
        try {
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(() -> {
                refreshUsersFromFirebase();
            }, 2000);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling refresh: " + e.getMessage());
        }
    }
    
    private void mergePatients(List<Patient> firebasePatients) {
        if (patients == null) patients = new ArrayList<>();
        
        // Create a map of existing patients by ID
        Map<String, Patient> patientMap = new HashMap<>();
        for (Patient p : patients) {
            patientMap.put(p.getId(), p);
        }
        
        // Add or update from Firebase
        for (Patient fbPatient : firebasePatients) {
            patientMap.put(fbPatient.getId(), fbPatient);
        }
        
        // Convert back to list
        patients.clear();
        patients.addAll(patientMap.values());
        savePatientsToStorage();
        Log.d(TAG, "Merged " + patients.size() + " patients from Firebase");
    }
    
    private void mergeUsers(List<User> firebaseUsers) {
        if (users == null) users = new ArrayList<>();
        
        // Create a map of existing users by ID
        Map<String, User> userMap = new HashMap<>();
        for (User u : users) {
            userMap.put(u.getId(), u);
        }
        
        // Add or update from Firebase
        for (User fbUser : firebaseUsers) {
            userMap.put(fbUser.getId(), fbUser);
        }
        
        // Convert back to list
        users.clear();
        users.addAll(userMap.values());
        saveUsersToStorage();
        Log.d(TAG, "Merged " + users.size() + " users from Firebase");
    }
    
    public void refreshUsersFromFirebase() {
    if (firebaseHelper == null) {
        Log.w(TAG, "FirebaseHelper is null, cannot refresh");
        return;
    }
    
    Log.d(TAG, "Refreshing users from Firebase...");
    
    firebaseHelper.syncUsers(new FirebaseHelper.UsersCallback() {
        @Override
        public void onSuccess(List<User> firebaseUsers) {
            if (firebaseUsers != null && !firebaseUsers.isEmpty()) {
                Log.d(TAG, "Received " + firebaseUsers.size() + " users from Firebase");
                
                // Merge Firebase users with local users
                mergeUsers(firebaseUsers);
                
                // Force update staff list from these users
                staffList = new ArrayList<>();
                for (User user : users) {
                    staffList.add(convertUserToStaff(user));
                }
                saveStaffToStorage();
                
                Log.d(TAG, "Staff list updated from Firebase: " + staffList.size() + " staff members");
            } else {
                Log.w(TAG, "No users received from Firebase");
            }
        }
        
        @Override
        public void onFailure(String error) {
            Log.e(TAG, "Failed to refresh users: " + error);
        }
    });
}
    
    public void refreshPatientsFromFirebase() {
    if (firebaseHelper == null) {
        Log.w(TAG, "FirebaseHelper is null, cannot refresh patients");
        return;
    }
    
    Log.d(TAG, "Refreshing patients from Firebase...");
    
    firebaseHelper.syncPatients(new FirebaseHelper.PatientsCallback() {
        @Override
        public void onSuccess(List<Patient> firebasePatients) {
            if (firebasePatients != null && !firebasePatients.isEmpty()) {
                Log.d(TAG, "Received " + firebasePatients.size() + " patients from Firebase");
                
                // Use existing mergePatients method
                if (patients == null) patients = new ArrayList<>();
                
                // Create a map of existing patients by ID
                java.util.Map<String, Patient> patientMap = new java.util.HashMap<>();
                for (Patient p : patients) {
                    patientMap.put(p.getId(), p);
                }
                
                // Add or update from Firebase
                for (Patient fbPatient : firebasePatients) {
                    patientMap.put(fbPatient.getId(), fbPatient);
                }
                
                // Convert back to list
                patients.clear();
                patients.addAll(patientMap.values());
                savePatientsToStorage();
                
                Log.d(TAG, "Merged " + patients.size() + " patients from Firebase");
            } else {
                Log.d(TAG, "No patients received from Firebase");
            }
        }
        
        @Override
        public void onFailure(String error) {
            Log.e(TAG, "Failed to refresh patients: " + error);
        }
    });
}
    private void updateStaffListFromUsers(List<User> userList) {
        if (userList == null) return;
        
        // Clear existing staff list
        if (staffList == null) {
            staffList = new ArrayList<>();
        } else {
            staffList.clear();
        }
        
        // Convert each user to staff
        for (User user : userList) {
            Staff staff = convertUserToStaff(user);
            staffList.add(staff);
        }
        
        saveStaffToStorage();
        Log.d(TAG, "Staff list updated from users: " + staffList.size() + " staff members");
    }
    
    private Staff convertUserToStaff(User user) {
    Log.d(TAG, "Converting user to staff: " + user.getName() + " (" + user.getId() + ")");
    
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
    staff.setStatus("active");
    
    // Add role-specific display info
    if ("phcdoctor".equals(user.getRole())) {
        staff.setQualification("Doctor");
    } else if ("phcnurse".equals(user.getRole())) {
        staff.setQualification("Nurse");
    } else if ("asha".equals(user.getRole())) {
        staff.setQualification("ASHA Worker");
    } else if ("phcadmin".equals(user.getRole())) {
        staff.setQualification("Administrator");
    }
    
    return staff;
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
        
        // Try to sync to Firebase if available
        if (firebaseHelper != null) {
            try {
                com.google.firebase.database.DatabaseReference userRef = 
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users").child(user.getId());
                userRef.setValue(user);
                Log.d(TAG, "User synced to Firebase: " + user.getName());
            } catch (Exception e) {
                Log.e(TAG, "Failed to sync user to Firebase: " + e.getMessage());
            }
        }
        
        // ALSO ADD TO STAFF LIST
        addUserToStaff(user);
        
        Log.d(TAG, "User registered successfully: " + user.getName());
        return true;
    }

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
            staff.setQualification("MBBS");
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

    public boolean addPatient(Patient patient) {
        try {
            if (patients == null) patients = new ArrayList<>();
            patients.add(patient);
            savePatientsToStorage();
            
            if (firebaseHelper != null) {
                firebaseHelper.savePatient(patient);
            }
            
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
                    
                    if (firebaseHelper != null) {
                        firebaseHelper.updatePatient(updatedPatient);
                    }
                    
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
                    
                    if (firebaseHelper != null) {
                        firebaseHelper.deletePatient(patientId);
                    }
                    
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
    // First, make sure users are loaded
    if (users == null || users.isEmpty()) {
        Log.w(TAG, "No users found, cannot create staff list");
        return new ArrayList<>();
    }
    
    // Create fresh staff list from users
    List<Staff> freshStaffList = new ArrayList<>();
    for (User user : users) {
        Staff staff = convertUserToStaff(user);
        freshStaffList.add(staff);
        Log.d(TAG, "Added staff: " + user.getName() + " (" + user.getRole() + ")");
    }
    
    // Update the stored staff list
    staffList = freshStaffList;
    saveStaffToStorage();
    
    Log.d(TAG, "getStaffList returning " + staffList.size() + " staff members");
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

    public void refreshInventoryFromStorage() {
        loadOrInitializeInventory();
        Log.d(TAG, "Inventory refreshed from storage: " + (inventoryItems != null ? inventoryItems.size() : 0) + " items");
    }

    public void refreshAllFromStorage() {
        loadOrInitializeUsers();
        loadOrInitializePatients();
        loadOrInitializeInventory();
        loadOrInitializeStaff();
        loadOrInitializeFinancial();
        Log.d(TAG, "All data refreshed. " + getStorageInfo());
    }

    // Placeholder methods
    public List<Object> getPendingReferrals() { return new ArrayList<>(); }
    public List<Object> getScheduledVisitsForUser(String userId) { return new ArrayList<>(); }
    public List<Object> getChildrenForASHA(String ashaId) { return new ArrayList<>(); }
}