package com.aoop.renthubbd.dto;

import com.aoop.renthubbd.model.PropertyCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingSearchFilter {
    private String q;
    private String city;
    private PropertyCategory category;
    private Double minRent;
    private Double maxRent;
    private Integer minBedrooms;
    private Double minSize;
    private String sort = "newest";

    public boolean isEmpty() {
        return (q == null || q.isBlank())
                && (city == null || city.isBlank())
                && category == null
                && minRent == null && maxRent == null
                && minBedrooms == null
                && minSize == null;
    }
}