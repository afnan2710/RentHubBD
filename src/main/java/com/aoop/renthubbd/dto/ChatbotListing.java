package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatbotListing {
    private Long id;
    private String title;
    private String url;
    private String photo;
    private String city;
    private String area;
    private String typeLabel;
    private String categoryLabel;
    private Double rent;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double sizeSqft;
    private List<String> facilities;
}