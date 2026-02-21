package com.macrovision.sihasha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.Staff;

import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private List<Staff> staffList;

    public StaffAdapter(List<Staff> staffList) {
        this.staffList = staffList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Staff staff = staffList.get(position);
        
        holder.tvName.setText(staff.getName() != null ? staff.getName() : "Unknown");
        holder.tvRole.setText(formatRole(staff.getRole()));
        holder.tvPhone.setText(staff.getPhone() != null ? staff.getPhone() : "");
        holder.tvLocation.setText(staff.getVillage() != null ? staff.getVillage() : "Not assigned");
    }

    private String formatRole(String role) {
        if (role == null) return "Unknown";
        switch (role) {
            case "asha": return "ASHA Worker";
            case "phcdoctor": return "PHC Doctor";
            case "phcnurse": return "PHC Nurse";
            case "phcadmin": return "PHC Administrator";
            default: return role;
        }
    }

    @Override
    public int getItemCount() {
        return staffList != null ? staffList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvPhone, tvLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_staff_name);
            tvRole = itemView.findViewById(R.id.tv_staff_role);
            tvPhone = itemView.findViewById(R.id.tv_staff_phone);
            tvLocation = itemView.findViewById(R.id.tv_staff_location);
        }
    }
}