package com.macrovision.sihasha.models;

public class User {
    private String id;
    private String name;
    private String role;
    private String phone;
    private String village;
    private String block;
    private String district;
    private String state;
    private String phcId;
    private String password;
    private Performance performance;

    // Constructors
    public User() {}

    public User(String id, String name, String role, String phone, String village,
                String block, String district, String state, String phcId, String password) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.village = village;
        this.block = block;
        this.district = district;
        this.state = state;
        this.phcId = phcId;
        this.password = password;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }

    // Inner class for performance metrics
    public static class Performance {
        private int monthlyTarget;
        private int achieved;
        private int efficiency;

        public Performance(int monthlyTarget, int achieved, int efficiency) {
            this.monthlyTarget = monthlyTarget;
            this.achieved = achieved;
            this.efficiency = efficiency;
        }

        // Getters and setters
        public int getMonthlyTarget() { return monthlyTarget; }
        public void setMonthlyTarget(int monthlyTarget) { this.monthlyTarget = monthlyTarget; }

        public int getAchieved() { return achieved; }
        public void setAchieved(int achieved) { this.achieved = achieved; }

        public int getEfficiency() { return efficiency; }
        public void setEfficiency(int efficiency) { this.efficiency = efficiency; }
    }
}