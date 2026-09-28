package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardStats {
    private long totalUsers;
    private long totalRenters;
    private long totalOwners;
    private long inactiveUsers;
    private long totalListings;
    private long pendingListings;
    private long publishedListings;
    private long rejectedListings;
    private long totalAdmins;
    private long activeAdmins;
}