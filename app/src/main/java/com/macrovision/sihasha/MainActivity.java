package com.macrovision.sihasha;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editUserId;
    private EditText editPassword;
    private Spinner spinnerRole, spinnerLanguage;
    private Button btnLogin;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize components
        initializeViews();
        setupRoleSpinner();
        setupLanguageSpinner();
        setupEventListeners();

        // Initialize data and preferences
        dataManager = DataManager.getInstance(this);
        prefsManager = new SharedPrefsManager(this);
        Log.d("Dashboard", "Patients loaded: " + DataManager.getInstance(this).getAllPatients().size());

        // Check if user is already logged in
        if (prefsManager.isLoggedIn()) {
            navigateToDashboard();
        }
    }

    private void initializeViews() {
        editUserId = findViewById(R.id.edit_user_id);
        editPassword = findViewById(R.id.edit_password);
        spinnerRole = findViewById(R.id.spinner_role);
        btnLogin = findViewById(R.id.btn_login);
        spinnerLanguage=findViewById(R.id.spinner_language);
    }

    private void setupRoleSpinner() {
        List<String> roles = Arrays.asList(
                "Select Role",
                "ASHA Worker",
                "PHC Doctor",
                "PHC Nurse",
                "PHC Administrator"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }
    private void setupLanguageSpinner() {
        List<String> lang = Arrays.asList(
                "English",
                "हिन्दी",
                "ଓଡିଆ",
                "বাংলা"
        );

        ArrayAdapter<String> adapter12 = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                lang
        );
        adapter12.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter12);
    }

    private void setupEventListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });
    }

    private void handleLogin() {
        String userId = editUserId.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String selectedRole = spinnerRole.getSelectedItem().toString();

        // Validate inputs
        if (userId.isEmpty() || password.isEmpty() || selectedRole.equals("Select Role")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert selected role to internal format
        String roleKey = convertRoleToKey(selectedRole);

        // Authenticate user
        User user = dataManager.authenticateUser(userId, roleKey, password);

        if (user != null) {
            // Save login state
            prefsManager.setLoggedIn(true);
            prefsManager.setCurrentUser(user);

            // Navigate to dashboard
            navigateToDashboard();

            Toast.makeText(this, "Welcome, " + user.getName() + "!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertRoleToKey(String selectedRole) {
        switch (selectedRole) {
            case "ASHA Worker": return "asha";
            case "PHC Doctor": return "phcdoctor";
            case "PHC Nurse": return "phcnurse";
            case "PHC Administrator": return "phcadmin";
            default: return "";
        }
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(MainActivity.this, activity_dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}