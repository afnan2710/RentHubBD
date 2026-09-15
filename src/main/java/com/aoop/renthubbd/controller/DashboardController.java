package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.service.ListingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Controller
public class DashboardController {

    private final ListingService listingService;
    public DashboardController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/owner/dashboard")
    public String ownerDashboard(HttpSession session, Model model) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";

        ZonedDateTime bdDateTime = ZonedDateTime.now(ZoneId.of("Asia/Dhaka"));

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("stats", listingService.getDashboardStats(ownerId));
        model.addAttribute("recentListings", listingService.getRecentListings(ownerId, 5));
        model.addAttribute("bdDateTime", bdDateTime);
        return "owner-dashboard";
    }

    @GetMapping("/owner/visit-requests")
    public String ownerVisitRequests(HttpSession session, Model model) {
        if (currentOwnerId(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "visits");
        model.addAttribute("pageTitle", "Visit Requests");
        model.addAttribute("pageMessage", "Soon you'll be able to accept, reject or reschedule visit requests from renters directly from this page.");
        return "owner-coming-soon";
    }

    @GetMapping("/owner/messages")
    public String ownerMessages(HttpSession session, Model model) {
        if (currentOwnerId(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "messages");
        model.addAttribute("pageTitle", "Messages");
        model.addAttribute("pageMessage", "Direct messaging with renters who are interested in your properties is coming soon.");
        return "owner-coming-soon";
    }

    @GetMapping("/owner/reviews")
    public String ownerReviews(HttpSession session, Model model) {
        if (currentOwnerId(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "reviews");
        model.addAttribute("pageTitle", "Reviews");
        model.addAttribute("pageMessage", "Reviews left by renters on your properties will appear here once the review system is built.");
        return "owner-coming-soon";
    }

    @GetMapping("/renter/dashboard")
    public String renterDashboard(HttpSession session, Model model) {
        Object userType = session.getAttribute("userType");
        if (userType == null || !userType.equals("RENTER")) {
            return "redirect:/login";
        }
        model.addAttribute("firstName", session.getAttribute("firstName"));
        return "renter-dashboard";
    }

    private Long currentOwnerId(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        return (Long) id;
    }
}