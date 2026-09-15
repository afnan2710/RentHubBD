package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.service.AdminListingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/listings")
public class AdminListingController {

    private final AdminListingService listingService;

    public AdminListingController(AdminListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public String pending(@RequestParam(required = false) String category,
                          @RequestParam(required = false) String city,
                          @RequestParam(required = false) String keyword,
                          HttpSession session, Model model) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";

        List<Property> filtered = filterListings(listingService.getPending(), category, city, keyword);

        model.addAttribute("activeNav", "listings");
        model.addAttribute("adminUsername", actor.getUsername());
        model.addAttribute("adminRole", actor.getRole().name());
        model.addAttribute("listings", filtered);
        model.addAttribute("mode", "pending");
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterCity", city);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("categories", com.aoop.renthubbd.model.PropertyCategory.values());
        return "admin-listings";
    }

    @GetMapping("/all")
    public String all(@RequestParam(required = false) String status,
                      @RequestParam(required = false) String category,
                      @RequestParam(required = false) String city,
                      @RequestParam(required = false) String keyword,
                      HttpSession session, Model model) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";

        List<Property> base = listingService.getAll();
        List<Property> filtered = filterListings(base, category, city, keyword);
        filtered = filterByStatus(filtered, status);

        model.addAttribute("activeNav", "listings-all");
        model.addAttribute("adminUsername", actor.getUsername());
        model.addAttribute("adminRole", actor.getRole().name());
        model.addAttribute("listings", filtered);
        model.addAttribute("mode", "all");
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterCity", city);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("categories", com.aoop.renthubbd.model.PropertyCategory.values());
        model.addAttribute("statuses", com.aoop.renthubbd.model.ListingStatus.values());
        return "admin-listings";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, HttpSession session, Model model) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";

        Property p;
        try {
            p = listingService.getById(id);
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/listings";
        }

        model.addAttribute("activeNav", "listings");
        model.addAttribute("adminUsername", actor.getUsername());
        model.addAttribute("adminRole", actor.getRole().name());
        model.addAttribute("property", p);
        return "admin-listing-detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "pending") String from,
                          HttpSession session,
                          RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        try {
            listingService.approve(id, actor);
            ra.addFlashAttribute("success", "Listing approved and published. Owner notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "all".equals(from) ? "redirect:/admin/listings/all" : "redirect:/admin/listings";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         @RequestParam(defaultValue = "pending") String from,
                         HttpSession session,
                         RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        try {
            listingService.reject(id, reason, actor);
            ra.addFlashAttribute("success", "Listing rejected. Owner notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "all".equals(from) ? "redirect:/admin/listings/all" : "redirect:/admin/listings";
    }


    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id,
                          @RequestParam(defaultValue = "detail") String from,
                          HttpSession session,
                          RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        try {
            listingService.archive(id, actor);
            ra.addFlashAttribute("success", "Listing archived. Owner notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "detail".equals(from)
                ? "redirect:/admin/listings/" + id
                : "redirect:/admin/listings/all";
    }

    @PostMapping("/{id}/unarchive")
    public String unarchive(@PathVariable Long id,
                            @RequestParam(defaultValue = "detail") String from,
                            HttpSession session,
                            RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        try {
            listingService.unarchive(id, actor);
            ra.addFlashAttribute("success", "Listing restored and published again.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "detail".equals(from)
                ? "redirect:/admin/listings/" + id
                : "redirect:/admin/listings/all";
    }

    private List<Property> filterListings(List<Property> input, String category, String city, String keyword) {
        List<Property> out = new ArrayList<>();
        String cat = category != null && !category.isBlank() && !"ALL".equals(category)
                ? category.toLowerCase() : null;
        String cty = city != null && !city.isBlank() ? city.toLowerCase() : null;
        String kw  = keyword != null && !keyword.isBlank() ? keyword.toLowerCase() : null;

        for (Property p : input) {
            if (cat != null && (p.getCategory() == null
                    || !p.getCategory().name().toLowerCase().equals(cat))) continue;
            if (cty != null && (p.getCity() == null
                    || !p.getCity().toLowerCase().contains(cty))) continue;
            if (kw != null && (p.getTitle() == null
                    || !p.getTitle().toLowerCase().contains(kw))) continue;
            out.add(p);
        }
        return out;
    }

    private List<Property> filterByStatus(List<Property> input, String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) return input;
        List<Property> out = new ArrayList<>();
        for (Property p : input) {
            if (p.getStatus() != null && p.getStatus().name().equals(status)) out.add(p);
        }
        return out;
    }

    private Admin currentAdmin(HttpSession session) {
        Object id = session.getAttribute("adminId");
        Object role = session.getAttribute("adminRole");
        if (id == null || role == null) return null;
        Admin a = new Admin();
        a.setId((Long) id);
        a.setUsername(session.getAttribute("adminUsername").toString());
        a.setRole(AdminRole.valueOf(role.toString()));
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }
}