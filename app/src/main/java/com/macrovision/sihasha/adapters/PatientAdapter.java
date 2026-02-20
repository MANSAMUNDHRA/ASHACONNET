package com.macrovision.sihasha.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.Patient;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {

    private List<Patient> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
        void onPatientLongClick(Patient patient);
    }

    public PatientAdapter(List<Patient> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
        Log.d("PatientAdapter", "Constructor: " + patients.size() + " patients");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("PatientAdapter", "onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_patient_card, parent, false); // ✅ Fixed layout name
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patients.get(position);
        Log.d("PatientAdapter", "Binding patient at position " + position + ": " + patient.getName());
        holder.bind(patient, listener);
    }

    @Override
    public int getItemCount() {
        Log.d("PatientAdapter", "getItemCount: " + patients.size());
        return patients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatientName, tvPatientAge, tvPatientVillage,
                tvPatientPhone, tvPatientEdd, tvPregnancyStatus;
        private LinearLayout layoutEdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvPatientAge = itemView.findViewById(R.id.tv_patient_age);
            tvPatientVillage = itemView.findViewById(R.id.tv_patient_village);
            tvPatientPhone = itemView.findViewById(R.id.tv_patient_phone);
            tvPatientEdd = itemView.findViewById(R.id.tv_patient_edd);
            tvPregnancyStatus = itemView.findViewById(R.id.tv_pregnancy_status);
            layoutEdd = itemView.findViewById(R.id.layout_edd); // ✅ Added this
        }

        public void bind(Patient patient, OnPatientClickListener listener) {
            Log.d("PatientAdapter", "Binding patient: " + patient.getName());

            tvPatientName.setText(patient.getName());
            tvPatientAge.setText(patient.getAge() + " years");
            tvPatientVillage.setText(patient.getVillage());
            tvPatientPhone.setText(patient.getPhoneNumber());
            tvPregnancyStatus.setText(patient.getPregnancyStatus());

            // ✅ Fixed EDD handling
            if ("pregnant".equals(patient.getPregnancyStatus()) && patient.getEddDate() != null) {
                tvPatientEdd.setText(patient.getEddDate());
                layoutEdd.setVisibility(View.VISIBLE);
            } else {
                layoutEdd.setVisibility(View.GONE);
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPatientClick(patient);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onPatientLongClick(patient);
                }
                return true;
            });
        }
    }
}
