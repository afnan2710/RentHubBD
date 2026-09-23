package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.ListingForm;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.model.PropertyType;
import com.aoop.renthubbd.service.ListingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/owner/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public String myListings(HttpSession session, Model model) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        List<Property> listings = listingService.getOwnerListings(ownerId);
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "listings");
        model.addAttribute("listings", listings);
        model.addAttribute("statusSummary", listingService.getOwnerStatusSummary(ownerId));
        return "owner-listings";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        if (!model.containsAttribute("listingForm")) {
            model.addAttribute("listingForm", new ListingForm());
        }
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "new-listing");
        model.addAttribute("propertyTypes", propertyTypesForJs());
        model.addAttribute("mode", "create");
        return "owner-new-listing";
    }

    @PostMapping("/new")
    public String createListing(
            @ModelAttribute("listingForm") ListingForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        String validationError = validate(form);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            redirectAttributes.addFlashAttribute("listingForm", form);
            return "redirect:/owner/listings/new";
        }

        try {
            listingService.createListing(ownerId, form);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not save one or more photos. Please try again.");
            redirectAttributes.addFlashAttribute("listingForm", form);
            return "redirect:/owner/listings/new";
        }

        redirectAttributes.addFlashAttribute("success",
                "Listing submitted. It will appear on your dashboard once an admin approves it.");
        return "redirect:/owner/listings";
    }

    private List<Map<String, String>> propertyTypesForJs() {
        List<Map<String, String>> out = new ArrayList<>();
        for (PropertyType t : PropertyType.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("value", t.name());
            m.put("label", t.getLabel());
            m.put("category", t.getCategory().name());
            out.add(m);
        }
        return out;
    }

    private String validate(ListingForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank()) return "Title is required.";
        if (f.getCategory() == null) return "Please choose a category.";
        if (f.getPropertyType() == null) return "Please choose a property type.";
        if (f.getAddress() == null || f.getAddress().isBlank()) return "Address is required.";
        if (f.getCity() == null || f.getCity().isBlank()) return "City is required.";
        if (f.getArea() == null || f.getArea().isBlank()) return "Area / Thana is required.";
        if (f.getMonthlyRent() == null || f.getMonthlyRent() <= 0) return "Monthly rent must be greater than zero.";
        if (f.getPropertyType().getCategory() != f.getCategory())
            return "Selected property type does not match the chosen category.";
        return null;
    }

    private Long requireOwner(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        return (Long) id;
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        Property p;
        try {
            p = listingService.getOwnedListing(id, ownerId);
        } catch (IllegalArgumentException e) {
            return "redirect:/owner/listings";
        }

        if (!model.containsAttribute("listingForm")) {
            model.addAttribute("listingForm", listingService.toForm(p));
        }
        model.addAttribute("propertyTypes", propertyTypesForJs());
        model.addAttribute("mode", "edit");
        model.addAttribute("existingPhotos", p.getPhotos());
        model.addAttribute("currentStatus", p.getStatus());
        model.addAttribute("rejectionReason", p.getRejectionReason());
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "listings");
        return "owner-new-listing";
    }

    @PostMapping("/{id}/edit")
    public String updateListing(
            @PathVariable Long id,
            @ModelAttribute("listingForm") ListingForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        String validationError = validate(form);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            redirectAttributes.addFlashAttribute("listingForm", form);
            return "redirect:/owner/listings/" + id + "/edit";
        }

        try {
            listingService.updateListing(ownerId, id, form);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not save one or more photos. Please try again.");
            redirectAttributes.addFlashAttribute("listingForm", form);
            return "redirect:/owner/listings/" + id + "/edit";
        }

        redirectAttributes.addFlashAttribute("success",
                "Listing updated and sent back for admin review.");
        return "redirect:/owner/listings";
    }
}