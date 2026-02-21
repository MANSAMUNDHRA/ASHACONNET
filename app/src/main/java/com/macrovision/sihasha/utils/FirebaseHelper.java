package com.macrovision.sihasha.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Context context;
    
    // Listeners to keep track
    private ValueEventListener patientsListener;
    private ValueEventListener usersListener;
    
    // Singleton instance
    private static FirebaseHelper instance;
    
    private FirebaseHelper(Context context) {
        this.context = context.getApplicationContext();
        this.mAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }
    
    public static synchronized FirebaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context);
        }
        return instance;
    }
    
    // ===== AUTHENTICATION =====
    
    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }
    
    public void loginUser(String userId, String password, AuthCallback callback) {
        // Use email format for Firebase Auth
        String email = userId + "@asha-connect.local";
        
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Get user data from Realtime Database
                    mDatabase.child("users").child(userId).get()
                        .addOnCompleteListener(userTask -> {
                            if (userTask.isSuccessful() && userTask.getResult().exists()) {
                                User user = userTask.getResult().getValue(User.class);
                                callback.onSuccess(user);
                            } else {
                                callback.onFailure("User data not found in database");
                            }
                        });
                } else {
                    callback.onFailure(task.getException().getMessage());
                }
            });
    }
    
    public void registerUser(User user, String password, AuthCallback callback) {
        String email = user.getId() + "@asha-connect.local";
        
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Save user data to Realtime Database
                    mDatabase.child("users").child(user.getId()).setValue(user)
                        .addOnCompleteListener(saveTask -> {
                            if (saveTask.isSuccessful()) {
                                callback.onSuccess(user);
                            } else {
                                callback.onFailure("Failed to save user data");
                            }
                        });
                } else {
                    callback.onFailure(task.getException().getMessage());
                }
            });
    }
    
    // ===== PATIENT SYNC =====
    
    public interface PatientsCallback {
        void onSuccess(List<Patient> patients);
        void onFailure(String error);
    }
    
    public void syncPatients(PatientsCallback callback) {
        // Remove existing listener if any
        if (patientsListener != null) {
            mDatabase.child("patients").removeEventListener(patientsListener);
        }
        
        patientsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Patient> patients = new ArrayList<>();
                for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                    Patient patient = patientSnapshot.getValue(Patient.class);
                    if (patient != null) {
                        patients.add(patient);
                        Log.d(TAG, "Patient from Firebase: " + patient.getName());
                    }
                }
                Log.d(TAG, "Synced " + patients.size() + " patients from Firebase");
                callback.onSuccess(patients);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Patient sync cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        };
        
        mDatabase.child("patients").addValueEventListener(patientsListener);
    }
    
    public void savePatient(Patient patient) {
        mDatabase.child("patients").child(patient.getId()).setValue(patient)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Patient saved to Firebase: " + patient.getName());
                } else {
                    Log.e(TAG, "Failed to save patient", task.getException());
                }
            });
    }
    
    public void updatePatient(Patient patient) {
        mDatabase.child("patients").child(patient.getId()).setValue(patient)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Patient updated in Firebase: " + patient.getName());
                } else {
                    Log.e(TAG, "Failed to update patient", task.getException());
                }
            });
    }
    
    public void deletePatient(String patientId) {
        mDatabase.child("patients").child(patientId).removeValue()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Patient deleted from Firebase: " + patientId);
                } else {
                    Log.e(TAG, "Failed to delete patient", task.getException());
                }
            });
    }
    
    // ===== USER SYNC =====
    
    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onFailure(String error);
    }
    
    public void syncUsers(UsersCallback callback) {
        // Remove existing listener if any
        if (usersListener != null) {
            mDatabase.child("users").removeEventListener(usersListener);
        }
        
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        users.add(user);
                    }
                }
                Log.d(TAG, "Synced " + users.size() + " users from Firebase");
                callback.onSuccess(users);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "User sync cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        };
        
        mDatabase.child("users").addValueEventListener(usersListener);
    }
    
    // ===== LOGOUT =====
    
    public void logout() {
        mAuth.signOut();
    }
    
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }
    
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Extract user ID from email (remove @asha-connect.local)
            return user.getEmail().replace("@asha-connect.local", "");
        }
        return null;
    }
    
    public void removeListeners() {
        if (patientsListener != null) {
            mDatabase.child("patients").removeEventListener(patientsListener);
        }
        if (usersListener != null) {
            mDatabase.child("users").removeEventListener(usersListener);
        }
    }
}