package com.pda.uhf_g.entity;

public class Equipment {
    private String epc;
    private String itemNumber;
    private String serialNumber;
    private String brand;
    private String model;
    private String type;
    private String room;
    private String description;
    private boolean isFound;
    private int readCount;

    public Equipment(String epc, String itemNumber, String serialNumber, String brand, String model, String type, String room, String description) {
        this.epc = epc;
        this.itemNumber = itemNumber;
        this.serialNumber = serialNumber;
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.room = room;
        this.description = description;
        this.isFound = false;
        this.readCount = 0;
    }

    public String getEpc() { return epc; }
    public void setEpc(String epc) { this.epc = epc; }

    public String getItemNumber() { return itemNumber; }
    public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isFound() { return isFound; }
    public void setFound(boolean found) { isFound = found; }

    public int getReadCount() { return readCount; }
    public void incrementReadCount() { this.readCount++; }
    public void resetReadCount() { this.readCount = 0; }
}
