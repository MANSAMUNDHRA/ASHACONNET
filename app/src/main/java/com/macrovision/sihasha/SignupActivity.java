package com.macrovision.sihasha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;

public class SignupActivity extends AppCompatActivity {

    private EditText etUserId, etName, etPassword, etConfirmPassword;
    private Spinner spinnerRole;
    private Button btnSignup;
    private TextView tvLoginLink;
    private ProgressBar progressBar;
    private CardView signupCard;
    
    private DataManager dataManager;
    private String selectedRole = "asha"; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dataManager = DataManager.getInstance(this);

        initializeViews();
        setupRoleSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        etUserId = findViewById(R.id.et_user_id);
        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerRole = findViewById(R.id.spinner_role);
        btnSignup = findViewById(R.id.btn_signup);
        tvLoginLink = findViewById(R.id.tv_login_link);
        progressBar = findViewById(R.id.progress_bar);
        signupCard = findViewById(R.id.signup_card);
    }

    private void setupRoleSpinner() {
        // Create adapter for spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
        
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String displayRole = parent.getItemAtPosition(position).toString();
                // Convert display names to role keys
                if (displayRole.equals("ASHA Worker")) {
                    selectedRole = "asha";
                } else if (displayRole.equals("PHC Doctor")) {
                    selectedRole = "phcdoctor";
                } else if (displayRole.equals("PHC Nurse")) {
                    selectedRole = "phcnurse";
                } else if (displayRole.equals("PHC Administrator")) {
                    selectedRole = "phcadmin";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRole = "asha";
            }
        });
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> registerUser());
        
        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String userId = etUserId.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (userId.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        // Create user with selected role
        User newUser = new User(
                userId,
                name,
                selectedRole,  // ✅ Now uses selected role
                "", // phone
                "", // village
                "", // block
                "", // district
                "West Bengal", // state
                "PHC001", // phcId
                password
        );

        boolean success = dataManager.registerUser(newUser);

        if (success) {
            Toast.makeText(this, "✅ " + getRoleDisplayName(selectedRole) + " Registered Successfully", Toast.LENGTH_SHORT).show();
            finish(); // Go back to login
        } else {
            showProgress(false);
            Toast.makeText(this, "❌ User ID already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRoleDisplayName(String role) {
        switch (role) {
            case "asha": return "ASHA Worker";
            case "phcdoctor": return "PHC Doctor";
            case "phcnurse": return "PHC Nurse";
            case "phcadmin": return "PHC Administrator";
            default: return "User";
        }
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSignup.setVisibility(View.GONE);
            signupCard.setAlpha(0.7f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSignup.setVisibility(View.VISIBLE);
            signupCard.setAlpha(1.0f);
        }
    }
}