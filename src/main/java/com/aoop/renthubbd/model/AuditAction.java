package com.aoop.renthubbd.model;

public enum AuditAction {
    LOGIN_SUCCESS("Login success"),
    LOGIN_FAILED("Login failed"),
    LOGIN_BLOCKED("Login blocked"),
    LOGOUT("Logout"),
    ADMIN_CREATED("Admin created"),
    ADMIN_STATUS_CHANGED("Admin status changed"),
    USER_ACTIVATED("User activated"),
    USER_DEACTIVATED("User deactivated"),
    LISTING_APPROVED("Listing approved"),
    LISTING_REJECTED("Listing rejected"),
    LISTING_ARCHIVED("Listing archived"),
    LISTING_UNARCHIVED("Listing unarchived"),
    PASSWORD_CHANGED("Password changed"),
    DATA_EXPORTED("Data exported");

    private final String label;

    AuditAction(String label) { this.label = label; }
    public String getLabel() { return label; }
}