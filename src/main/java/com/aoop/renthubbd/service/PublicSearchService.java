package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.ListingSearchFilter;
import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicSearchService {

    private final PropertyRepository propertyRepository;

    public PublicSearchService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public List<Property> search(ListingSearchFilter filter) {
        List<Property> published = propertyRepository
                .findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED);

        if (filter == null || filter.isEmpty()) {
            return published;
        }

        List<Property> out = new ArrayList<>();
        String q = filter.getQ() != null && !filter.getQ().isBlank()
                ? filter.getQ().toLowerCase() : null;
        String city = filter.getCity() != null && !filter.getCity().isBlank()
                ? filter.getCity().toLowerCase() : null;

        for (Property p : published) {
            if (filter.getCategory() != null && p.getCategory() != filter.getCategory()) continue;
            if (city != null && (p.getCity() == null || !p.getCity().toLowerCase().contains(city))) continue;
            if (filter.getMinRent() != null && (p.getMonthlyRent() == null
                    || p.getMonthlyRent() < filter.getMinRent())) continue;
            if (filter.getMaxRent() != null && (p.getMonthlyRent() == null
                    || p.getMonthlyRent() > filter.getMaxRent())) continue;
            if (filter.getMinBedrooms() != null && (p.getBedrooms() == null
                    || p.getBedrooms() < filter.getMinBedrooms())) continue;
            if (filter.getMinSize() != null && (p.getSizeSqft() == null
                    || p.getSizeSqft() < filter.getMinSize())) continue;

            if (q != null) {
                String haystack = ((p.getTitle() == null ? "" : p.getTitle()) + " "
                        + (p.getDescription() == null ? "" : p.getDescription()) + " "
                        + (p.getArea() == null ? "" : p.getArea()) + " "
                        + (p.getCity() == null ? "" : p.getCity())).toLowerCase();
                if (!haystack.contains(q)) continue;
            }

            out.add(p);
        }

        applySort(out, filter.getSort());
        return out;
    }

    private void applySort(List<Property> list, String sort) {
        if (sort == null) return;
        switch (sort) {
            case "price-low"  -> list.sort(Comparator.comparing(
                    p -> p.getMonthlyRent() == null ? Double.MAX_VALUE : p.getMonthlyRent()));
            case "price-high" -> list.sort(Comparator.comparing(
                    (Property p) -> p.getMonthlyRent() == null ? 0.0 : p.getMonthlyRent()).reversed());
            case "size-high"  -> list.sort(Comparator.comparing(
                    (Property p) -> p.getSizeSqft() == null ? 0.0 : p.getSizeSqft()).reversed());
            default -> { }
        }
    }

    public List<String> distinctCities() {
        return propertyRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED)
                .stream()
                .map(Property::getCity)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}