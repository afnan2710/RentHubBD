package com.aoop.renthubbd.model;

public enum AdminRole {
    SUPERADMIN("Superadmin"),
    ADMIN("Admin");

    private final String label;

    AdminRole(String label) { this.label = label; }
    public String getLabel() { return label; }
}