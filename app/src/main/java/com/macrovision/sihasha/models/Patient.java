package com.macrovision.sihasha.models;

public class Patient {
    private String id;
    private String name;
    private String husbandName;
    private int age;
    private String phoneNumber;
    private String address;
    private String village;
    private String block;
    private String district;
    private String aadharNumber;
    private String bankAccount;
    private String ifscCode;
    private String religion;
    private String caste;
    private String education;
    private String occupation;
    private String economicStatus;
    private int pregnancyNumber;
    private int liveChildren;
    private int previousAbortions;
    private String lmpDate;
    private String eddDate;
    private String pregnancyStatus;
    private String[] riskFactors;
    private int height;
    private String bloodGroup;
    private String registrationDate;
    private String ashaId;
    private String phcId;
    private boolean isHighRisk;
    private String assignedDoctor;
    private String lastVisit;

    // Constructors
    public Patient() {}

    public Patient(String id, String name, int age, String phoneNumber, String village,
                   String pregnancyStatus, String ashaId, String phcId, boolean isHighRisk) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
        this.village = village;
        this.pregnancyStatus = pregnancyStatus;
        this.ashaId = ashaId;
        this.phcId = phcId;
        this.isHighRisk = isHighRisk;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHusbandName() { return husbandName; }
    public void setHusbandName(String husbandName) { this.husbandName = husbandName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getCaste() { return caste; }
    public void setCaste(String caste) { this.caste = caste; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getEconomicStatus() { return economicStatus; }
    public void setEconomicStatus(String economicStatus) { this.economicStatus = economicStatus; }

    public int getPregnancyNumber() { return pregnancyNumber; }
    public void setPregnancyNumber(int pregnancyNumber) { this.pregnancyNumber = pregnancyNumber; }

    public int getLiveChildren() { return liveChildren; }
    public void setLiveChildren(int liveChildren) { this.liveChildren = liveChildren; }

    public int getPreviousAbortions() { return previousAbortions; }
    public void setPreviousAbortions(int previousAbortions) { this.previousAbortions = previousAbortions; }

    public String getLmpDate() { return lmpDate; }
    public void setLmpDate(String lmpDate) { this.lmpDate = lmpDate; }

    public String getEddDate() { return eddDate; }
    public void setEddDate(String eddDate) { this.eddDate = eddDate; }

    public String getPregnancyStatus() { return pregnancyStatus; }
    public void setPregnancyStatus(String pregnancyStatus) { this.pregnancyStatus = pregnancyStatus; }

    public String[] getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String[] riskFactors) { this.riskFactors = riskFactors; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }

    public String getAshaId() { return ashaId; }
    public void setAshaId(String ashaId) { this.ashaId = ashaId; }

    public String getPhcId() { return phcId; }
    public void setPhcId(String phcId) { this.phcId = phcId; }

    public boolean isHighRisk() { return isHighRisk; }
    public void setHighRisk(boolean highRisk) { isHighRisk = highRisk; }

    public String getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(String assignedDoctor) { this.assignedDoctor = assignedDoctor; }

    public String getLastVisit() { return lastVisit; }
    public void setLastVisit(String lastVisit) { this.lastVisit = lastVisit; }
}
