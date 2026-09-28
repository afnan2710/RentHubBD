package com.aoop.renthubbd.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "properties")
@Getter
@Setter
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.PENDING;

    // Location
    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String area;

    @Column(name = "postal_code")
    private String postalCode;

    // Pricing
    @Column(name = "monthly_rent", nullable = false)
    private Double monthlyRent;

    @Column(name = "service_charge")
    private Double serviceCharge;

    @Column(name = "parking_fee")
    private Double parkingFee;

    @Column(name = "utility_estimate")
    private Double utilityEstimate;

    @Column(name = "advance_months")
    private Integer advanceMonths;

    // Size & rooms
    @Column(name = "size_sqft")
    private Double sizeSqft;

    private Integer bedrooms;
    private Integer bathrooms;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    // Facilities
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_facilities", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "facility")
    private Set<String> facilities = new HashSet<>();

    // Photo file paths relative to uploads dir
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_photos", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "photo_path")
    private List<String> photos = new ArrayList<>();

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Transparent total cost, addresses the hidden costs problem.
    @Transient
    public double getTotalMonthlyCost() {
        double total = monthlyRent != null ? monthlyRent : 0;
        if (serviceCharge   != null) total += serviceCharge;
        if (parkingFee      != null) total += parkingFee;
        if (utilityEstimate != null) total += utilityEstimate;
        return total;
    }

    @Transient
    public String getPrimaryPhoto() {
        return photos.isEmpty() ? null : photos.get(0);
    }
}