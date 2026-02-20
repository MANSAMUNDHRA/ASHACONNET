package com.macrovision.sihasha.models;

import java.util.Map;

public class InventoryItem {
    private String id;
    private String name;
    private String category;
    private int currentStock;
    private int minimumStock;
    private String expiryDate;
    private String batchNumber;
    private String supplier;
    private double costPerUnit;
    private String storageTemp;
    private String manufacturer;
    private Map<String, Integer> phcDistribution;

    // Constructors
    public InventoryItem() {}

    public InventoryItem(String id, String name, String category, int currentStock,
                         int minimumStock, String expiryDate, String batchNumber,
                         String supplier, double costPerUnit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.currentStock = currentStock;
        this.minimumStock = minimumStock;
        this.expiryDate = expiryDate;
        this.batchNumber = batchNumber;
        this.supplier = supplier;
        this.costPerUnit = costPerUnit;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    public int getMinimumStock() { return minimumStock; }
    public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(double costPerUnit) { this.costPerUnit = costPerUnit; }

    public String getStorageTemp() { return storageTemp; }
    public void setStorageTemp(String storageTemp) { this.storageTemp = storageTemp; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Map<String, Integer> getPhcDistribution() { return phcDistribution; }
    public void setPhcDistribution(Map<String, Integer> phcDistribution) { this.phcDistribution = phcDistribution; }

    // Utility methods
    public boolean isLowStock() {
        return currentStock <= minimumStock;
    }

    public boolean isOutOfStock() {
        return currentStock <= 0;
    }

    public String getStockStatus() {
        if (isOutOfStock()) return "Out of Stock";
        if (isLowStock()) return "Low Stock";
        return "In Stock";
    }
}
