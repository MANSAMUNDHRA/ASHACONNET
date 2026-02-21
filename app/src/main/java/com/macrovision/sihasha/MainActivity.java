package com.macrovision.sihasha;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.macrovision.sihasha.utils.FirebaseHelper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

public class MainActivity extends AppCompatActivity {

    private EditText etUserId, etPassword;
    private Spinner spinnerRole;
    private Button btnLogin;
    private TextView tvSignupLink;
    private ProgressBar progressBar;
    private CardView loginCard;
    
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    
    private String selectedRole = "asha"; // default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is already logged in
        prefsManager = new SharedPrefsManager(this);
        if (prefsManager.isLoggedIn()) {
            navigateToDashboard();
            return;
        }

        initializeViews();
        setupRoleSpinner();
        setupClickListeners();
        
        dataManager = DataManager.getInstance(this);
    }

    private void initializeViews() {
        etUserId = findViewById(R.id.et_user_id);
        etPassword = findViewById(R.id.et_password);
        spinnerRole = findViewById(R.id.spinner_role);
        btnLogin = findViewById(R.id.btn_login);
        tvSignupLink = findViewById(R.id.tv_signup_link);
        progressBar = findViewById(R.id.progress_bar);
        loginCard = findViewById(R.id.login_card);
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
        btnLogin.setOnClickListener(v -> attemptLogin());
        
        tvSignupLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
    String userId = etUserId.getText().toString().trim();
    String password = etPassword.getText().toString().trim();

    if (TextUtils.isEmpty(userId)) {
        etUserId.setError("User ID is required");
        etUserId.requestFocus();
        return;
    }

    if (TextUtils.isEmpty(password)) {
        etPassword.setError("Password is required");
        etPassword.requestFocus();
        return;
    }

    showProgress(true);

    // Use Firebase for authentication
    FirebaseHelper.getInstance(this).loginUser(userId, password, 
        new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                showProgress(false);
                Toast.makeText(MainActivity.this, 
                    "✅ Login Successful! Welcome " + user.getName(), 
                    Toast.LENGTH_SHORT).show();
                
                prefsManager.setCurrentUser(user);
                prefsManager.setLoggedIn(true);
                
                navigateToDashboard();
            }
            
            @Override
            public void onFailure(String error) {
                showProgress(false);
                Toast.makeText(MainActivity.this, 
                    "❌ Login failed: " + error, 
                    Toast.LENGTH_LONG).show();
            }
        });
}
    private void navigateToDashboard() {
        Intent intent = new Intent(MainActivity.this, activity_dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            loginCard.setAlpha(0.7f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            loginCard.setAlpha(1.0f);
        }
    }
}