package com.aoop.renthubbd.model;

public enum NotificationType {
    LISTING_APPROVED("Listing approved"),
    LISTING_REJECTED("Listing rejected"),
    LISTING_ARCHIVED("Listing archived"),
    LISTING_UNARCHIVED("Listing unarchived"),
    VISIT_REQUESTED("Visit requested"),
    VISIT_ACCEPTED("Visit accepted"),
    VISIT_REJECTED("Visit rejected"),
    VISIT_RESCHEDULED("Visit rescheduled"),
    VISIT_CANCELLED("Visit cancelled"),
    NEW_MESSAGE("New message"),
    NEW_REVIEW("New review"),
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