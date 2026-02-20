package com.macrovision.sihasha.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddPatientActivity extends AppCompatActivity {

    private static final String TAG = "AddPatientActivity";
    public static final String EXTRA_PATIENT_ID = "patient_id";
    public static final String EXTRA_IS_EDIT_MODE = "is_edit_mode";

    // UI Components
    private ImageView btnBack;
    private Button btnSavePatient, btnCancel, btnSave;
    private TextView tvTitle;

    // Basic Information
    private TextInputEditText editPatientName, editPatientAge, editHusbandName, editPhoneNumber;
    private Spinner spinnerBloodGroup;

    // Address Details
    private TextInputEditText editAddress, editVillage, editBlock, editDistrict;

    // Personal Details
    private TextInputEditText editAadharNumber, editBankAccount;
    private Spinner spinnerReligion, spinnerCaste, spinnerEducation;

    // Pregnancy Information
    private Spinner spinnerPregnancyStatus;
    private TextInputEditText editPregnancyNumber, editLmpDate, editEddDate;
    private TextInputEditText editLiveChildren, editPreviousAbortions, editHeight;
    private CheckBox checkboxHighRisk;
    private LinearLayout layoutPregnancyDates;

    // Data Management
    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private SimpleDateFormat dateFormat;

    // Edit mode variables
    private boolean isEditMode = false;
    private String patientId = null;
    private Patient currentPatient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        Log.d(TAG, "AddPatientActivity started");

        // Check if this is edit mode
        checkEditMode();

        initializeComponents();
        setupSpinners();
        setupEventListeners();
        loadUserData();

        // Load patient data if in edit mode
        if (isEditMode) {
            loadPatientData();
        }
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null) {
            isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false);
            patientId = intent.getStringExtra(EXTRA_PATIENT_ID);

            Log.d(TAG, "Edit mode: " + isEditMode + ", Patient ID: " + patientId);
        }
    }

    private void initializeComponents() {
        Log.d(TAG, "Initializing components...");

        // Header components
        btnBack = findViewById(R.id.btn_back);
        btnSavePatient = findViewById(R.id.btn_save_patient);
        tvTitle = findViewById(R.id.tv_title); // Add this to your layout

        // Update title based on mode
        if (isEditMode) {
            if (tvTitle != null) {
                tvTitle.setText("Edit Patient");
            }
            btnSavePatient.setText("Update");
        } else {
            if (tvTitle != null) {
                tvTitle.setText("Add New Patient");
            }
            btnSavePatient.setText("Save");
        }

        // Basic Information
        editPatientName = findViewById(R.id.edit_patient_name);
        editPatientAge = findViewById(R.id.edit_patient_age);
        editHusbandName = findViewById(R.id.edit_husband_name);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);

        // Address Details
        editAddress = findViewById(R.id.edit_address);
        editVillage = findViewById(R.id.edit_village);
        editBlock = findViewById(R.id.edit_block);
        editDistrict = findViewById(R.id.edit_district);

        // Personal Details
        editAadharNumber = findViewById(R.id.edit_aadhar_number);
        editBankAccount = findViewById(R.id.edit_bank_account);
        spinnerReligion = findViewById(R.id.spinner_religion);
        spinnerCaste = findViewById(R.id.spinner_caste);
        spinnerEducation = findViewById(R.id.spinner_education);

        // Pregnancy Information
        spinnerPregnancyStatus = findViewById(R.id.spinner_pregnancy_status);
        editPregnancyNumber = findViewById(R.id.edit_pregnancy_number);
        editLmpDate = findViewById(R.id.edit_lmp_date);
        editEddDate = findViewById(R.id.edit_edd_date);
        editLiveChildren = findViewById(R.id.edit_live_children);
        editPreviousAbortions = findViewById(R.id.edit_previous_abortions);
        editHeight = findViewById(R.id.edit_height);
        checkboxHighRisk = findViewById(R.id.checkbox_high_risk);
        layoutPregnancyDates = findViewById(R.id.layout_pregnancy_dates);

        // Action Buttons
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        // Update save button text based on mode
        if (isEditMode) {
            btnSave.setText("Update Patient");
        } else {
            btnSave.setText("Save Patient");
        }

        // Data Management
        dataManager = DataManager.getInstance(this);
        prefsManager = new SharedPrefsManager(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Log.d(TAG, "Components initialized successfully");
    }

    private void setupSpinners() {
        Log.d(TAG, "Setting up spinners...");

        // Blood Group Spinner
        List<String> bloodGroups = Arrays.asList(
                "Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
        );
        ArrayAdapter<String> bloodGroupAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, bloodGroups
        );
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        // Religion Spinner
        List<String> religions = Arrays.asList(
                "Select Religion", "Hindu", "Muslim", "Christian", "Sikh", "Buddhist", "Jain", "Other"
        );
        ArrayAdapter<String> religionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, religions
        );
        religionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReligion.setAdapter(religionAdapter);

        // Caste Spinner
        List<String> castes = Arrays.asList(
                "Select Caste", "General", "OBC", "SC", "ST", "Other"
        );
        ArrayAdapter<String> casteAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, castes
        );
        casteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCaste.setAdapter(casteAdapter);

        // Education Spinner
        List<String> education = Arrays.asList(
                "Select Education", "Illiterate", "Primary", "Secondary", "Higher Secondary",
                "Graduate", "Post Graduate", "Technical/Vocational"
        );
        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, education
        );
        educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(educationAdapter);

        // Pregnancy Status Spinner
        List<String> pregnancyStatuses = Arrays.asList(
                "Select Status", "pregnant", "delivered", "not_pregnant", "other"
        );
        ArrayAdapter<String> pregnancyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, pregnancyStatuses
        );
        pregnancyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPregnancyStatus.setAdapter(pregnancyAdapter);

        Log.d(TAG, "Spinners setup completed");
    }

    private void setupEventListeners() {
        Log.d(TAG, "Setting up event listeners...");

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Save buttons (header and bottom)
        btnSavePatient.setOnClickListener(v -> saveOrUpdatePatient());
        btnSave.setOnClickListener(v -> saveOrUpdatePatient());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Date picker for LMP
        editLmpDate.setOnClickListener(v -> showDatePicker(editLmpDate, "Select LMP Date"));

        // Date picker for EDD
        editEddDate.setOnClickListener(v -> showDatePicker(editEddDate, "Select EDD"));

        // Pregnancy status change listener
        spinnerPregnancyStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = parent.getItemAtPosition(position).toString();

                // Show pregnancy dates only if pregnant
                if ("pregnant".equals(selectedStatus)) {
                    layoutPregnancyDates.setVisibility(View.VISIBLE);
                } else {
                    layoutPregnancyDates.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        Log.d(TAG, "Event listeners setup completed");
    }

    private void loadUserData() {
        currentUser = prefsManager.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user: " + currentUser.getName() + ", Role: " + currentUser.getRole());

            // Pre-fill district if available from user data (only for new patients)
            if (!isEditMode && currentUser.getDistrict() != null && !currentUser.getDistrict().isEmpty()) {
                editDistrict.setText(currentUser.getDistrict());
            }
        } else {
            Log.w(TAG, "Current user is null");
        }
    }

    private void loadPatientData() {
        if (!isEditMode || patientId == null) {
            return;
        }

        Log.d(TAG, "Loading patient data for ID: " + patientId);

        try {
            currentPatient = dataManager.getPatientById(patientId);

            if (currentPatient == null) {
                Log.e(TAG, "Patient not found with ID: " + patientId);
                Toast.makeText(this, "Patient not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Loading data for patient: " + currentPatient.getName());
            populateFormWithPatientData(currentPatient);

        } catch (Exception e) {
            Log.e(TAG, "Error loading patient data", e);
            Toast.makeText(this, "Error loading patient data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateFormWithPatientData(Patient patient) {
        Log.d(TAG, "Populating form with patient data...");

        try {
            // Basic Information
            setText(editPatientName, patient.getName());
            setText(editPatientAge, String.valueOf(patient.getAge()));
            setText(editHusbandName, patient.getHusbandName());
            setText(editPhoneNumber, patient.getPhoneNumber());

            // Blood Group
            if (patient.getBloodGroup() != null && !patient.getBloodGroup().isEmpty()) {
                setSpinnerSelection(spinnerBloodGroup, patient.getBloodGroup());
            }

            // Address Details
            setText(editAddress, patient.getAddress());
            setText(editVillage, patient.getVillage());
            setText(editBlock, patient.getBlock());
            setText(editDistrict, patient.getDistrict());

            // Personal Details
            setText(editAadharNumber, patient.getAadharNumber());
            setText(editBankAccount, patient.getBankAccount());

            if (patient.getReligion() != null && !patient.getReligion().isEmpty()) {
                setSpinnerSelection(spinnerReligion, patient.getReligion());
            }

            if (patient.getCaste() != null && !patient.getCaste().isEmpty()) {
                setSpinnerSelection(spinnerCaste, patient.getCaste());
            }

            if (patient.getEducation() != null && !patient.getEducation().isEmpty()) {
                setSpinnerSelection(spinnerEducation, patient.getEducation());
            }

            // Pregnancy Information
            if (patient.getPregnancyStatus() != null && !patient.getPregnancyStatus().isEmpty()) {
                setSpinnerSelection(spinnerPregnancyStatus, patient.getPregnancyStatus());

                // Show pregnancy dates if pregnant
                if ("pregnant".equals(patient.getPregnancyStatus())) {
                    layoutPregnancyDates.setVisibility(View.VISIBLE);
                }
            }

            if (patient.getPregnancyNumber() > 0) {
                setText(editPregnancyNumber, String.valueOf(patient.getPregnancyNumber()));
            }

            setText(editLmpDate, patient.getLmpDate());
            setText(editEddDate, patient.getEddDate());

            if (patient.getLiveChildren() > 0) {
                setText(editLiveChildren, String.valueOf(patient.getLiveChildren()));
            }

            if (patient.getPreviousAbortions() > 0) {
                setText(editPreviousAbortions, String.valueOf(patient.getPreviousAbortions()));
            }

            if (patient.getHeight() > 0) {
                setText(editHeight, String.valueOf(patient.getHeight()));
            }

            checkboxHighRisk.setChecked(patient.isHighRisk());

            Log.d(TAG, "Form populated successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error populating form", e);
            Toast.makeText(this, "Error loading patient details", Toast.LENGTH_SHORT).show();
        }
    }

    private void setText(TextInputEditText editText, String value) {
        if (editText != null && value != null && !value.isEmpty()) {
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

    private void showDatePicker(TextInputEditText editText, String title) {
        Calendar calendar = Calendar.getInstance();

        // If editing and field has existing date, use it
        String existingDate = getText(editText);
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
                    editText.setText(selectedDate);

                    // Auto-calculate EDD if LMP is selected
                    if (editText == editLmpDate) {
                        calculateEDD(calendar);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setTitle(title);
        datePickerDialog.show();
    }

    private void calculateEDD(Calendar lmpDate) {
        // EDD = LMP + 280 days (40 weeks)
        Calendar eddDate = (Calendar) lmpDate.clone();
        eddDate.add(Calendar.DAY_OF_YEAR, 280);

        String eddString = dateFormat.format(eddDate.getTime());
        editEddDate.setText(eddString);

        Log.d(TAG, "EDD calculated: " + eddString);
    }

    private boolean validateInput() {
        Log.d(TAG, "Validating input...");

        // Required fields validation
        if (getText(editPatientName).isEmpty()) {
            editPatientName.setError("Patient name is required");
            editPatientName.requestFocus();
            return false;
        }

        if (getText(editPatientAge).isEmpty()) {
            editPatientAge.setError("Age is required");
            editPatientAge.requestFocus();
            return false;
        }

        if (getText(editPhoneNumber).isEmpty()) {
            editPhoneNumber.setError("Phone number is required");
            editPhoneNumber.requestFocus();
            return false;
        }

        if (getText(editVillage).isEmpty()) {
            editVillage.setError("Village is required");
            editVillage.requestFocus();
            return false;
        }

        if (spinnerPregnancyStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select pregnancy status", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Phone number validation
        String phone = getText(editPhoneNumber);
        if (phone.length() != 10 || !phone.matches("\\d{10}")) {
            editPhoneNumber.setError("Please enter a valid 10-digit phone number");
            editPhoneNumber.requestFocus();
            return false;
        }

        // Age validation
        try {
            int age = Integer.parseInt(getText(editPatientAge));
            if (age < 1 || age > 100) {
                editPatientAge.setError("Please enter a valid age (1-100)");
                editPatientAge.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            editPatientAge.setError("Please enter a valid age");
            editPatientAge.requestFocus();
            return false;
        }

        Log.d(TAG, "Input validation passed");
        return true;
    }

    private void saveOrUpdatePatient() {
        Log.d(TAG, "Attempting to save/update patient...");

        if (!validateInput()) {
            return;
        }

        try {
            Patient patient;

            if (isEditMode && currentPatient != null) {
                // Update existing patient
                patient = currentPatient;
                Log.d(TAG, "Updating existing patient: " + patient.getId());
            } else {
                // Create new patient
                patient = new Patient();
                String patientId = "PAT" + String.format("%03d", (int)(Math.random() * 1000));
                patient.setId(patientId);
                Log.d(TAG, "Creating new patient with ID: " + patientId);
            }

            // Update all fields
            updatePatientFromForm(patient);

            // Save or update patient in DataManager
            boolean success;
            if (isEditMode) {
                success = dataManager.updatePatient(patient);
            } else {
                success = dataManager.addPatient(patient);
            }

            if (success) {
                String message = isEditMode ? "Patient updated successfully!" : "Patient saved successfully!";
                Log.d(TAG, message + " - " + patient.getName());
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                // Set result and finish
                setResult(RESULT_OK);
                finish();
            } else {
                String errorMessage = isEditMode ? "Failed to update patient. Please try again." : "Failed to save patient. Please try again.";
                Log.e(TAG, errorMessage);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            String errorMessage = "Error " + (isEditMode ? "updating" : "saving") + " patient: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void updatePatientFromForm(Patient patient) {
        // Basic Information
        patient.setName(getText(editPatientName));
        patient.setAge(Integer.parseInt(getText(editPatientAge)));
        patient.setHusbandName(getText(editHusbandName));
        patient.setPhoneNumber(getText(editPhoneNumber));

        // Blood Group
        if (spinnerBloodGroup.getSelectedItemPosition() > 0) {
            patient.setBloodGroup(spinnerBloodGroup.getSelectedItem().toString());
        }

        // Address Details
        patient.setAddress(getText(editAddress));
        patient.setVillage(getText(editVillage));
        patient.setBlock(getText(editBlock));
        patient.setDistrict(getText(editDistrict));

        // Personal Details
        patient.setAadharNumber(getText(editAadharNumber));
        patient.setBankAccount(getText(editBankAccount));

        if (spinnerReligion.getSelectedItemPosition() > 0) {
            patient.setReligion(spinnerReligion.getSelectedItem().toString());
        }

        if (spinnerCaste.getSelectedItemPosition() > 0) {
            patient.setCaste(spinnerCaste.getSelectedItem().toString());
        }

        if (spinnerEducation.getSelectedItemPosition() > 0) {
            patient.setEducation(spinnerEducation.getSelectedItem().toString());
        }

        // Pregnancy Information
        patient.setPregnancyStatus(spinnerPregnancyStatus.getSelectedItem().toString());

        if (!getText(editPregnancyNumber).isEmpty()) {
            patient.setPregnancyNumber(Integer.parseInt(getText(editPregnancyNumber)));
        }

        patient.setLmpDate(getText(editLmpDate));
        patient.setEddDate(getText(editEddDate));

        if (!getText(editLiveChildren).isEmpty()) {
            patient.setLiveChildren(Integer.parseInt(getText(editLiveChildren)));
        }

        if (!getText(editPreviousAbortions).isEmpty()) {
            patient.setPreviousAbortions(Integer.parseInt(getText(editPreviousAbortions)));
        }

        if (!getText(editHeight).isEmpty()) {
            patient.setHeight(Integer.parseInt(getText(editHeight)));
        }

        patient.setHighRisk(checkboxHighRisk.isChecked());

        // Set current user's ASHA ID and PHC ID (only for new patients)
        if (!isEditMode && currentUser != null) {
            if ("asha".equals(currentUser.getRole())) {
                patient.setAshaId(currentUser.getId());
                patient.setPhcId(currentUser.getPhcId());
            } else {
                // For PHC staff, assign to the PHC
                patient.setPhcId(currentUser.getPhcId() != null ? currentUser.getPhcId() : "PHC001");
                // TODO: Allow selection of ASHA worker
                if (patient.getAshaId() == null || patient.getAshaId().isEmpty()) {
                    patient.setAshaId("ASHA001"); // Default for now
                }
            }
        }

        // Set registration date (only for new patients)
        if (!isEditMode) {
            patient.setRegistrationDate(dateFormat.format(Calendar.getInstance().getTime()));
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
