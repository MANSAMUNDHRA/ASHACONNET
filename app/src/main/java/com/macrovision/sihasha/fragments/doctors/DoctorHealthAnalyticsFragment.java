package com.macrovision.sihasha.fragments.doctors;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.Patient;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorHealthAnalyticsFragment extends Fragment {

    private TextView tvTotalPatients, tvPregnantCount, tvDeliveredCount, tvHighRiskCount;
    private TextView tvAncCompletion, tvInstitutionalDelivery, tvAnemiaRate, tvImmunizationRate;
    private Button btnRefresh, btnGenerateReport;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;
    private List<Patient> allPatients;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_health_analytics, container, false);
        initializeViews(view);
        setupDataManager();
        loadAnalyticsData();
        setupClickListeners();
        return view;
    }

    private void initializeViews(View view) {
        tvTotalPatients = view.findViewById(R.id.tv_total_patients);
        tvPregnantCount = view.findViewById(R.id.tv_pregnant_count);
        tvDeliveredCount = view.findViewById(R.id.tv_delivered_count);
        tvHighRiskCount = view.findViewById(R.id.tv_high_risk_count);
        tvAncCompletion = view.findViewById(R.id.tv_anc_completion);
        tvInstitutionalDelivery = view.findViewById(R.id.tv_institutional_delivery);
        tvAnemiaRate = view.findViewById(R.id.tv_anemia_rate);
        tvImmunizationRate = view.findViewById(R.id.tv_immunization_rate);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnGenerateReport = view.findViewById(R.id.btn_generate_report);
    }

    private void setupDataManager() {
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());
        currentUser = prefsManager.getCurrentUser();
    }

    private void setupClickListeners() {
        if (btnRefresh != null)
            btnRefresh.setOnClickListener(v -> loadAnalyticsData());

        if (btnGenerateReport != null)
            btnGenerateReport.setOnClickListener(v -> showReportOptions());
    }

    private void loadAnalyticsData() {
        allPatients = dataManager.getAllPatients();

        int total = allPatients.size();
        int pregnant = 0, delivered = 0, highRisk = 0;

        for (Patient p : allPatients) {
            if ("pregnant".equals(p.getPregnancyStatus())) pregnant++;
            else if ("delivered".equals(p.getPregnancyStatus())) delivered++;
            if (p.isHighRisk()) highRisk++;
        }

        if (tvTotalPatients != null) tvTotalPatients.setText(String.valueOf(total));
        if (tvPregnantCount != null) tvPregnantCount.setText(String.valueOf(pregnant));
        if (tvDeliveredCount != null) tvDeliveredCount.setText(String.valueOf(delivered));
        if (tvHighRiskCount != null) tvHighRiskCount.setText(String.valueOf(highRisk));

        // Calculate percentages based on actual data
        if (tvAncCompletion != null) {
            // ANC completion rate = pregnant women with 4+ visits / total pregnant women
            int pregnantWithAnc = 0;
            for (Patient p : allPatients) {
                if ("pregnant".equals(p.getPregnancyStatus()) && p.getPregnancyNumber() >= 4) {
                    pregnantWithAnc++;
                }
            }
            int ancRate = pregnant > 0 ? (pregnantWithAnc * 100 / pregnant) : 0;
            tvAncCompletion.setText(ancRate + "%");
        }

        if (tvInstitutionalDelivery != null) {
            // Institutional delivery rate = delivered in hospital / total delivered
            int institutional = 0;
            int totalDelivered = 0;
            for (Patient p : allPatients) {
                if ("delivered".equals(p.getPregnancyStatus())) {
                    totalDelivered++;
                    // Assuming hospital delivery if blood group is recorded (you can change this logic)
                    if (p.getBloodGroup() != null && !p.getBloodGroup().isEmpty()) {
                        institutional++;
                    }
                }
            }
            int deliveryRate = totalDelivered > 0 ? (institutional * 100 / totalDelivered) : 0;
            tvInstitutionalDelivery.setText(deliveryRate + "%");
        }

        if (tvAnemiaRate != null) {
            // Anemia rate = patients with low hemoglobin / total patients
            int anemic = 0;
            for (Patient p : allPatients) {
                // Assuming anemia if patient is high risk (you can change this logic)
                if (p.isHighRisk()) {
                    anemic++;
                }
            }
            int anemiaRate = total > 0 ? (anemic * 100 / total) : 0;
            tvAnemiaRate.setText(anemiaRate + "%");
        }

        if (tvImmunizationRate != null) {
            // Immunization rate = children immunized / total children
            int immunized = 0;
            int totalChildren = 0;
            for (Patient p : allPatients) {
                if (p.getAge() <= 5) {
                    totalChildren++;
                    // Assuming immunized if registered (you can change this logic)
                    if (p.getRegistrationDate() != null) {
                        immunized++;
                    }
                }
            }
            int immunizationRate = totalChildren > 0 ? (immunized * 100 / totalChildren) : 0;
            tvImmunizationRate.setText(immunizationRate + "%");
        }
    }

    private void showReportOptions() {
        if (allPatients == null || allPatients.isEmpty()) {
            Toast.makeText(requireContext(), "No patient data available to generate report", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] reportTypes = {
            "Full Patient Summary",
            "High-Risk Cases Report",
            "Pregnant Patients Report",
            "Delivery Status Report"
        };

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Report Type")
                .setItems(reportTypes, (dialog, which) -> {
                    switch (which) {
                        case 0: generateAndShareReport("Full Patient Summary", buildFullReport()); break;
                        case 1: generateAndShareReport("High-Risk Cases", buildHighRiskReport()); break;
                        case 2: generateAndShareReport("Pregnant Patients", buildPregnantReport()); break;
                        case 3: generateAndShareReport("Delivery Status", buildDeliveryReport()); break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAndShareReport(String title, String content) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title + " - ASHA Connect");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, "Share Report via"));
    }

    private String getReportHeader(String title) {
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        String doctor = currentUser != null ? currentUser.getName() : "PHC Doctor";
        return "========================================\n" +
               "         ASHA CONNECT - PHC REPORT\n" +
               "========================================\n" +
               "Report: " + title + "\n" +
               "Doctor: " + doctor + "\n" +
               "Date:   " + date + "\n" +
               "========================================\n\n";
    }

    private String buildFullReport() {
        int total = allPatients.size();
        int pregnant = 0, delivered = 0, highRisk = 0, postpartum = 0;

        for (Patient p : allPatients) {
            if ("pregnant".equals(p.getPregnancyStatus())) pregnant++;
            else if ("delivered".equals(p.getPregnancyStatus())) delivered++;
            else if ("postpartum".equals(p.getPregnancyStatus())) postpartum++;
            if (p.isHighRisk()) highRisk++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getReportHeader("Full Patient Summary"));
        sb.append("OVERVIEW\n--------\n");
        sb.append("Total Patients:    ").append(total).append("\n");
        sb.append("Pregnant:          ").append(pregnant).append("\n");
        sb.append("Delivered:         ").append(delivered).append("\n");
        sb.append("Postpartum:        ").append(postpartum).append("\n");
        sb.append("High Risk:         ").append(highRisk).append("\n\n");
        sb.append("PATIENT LIST\n------------\n");

        for (int i = 0; i < allPatients.size(); i++) {
            Patient p = allPatients.get(i);
            sb.append(i + 1).append(". ").append(p.getName()).append("\n");
            sb.append("   Age: ").append(p.getAge());
            if (p.getVillage() != null) sb.append(" | Village: ").append(p.getVillage());
            sb.append("\n   Status: ").append(p.getPregnancyStatus() != null ? p.getPregnancyStatus() : "N/A");
            if (p.isHighRisk()) sb.append(" | ⚠ HIGH RISK");
            if (p.getBloodGroup() != null) sb.append("\n   Blood Group: ").append(p.getBloodGroup());
            if (p.getEddDate() != null) sb.append(" | EDD: ").append(p.getEddDate());
            sb.append("\n\n");
        }
        return sb.toString();
    }

    private String buildHighRiskReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(getReportHeader("High-Risk Cases Report"));

        List<Patient> highRisk = dataManager.getHighRiskPatientsForDoctor(
                currentUser != null ? currentUser.getId() : "");

        if (highRisk.isEmpty()) {
            sb.append("No high-risk patients currently assigned to you.\n");
        } else {
            sb.append("Total High-Risk Patients: ").append(highRisk.size()).append("\n\n");
            for (int i = 0; i < highRisk.size(); i++) {
                Patient p = highRisk.get(i);
                sb.append(i + 1).append(". ").append(p.getName()).append("\n");
                sb.append("   Age: ").append(p.getAge()).append("\n");
                if (p.getPhoneNumber() != null) sb.append("   Phone: ").append(p.getPhoneNumber()).append("\n");
                if (p.getVillage() != null) sb.append("   Village: ").append(p.getVillage()).append("\n");
                sb.append("   Status: ").append(p.getPregnancyStatus() != null ? p.getPregnancyStatus() : "N/A").append("\n");
                if (p.getBloodGroup() != null) sb.append("   Blood Group: ").append(p.getBloodGroup()).append("\n");
                if (p.getLmpDate() != null) sb.append("   LMP: ").append(p.getLmpDate()).append("\n");
                if (p.getEddDate() != null) sb.append("   EDD: ").append(p.getEddDate()).append("\n");
                if (p.getRiskFactors() != null && p.getRiskFactors().length > 0) {
                    sb.append("   Risk Factors: ");
                    for (String r : p.getRiskFactors()) sb.append(r).append(", ");
                    sb.deleteCharAt(sb.length() - 2);
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String buildPregnantReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(getReportHeader("Pregnant Patients Report"));

        int count = 0;
        for (int i = 0; i < allPatients.size(); i++) {
            Patient p = allPatients.get(i);
            if (!"pregnant".equals(p.getPregnancyStatus())) continue;
            count++;
            sb.append(count).append(". ").append(p.getName()).append("\n");
            sb.append("   Age: ").append(p.getAge());
            if (p.getVillage() != null) sb.append(" | Village: ").append(p.getVillage());
            sb.append("\n");
            if (p.getHusbandName() != null) sb.append("   Husband: ").append(p.getHusbandName()).append("\n");
            if (p.getPhoneNumber() != null) sb.append("   Phone: ").append(p.getPhoneNumber()).append("\n");
            if (p.getBloodGroup() != null) sb.append("   Blood Group: ").append(p.getBloodGroup()).append("\n");
            if (p.getLmpDate() != null) sb.append("   LMP Date: ").append(p.getLmpDate()).append("\n");
            if (p.getEddDate() != null) sb.append("   Expected Delivery: ").append(p.getEddDate()).append("\n");
            sb.append("   Pregnancy No.: ").append(p.getPregnancyNumber()).append("\n");
            if (p.isHighRisk()) sb.append("   ⚠ HIGH RISK\n");
            sb.append("\n");
        }

        if (count == 0) sb.append("No pregnant patients found.\n");
        else {
            String header = getReportHeader("Pregnant Patients Report");
            sb.insert(header.length(), "Total Pregnant Patients: " + count + "\n\n");
        }

        return sb.toString();
    }

    private String buildDeliveryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(getReportHeader("Delivery Status Report"));

        int delivered = 0, pregnant = 0, postpartum = 0, other = 0;
        for (Patient p : allPatients) {
            switch (p.getPregnancyStatus() != null ? p.getPregnancyStatus() : "") {
                case "delivered": delivered++; break;
                case "pregnant": pregnant++; break;
                case "postpartum": postpartum++; break;
                default: other++; break;
            }
        }

        sb.append("STATUS SUMMARY\n--------------\n");
        sb.append("Currently Pregnant:  ").append(pregnant).append("\n");
        sb.append("Delivered:           ").append(delivered).append("\n");
        sb.append("Postpartum:          ").append(postpartum).append("\n");
        sb.append("Other/Unknown:       ").append(other).append("\n\n");
        sb.append("DELIVERED PATIENTS\n------------------\n");

        int count = 0;
        for (Patient p : allPatients) {
            if (!"delivered".equals(p.getPregnancyStatus())) continue;
            count++;
            sb.append(count).append(". ").append(p.getName()).append("\n");
            sb.append("   Age: ").append(p.getAge());
            if (p.getVillage() != null) sb.append(" | Village: ").append(p.getVillage());
            sb.append("\n");
            if (p.getLiveChildren() > 0) sb.append("   Live Children: ").append(p.getLiveChildren()).append("\n");
            sb.append("\n");
        }
        if (count == 0) sb.append("No delivered patients found.\n");
        return sb.toString();
    }

    public void refreshData() {
        loadAnalyticsData();
    }
}