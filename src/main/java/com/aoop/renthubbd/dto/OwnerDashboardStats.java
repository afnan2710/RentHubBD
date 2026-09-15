package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerDashboardStats {
    private long totalListings;
    private long publishedListings;
    private long pendingListings;
    private long rejectedListings;
    private long pendingVisits;
    private long unreadMessages;
    private Double averageRating;
}