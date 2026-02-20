package com.macrovision.sihasha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.macrovision.sihasha.R;
import com.macrovision.sihasha.models.Staff;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private List<Staff> staffList;
    private OnStaffClickListener listener;

    public interface OnStaffClickListener {
        void onStaffClick(Staff staff);
        void onStaffLongClick(Staff staff);
    }

    public StaffAdapter(List<Staff> staffList, OnStaffClickListener listener) {
        this.staffList = staffList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Staff staff = staffList.get(position);
        holder.bind(staff, listener);
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvStaffName, tvStaffRole, tvStaffPhone, tvStaffLocation;
        private TextView tvStaffStatus, tvStaffPerformance, tvStaffExperience;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStaffName = itemView.findViewById(R.id.tv_staff_name);
            tvStaffRole = itemView.findViewById(R.id.tv_staff_role);
            tvStaffPhone = itemView.findViewById(R.id.tv_staff_phone);
            tvStaffLocation = itemView.findViewById(R.id.tv_staff_location);
            tvStaffStatus = itemView.findViewById(R.id.tv_staff_status);
            tvStaffPerformance = itemView.findViewById(R.id.tv_staff_performance);
            tvStaffExperience = itemView.findViewById(R.id.tv_staff_experience);
        }

        public void bind(Staff staff, OnStaffClickListener listener) {
            tvStaffName.setText(staff.getName());
            tvStaffRole.setText(getRoleDisplayName(staff.getRole()));
            tvStaffPhone.setText(staff.getPhone());

            // Location
            String location = "";
            if (staff.getVillage() != null && !staff.getVillage().isEmpty()) {
                location = staff.getVillage();
                if (staff.getDistrict() != null && !staff.getDistrict().isEmpty()) {
                    location += ", " + staff.getDistrict();
                }
            } else if (staff.getPhcName() != null) {
                location = staff.getPhcName();
            }
            tvStaffLocation.setText(location.isEmpty() ? "Location not specified" : location);

            // Status
            tvStaffStatus.setText(getStatusDisplayName(staff.getStatus()));
            setStatusColor(tvStaffStatus, staff.getStatus());

            // Performance
            if (staff.getPerformance() != null) {
                double efficiency = staff.getPerformance().getEfficiency();
                tvStaffPerformance.setText(String.format("%.1f%% efficiency", efficiency));
                setPerformanceColor(tvStaffPerformance, efficiency);
            } else {
                tvStaffPerformance.setText("No performance data");
                tvStaffPerformance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_text_secondary));
            }

            // Experience
            if (staff.getExperience() != null && !staff.getExperience().isEmpty()) {
                tvStaffExperience.setText(staff.getExperience());
            } else {
                tvStaffExperience.setText("Experience not specified");
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStaffClick(staff);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onStaffLongClick(staff);
                }
                return true;
            });
        }

        private String getRoleDisplayName(String role) {
            switch (role) {
                case "asha": return "ASHA Worker";
                case "phcdoctor": return "PHC Doctor";
                case "phcnurse": return "PHC Nurse";
                case "phcadmin": return "PHC Administrator";
                default: return role;
            }
        }

        private String getStatusDisplayName(String status) {
            switch (status) {
                case "active": return "Active";
                case "on_leave": return "On Leave";
                case "inactive": return "Inactive";
                default: return status;
            }
        }

        private void setStatusColor(TextView textView, String status) {
            int colorRes;
            switch (status) {
                case "active":
                    colorRes = R.color.color_success;
                    break;
                case "on_leave":
                    colorRes = R.color.color_warning;
                    break;
                case "inactive":
                    colorRes = R.color.color_error;
                    break;
                default:
                    colorRes = R.color.color_text_secondary;
                    break;
            }
            textView.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
        }

        private void setPerformanceColor(TextView textView, double efficiency) {
            int colorRes;
            if (efficiency >= 90) {
                colorRes = R.color.color_success;
            } else if (efficiency >= 70) {
                colorRes = R.color.color_warning;
            } else {
                colorRes = R.color.color_error;
            }
            textView.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
        }
    }
}
