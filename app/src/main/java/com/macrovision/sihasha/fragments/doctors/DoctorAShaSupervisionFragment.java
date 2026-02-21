package com.macrovision.sihasha.fragments.doctors;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.User;
import com.macrovision.sihasha.utils.DataManager;
import com.macrovision.sihasha.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class DoctorAShaSupervisionFragment extends Fragment {

    private RecyclerView recyclerView;
    private AShaAdapter adapter;
    private TextView tvTotalASHA, tvActiveASHA, tvAvgPatients, tvHighRiskDetected;
    private Button btnRefresh;

    private DataManager dataManager;
    private SharedPrefsManager prefsManager;
    private User currentUser;

    // ✅ FIX: Single list that adapter always references — never reassigned
    private final List<User> ashaWorkers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_asha_supervision, container, false);

        initializeViews(view);
        setupDataManager();
        setupRecyclerView(); // Adapter created ONCE, pointing at ashaWorkers list
        loadAShaData();      // Fills ashaWorkers and calls notifyDataSetChanged
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_asha);
        tvTotalASHA = view.findViewById(R.id.tv_total_asha);
        tvActiveASHA = view.findViewById(R.id.tv_active_asha);
        tvAvgPatients = view.findViewById(R.id.tv_avg_patients);
        tvHighRiskDetected = view.findViewById(R.id.tv_high_risk_detected);
        btnRefresh = view.findViewById(R.id.btn_refresh);
    }

    private void setupDataManager() {
        dataManager = DataManager.getInstance(requireContext());
        prefsManager = new SharedPrefsManager(requireContext());
        currentUser = prefsManager.getCurrentUser();
    }

    private void setupRecyclerView() {
        // ✅ Adapter is created once pointing to the persistent ashaWorkers list
        adapter = new AShaAdapter(ashaWorkers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> loadAShaData());
    }

    private void loadAShaData() {
        // ✅ FIX: Clear and refill the SAME list — don't reassign the variable
        ashaWorkers.clear();
        ashaWorkers.addAll(dataManager.getASHAWorkers());

        int total = ashaWorkers.size();
        int active = total; // All registered = active for now
        int totalPatients = 0;
        int highRisk = 0;

        for (User asha : ashaWorkers) {
            totalPatients += dataManager.getPatientsForASHA(asha.getId()).size();
            highRisk += dataManager.getHighRiskPatientsForASHA(asha.getId()).size();
        }

        if (tvTotalASHA != null) tvTotalASHA.setText(String.valueOf(total));
        if (tvActiveASHA != null) tvActiveASHA.setText(String.valueOf(active));
        if (tvAvgPatients != null)
            tvAvgPatients.setText(total > 0 ? String.format("%.1f", (double) totalPatients / total) : "0");
        if (tvHighRiskDetected != null) tvHighRiskDetected.setText(String.valueOf(highRisk));

        // ✅ Notify the adapter that the list it already holds has changed
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private class AShaAdapter extends RecyclerView.Adapter<AShaAdapter.ViewHolder> {

        private final List<User> ashaList;

        AShaAdapter(List<User> ashaList) {
            this.ashaList = ashaList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_asha_supervision, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User asha = ashaList.get(position);
            int patientCount = dataManager.getPatientsForASHA(asha.getId()).size();
            int highRiskCount = dataManager.getHighRiskPatientsForASHA(asha.getId()).size();

            if (holder.tvName != null) holder.tvName.setText(asha.getName());
            if (holder.tvVillage != null)
                holder.tvVillage.setText(asha.getVillage() != null ? asha.getVillage() : "Not assigned");
            if (holder.tvPhone != null)
                holder.tvPhone.setText(asha.getPhone() != null ? asha.getPhone() : "No phone");
            if (holder.tvPatientCount != null)
                holder.tvPatientCount.setText(patientCount + " patients");
            if (holder.tvHighRiskCount != null)
                holder.tvHighRiskCount.setText(highRiskCount + " high risk");
            if (holder.btnViewDetails != null)
                holder.btnViewDetails.setOnClickListener(v ->
                        showAShaDetails(asha, patientCount, highRiskCount));
        }

        @Override
        public int getItemCount() { return ashaList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvVillage, tvPhone, tvPatientCount, tvHighRiskCount;
            Button btnViewDetails;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_asha_name);
                tvVillage = itemView.findViewById(R.id.tv_asha_village);
                tvPhone = itemView.findViewById(R.id.tv_asha_phone);
                tvPatientCount = itemView.findViewById(R.id.tv_patient_count);
                tvHighRiskCount = itemView.findViewById(R.id.tv_high_risk_count);
                btnViewDetails = itemView.findViewById(R.id.btn_view_details);
            }
        }
    }

    private void showAShaDetails(User asha, int patientCount, int highRiskCount) {
        String details = "ASHA Worker: " + asha.getName() + "\n" +
                "Village: " + (asha.getVillage() != null ? asha.getVillage() : "Not assigned") + "\n" +
                "Phone: " + (asha.getPhone() != null ? asha.getPhone() : "Not provided") + "\n\n" +
                "Performance:\n" +
                "• Total Patients: " + patientCount + "\n" +
                "• High Risk Cases: " + highRiskCount + "\n" +
                "• Follow-ups: N/A\n" +
                "• Home Visits: N/A";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("ASHA Performance Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Message", (dialog, which) ->
                        Toast.makeText(requireContext(), "Send message to " + asha.getName(),
                                Toast.LENGTH_SHORT).show())
                .show();
    }

    public void refreshData() { loadAShaData(); }
}