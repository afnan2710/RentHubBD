package com.aoop.renthubbd.model;

public enum ListingStatus {
    PENDING("Pending Review"),
    PUBLISHED("Published"),
    REJECTED("Rejected"),
    ARCHIVED("Archived");

    private final String label;
    ListingStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}