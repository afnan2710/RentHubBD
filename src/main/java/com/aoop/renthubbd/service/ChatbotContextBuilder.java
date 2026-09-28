package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotContextBuilder {

    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    public ChatbotContextBuilder(PropertyRepository propertyRepository,
                                 ObjectMapper objectMapper) {
        this.propertyRepository = propertyRepository;
        this.objectMapper = objectMapper;
    }

    public String buildCatalog() {
        List<Property> listings = propertyRepository
                .findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED);

        List<Map<String, Object>> compact = new ArrayList<>();
        for (Property p : listings) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("title", p.getTitle());
            row.put("category", p.getCategory() != null ? p.getCategory().name() : null);
            row.put("type", p.getPropertyType() != null ? p.getPropertyType().name() : null);
            row.put("city", p.getCity());
            row.put("area", p.getArea());
            row.put("rent", p.getMonthlyRent());
            row.put("bedrooms", p.getBedrooms());
            row.put("bathrooms", p.getBathrooms());
            row.put("size_sqft", p.getSizeSqft());
            row.put("facilities", p.getFacilities() != null ? p.getFacilities() : List.of());
            compact.add(row);
        }

        try {
            return objectMapper.writeValueAsString(compact);
        } catch (Exception e) {
            return "[]";
        }
    }
}