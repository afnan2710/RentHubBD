package com.aoop.renthubbd.model;

public enum NotificationType {
    LISTING_APPROVED("Listing approved"),
    LISTING_REJECTED("Listing rejected"),
    LISTING_ARCHIVED("Listing archived"),
    LISTING_UNARCHIVED("Listing unarchived"),
    LISTING_SUBMITTED("Listing submitted"),
    LISTING_UPDATED("Listing updated"),
    ACCOUNT_ACTIVATED("Account activated"),
    ACCOUNT_DEACTIVATED("Account deactivated"),
    ADMIN_CREATED("Admin account created"),
    ADMIN_STATUS_CHANGED("Admin status changed");

    private final String label;

    NotificationType(String label) { this.label = label; }
    public String getLabel() { return label; }
}