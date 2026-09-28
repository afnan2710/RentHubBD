package com.aoop.renthubbd.model;

public enum PropertyCategory {
    RESIDENTIAL("Residential"),
    COMMERCIAL("Commercial"),
    INDUSTRIAL("Industrial & Storage"),
    OTHER("Other Rentable Spaces");

    private final String label;

    PropertyCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
}