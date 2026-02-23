package com.macrovision.sihasha;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
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
import com.macrovision.sihasha.utils.FirebaseHelper;

public class SignupActivity extends AppCompatActivity {

    private EditText etUserId, etName, etPassword, etConfirmPassword;
    private Spinner spinnerRole;
    private Button btnSignup;
    private TextView tvLoginLink, tvTitle, tvSubtitle;
    private TextView tvUserIdLabel, tvNameLabel, tvRoleLabel, tvPasswordLabel, tvConfirmPasswordLabel;
    private ProgressBar progressBar;
    private CardView signupCard;
    private View rootView;
    
    private DataManager dataManager;
    private String selectedRole = "asha";

    // Soft black color - not pure black, not grey
    private static final int SOFT_BLACK = 0xFF2C2C2C; // Very dark charcoal, almost black
    // Light blue color
    private static final int LIGHT_BLUE = 0xFFADD8E6; // Light blue

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dataManager = DataManager.getInstance(this);

        initializeViews();
        setupRoleSpinner();
        setupClickListeners();
        setupLightBlueTheme();
        setAllTextToSoftBlack();
        startEntryAnimation();
    }

    private void initializeViews() {
        rootView = findViewById(android.R.id.content);
        etUserId = findViewById(R.id.et_user_id);
        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        
        spinnerRole = findViewById(R.id.spinner_role);
        btnSignup = findViewById(R.id.btn_signup);
        tvLoginLink = findViewById(R.id.tv_login_link);
        progressBar = findViewById(R.id.progress_bar);
        signupCard = findViewById(R.id.signup_card);
        
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        
        // Find all label TextViews
        tvUserIdLabel = findTextViewWithText("User ID");
        tvNameLabel = findTextViewWithText("Full Name");
        tvRoleLabel = findTextViewWithText("Select Role");
        tvPasswordLabel = findTextViewWithText("Password");
        tvConfirmPasswordLabel = findTextViewWithText("Confirm Password");
    }

    private TextView findTextViewWithText(String text) {
        // This is a helper - in reality, you should give them IDs in your layout
        // For now, we'll find by searching through the view hierarchy
        // But it's better to add IDs to your layout
        return null; // This will be handled by the layout IDs
    }

    private void setAllTextToSoftBlack() {
        // Set all text colors to soft black
        if (tvTitle != null) tvTitle.setTextColor(SOFT_BLACK);
        if (tvSubtitle != null) tvSubtitle.setTextColor(SOFT_BLACK);
        if (tvLoginLink != null) tvLoginLink.setTextColor(SOFT_BLACK);
        
        // Set EditText text color to soft black
        if (etUserId != null) {
            etUserId.setTextColor(SOFT_BLACK);
            etUserId.setHintTextColor(0xFF8A8A8A); // Light grey hint
        }
        if (etName != null) {
            etName.setTextColor(SOFT_BLACK);
            etName.setHintTextColor(0xFF8A8A8A);
        }
        if (etPassword != null) {
            etPassword.setTextColor(SOFT_BLACK);
            etPassword.setHintTextColor(0xFF8A8A8A);
        }
        if (etConfirmPassword != null) {
            etConfirmPassword.setTextColor(SOFT_BLACK);
            etConfirmPassword.setHintTextColor(0xFF8A8A8A);
        }
    }

    private void setupLightBlueTheme() {
        // Light blue gradient background
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFE0F0FF, 0xFFD0E8FF, 0xFFADD8E6} // Light blue gradient
        );
        gradient.setCornerRadius(0f);
        rootView.setBackground(gradient);
        
        // White card
        signupCard.setCardBackgroundColor(0xFFFFFFFF);
        signupCard.setCardElevation(4f);
        signupCard.setRadius(20f);
        
        // Light blue button
        btnSignup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                LIGHT_BLUE)); // Light blue
        
        // Light blue progress bar
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(
                LIGHT_BLUE)); // Light blue
    }

    private void startEntryAnimation() {
        signupCard.setAlpha(0f);
        signupCard.setTranslationY(50f);
        
        signupCard.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(600)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();
    }

    private void setupRoleSpinner() {
        String[] roles = {"ASHA Worker", "PHC Doctor", "PHC Nurse", "PHC Administrator"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_item, roles) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(SOFT_BLACK); // Soft black
                    tv.setTextSize(15f);
                    tv.setPadding(16, 16, 16, 16);
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(SOFT_BLACK); // Soft black
                    tv.setBackgroundColor(0xFFE8F0FE); // Light blue background
                    tv.setTextSize(15f);
                    tv.setPadding(24, 16, 24, 16);
                }
                return view;
            }
        };
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
        
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String displayRole = parent.getItemAtPosition(position).toString();
                
                if (displayRole.contains("ASHA")) {
                    selectedRole = "asha";
                } else if (displayRole.contains("Doctor")) {
                    selectedRole = "phcdoctor";
                } else if (displayRole.contains("Nurse")) {
                    selectedRole = "phcnurse";
                } else if (displayRole.contains("Administrator")) {
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
        btnSignup.setOnClickListener(v -> {
            v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start());
            registerUser();
        });
        
        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void registerUser() {
        String userId = etUserId.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

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

        User newUser = new User(
                userId,
                name,
                selectedRole,
                "",
                "",
                "",
                "",
                "West Bengal",
                "PHC001",
                password
        );

        FirebaseHelper.getInstance(this).registerUser(newUser, password, 
            new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(User user) {
                    showProgress(false);
                    
                    // Slightly darker blue for success
                    btnSignup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#7EC8E0"))); // Darker light blue
                    btnSignup.setText("✓ SUCCESS!");
                    
                    Toast.makeText(SignupActivity.this, 
                        "✅ " + getRoleDisplayName(selectedRole) + " Registered", 
                        Toast.LENGTH_SHORT).show();
                    
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, 1500);
                }
                
                @Override
                public void onFailure(String error) {
                    showProgress(false);
                    Toast.makeText(SignupActivity.this, 
                        "Registration failed: " + error, 
                        Toast.LENGTH_LONG).show();
                }
            });
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
            signupCard.setAlpha(0.8f);
            
            etUserId.setEnabled(false);
            etName.setEnabled(false);
            etPassword.setEnabled(false);
            etConfirmPassword.setEnabled(false);
            spinnerRole.setEnabled(false);
            tvLoginLink.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSignup.setVisibility(View.VISIBLE);
            signupCard.setAlpha(1.0f);
            
            etUserId.setEnabled(true);
            etName.setEnabled(true);
            etPassword.setEnabled(true);
            etConfirmPassword.setEnabled(true);
            spinnerRole.setEnabled(true);
            tvLoginLink.setEnabled(true);
        }
    }
}