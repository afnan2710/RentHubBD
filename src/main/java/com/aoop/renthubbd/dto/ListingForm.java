package com.aoop.renthubbd.dto;

import com.aoop.renthubbd.model.PropertyCategory;
import com.aoop.renthubbd.model.PropertyType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ListingForm {

    private Long id;
    private String title;
    private String description;
    private PropertyCategory category;
    private PropertyType propertyType;

    private String address;
    private String city;
    private String area;
    private String postalCode;

    private Double monthlyRent;
    private Double serviceCharge;
    private Double parkingFee;
    private Double utilityEstimate;
    private Integer advanceMonths;

    private Double sizeSqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer floorNumber;
    private Integer totalFloors;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableFrom;

    private List<String> facilities = new ArrayList<>();

    private MultipartFile[] photos;
}