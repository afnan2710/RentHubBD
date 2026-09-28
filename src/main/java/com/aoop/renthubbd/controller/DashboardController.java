package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.service.ListingService;
import com.aoop.renthubbd.service.OwnerDashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Controller
public class DashboardController {

    private final ListingService listingService;
    private final OwnerDashboardService ownerDashboardService;

    public DashboardController(ListingService listingService,
                               OwnerDashboardService ownerDashboardService) {
        this.listingService = listingService;
        this.ownerDashboardService = ownerDashboardService;
    }

    @GetMapping("/owner/dashboard")
    public String ownerDashboard(HttpSession session, Model model) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("stats", ownerDashboardService.getStats(ownerId));
        model.addAttribute("visitSummary", ownerDashboardService.visitSummary(ownerId));
        model.addAttribute("recentListings", listingService.getRecentListings(ownerId, 5));
        model.addAttribute("bdDateTime", ZonedDateTime.now(ZoneId.of("Asia/Dhaka")));
        return "owner-dashboard";
    }

    private Long currentOwnerId(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        return (Long) id;
    }
}