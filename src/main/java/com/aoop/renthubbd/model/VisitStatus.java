package com.aoop.renthubbd.model;

public enum VisitStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    RESCHEDULED("Rescheduled"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String label;

    VisitStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}