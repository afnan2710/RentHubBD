package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.ListingSearchFilter;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.model.PropertyCategory;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class ExploreController {

    private final PublicSearchService searchService;
    private final PropertyRepository propertyRepository;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final ChatService chatService;

    public ExploreController(PublicSearchService searchService,
                             PropertyRepository propertyRepository,
                             ReviewService reviewService,
                             FavoriteService favoriteService,
                             ChatService chatService) {
        this.searchService = searchService;
        this.propertyRepository = propertyRepository;
        this.reviewService = reviewService;
        this.favoriteService = favoriteService;
        this.chatService = chatService;
    }

    @GetMapping("/explore")
    public String explore(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minRent", required = false) Double minRent,
            @RequestParam(name = "maxRent", required = false) Double maxRent,
            @RequestParam(name = "minBedrooms", required = false) Integer minBedrooms,
            @RequestParam(name = "minSize", required = false) Double minSize,
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            HttpSession session, Model model) {

        ListingSearchFilter filter = new ListingSearchFilter();
        filter.setQ(q);
        filter.setCity(city);
        filter.setMinRent(minRent);
        filter.setMaxRent(maxRent);
        filter.setMinBedrooms(minBedrooms);
        filter.setMinSize(minSize);
        filter.setSort(sort);

        if (category != null && !category.isBlank()) {
            try {
                filter.setCategory(PropertyCategory.valueOf(category.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) { }
        }

        List<Property> listings = searchService.search(filter);

        model.addAttribute("listings", listings);
        model.addAttribute("cities", searchService.distinctCities());
        model.addAttribute("categories", PropertyCategory.values());
        model.addAttribute("filter", filter);
        model.addAttribute("hasFilters", !filter.isEmpty());
        model.addAttribute("resultCount", listings.size());

        Long userId = currentUserId(session);
        if (userId != null) {
            Set<Long> favIds = new HashSet<>();
            for (var f : favoriteService.listForUser(userId)) favIds.add(f.getProperty().getId());
            model.addAttribute("favoriteIds", favIds);
        }
        return "explore";
    }

    @GetMapping("/listing/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        Property property = propertyRepository.findById(id).orElse(null);
        if (property == null) return "redirect:/explore";

        model.addAttribute("property", property);
        model.addAttribute("reviews", reviewService.getForProperty(id));
        model.addAttribute("avgRating", reviewService.averageRating(id));
        model.addAttribute("ratingDist", reviewService.ratingDistribution(id));
        model.addAttribute("reviewCount", reviewService.getForProperty(id).size());

        Long userId = currentUserId(session);
        if (userId != null) {
            model.addAttribute("isOwnerOfListing",
                    property.getOwner() != null && property.getOwner().getId().equals(userId));
            model.addAttribute("hasReviewed", reviewService.hasReviewed(id, userId));
            model.addAttribute("isFavorited", favoriteService.isFavorited(userId, id));
        }
        return "listing-detail";
    }

    @PostMapping("/listing/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id,
                                 @RequestParam(defaultValue = "/explore") String from,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            favoriteService.toggle(userId, id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + from;
    }

    @PostMapping("/listing/{id}/message")
    public String startChat(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            var conv = chatService.getOrCreate(id, userId);
            return "redirect:/chat/" + conv.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/listing/" + id;
        }
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}