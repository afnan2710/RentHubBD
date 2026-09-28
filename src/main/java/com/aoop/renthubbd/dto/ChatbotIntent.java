package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotIntent {
    private String intent = "search";
    private String category;
    private String propertyType;
    private Integer bedrooms;
    private Integer bathrooms;
    private String city;
    private String area;
    private Double minRent;
    private Double maxRent;
    private Double minSize;
}