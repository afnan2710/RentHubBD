package com.aoop.renthubbd.model;

public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String label;

    AccountStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}