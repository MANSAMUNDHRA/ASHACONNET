package com.macrovision.sihasha.models;

public class Staff {
    private String id;
    private String name;
    private String role; // "asha", "phcdoctor", "phcnurse", "phcadmin"
    private String phone;
    private String email;
    private String village;
    private String block;
    private String district;
    private String state;
    private String phcId;
    private String phcName;
    private String qualification;
    private String experience;
    private String joiningDate;
    private String lastTraining;
    private String assignedPopulation;
    private String assignedFamilies;
    private String specialization;
    private String responsibilities;
    private Performance performance;
    private String status; // "active", "inactive", "on_leave"
    private String designation;
    private String managedPHCs;

    // Performance sub-class
    public static class Performance {
        private int monthlyTarget;
        private int achieved;
        private double efficiency;
        private int patientsHandled;
        private int trainingsCompleted;

        public Performance(int monthlyTarget, int achieved, double efficiency, int patientsHandled, int trainingsCompleted) {
            this.monthlyTarget = monthlyTarget;
            this.achieved = achieved;
            this.efficiency = efficiency;
            this.patientsHandled = patientsHandled;
            this.trainingsCompleted = trainingsCompleted;
        }

        // Getters and setters
        public int getMonthlyTarget() { return monthlyTarget; }
        public void setMonthlyTarget(int monthlyTarget) { this.monthlyTarget = monthlyTarget; }
        public int getAchieved() { return achieved; }
        public void setAchieved(int achieved) { this.achieved = achieved; }
        public double getEfficiency() { return efficiency; }
        public void setEfficiency(double efficiency) { this.efficiency = efficiency; }
        public int getPatientsHandled() { return patientsHandled; }
        public void setPatientsHandled(int patientsHandled) { this.patientsHandled = patientsHandled; }
        public int getTrainingsCompleted() { return trainingsCompleted; }
        public void setTrainingsCompleted(int trainingsCompleted) { this.trainingsCompleted = trainingsCompleted; }
    }

    // Constructors
    public Staff() {}

    public Staff(String id, String name, String role, String phone, String village, String district) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.village = village;
        this.district = district;
        this.status = "active";
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPhcId() { return phcId; }
    public void setPhcId(String phcId) { this.phcId = phcId; }
    public String getPhcName() { return phcName; }
    public void setPhcName(String phcName) { this.phcName = phcName; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }
    public String getLastTraining() { return lastTraining; }
    public void setLastTraining(String lastTraining) { this.lastTraining = lastTraining; }
    public String getAssignedPopulation() { return assignedPopulation; }
    public void setAssignedPopulation(String assignedPopulation) { this.assignedPopulation = assignedPopulation; }
    public String getAssignedFamilies() { return assignedFamilies; }
    public void setAssignedFamilies(String assignedFamilies) { this.assignedFamilies = assignedFamilies; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getManagedPHCs() { return managedPHCs; }
    public void setManagedPHCs(String managedPHCs) { this.managedPHCs = managedPHCs; }
}
