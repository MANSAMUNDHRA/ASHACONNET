package com.macrovision.sihasha.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.Patient;

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {

    private static final String TAG = "PatientAdapter";
    private List<Patient> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
        void onPatientLongClick(Patient patient);
    }

    public PatientAdapter(List<Patient> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
        Log.d(TAG, "Adapter created with " + (patients != null ? patients.size() : 0) + " patients");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_item_patient_card, parent, false);
            return new ViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating view holder: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (patients == null || position >= patients.size()) {
                Log.e(TAG, "Invalid position or patients list");
                return;
            }
            
            Patient patient = patients.get(position);
            if (patient == null) {
                Log.e(TAG, "Patient is null at position " + position);
                return;
            }
            
            Log.d(TAG, "Binding patient at position " + position + ": " + patient.getName());
            
            holder.tvPatientName.setText(patient.getName() != null ? patient.getName() : "Unknown");
            holder.tvPatientAge.setText(patient.getAge() + " years");
            holder.tvPatientVillage.setText(patient.getVillage() != null ? patient.getVillage() : "N/A");
            holder.tvPatientPhone.setText(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "N/A");
            
            // Handle item clicks
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPatientClick(patient);
                }
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onPatientLongClick(patient);
                }
                return true;
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return patients != null ? patients.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvPatientAge, tvPatientVillage, tvPatientPhone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                tvPatientName = itemView.findViewById(R.id.tv_patient_name);
                tvPatientAge = itemView.findViewById(R.id.tv_patient_age);
                tvPatientVillage = itemView.findViewById(R.id.tv_patient_village);
                tvPatientPhone = itemView.findViewById(R.id.tv_patient_phone);
                
                Log.d(TAG, "View holder views initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing view holder: " + e.getMessage(), e);
            }
        }
    }
}