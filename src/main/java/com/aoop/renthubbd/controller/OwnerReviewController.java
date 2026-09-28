package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OwnerReviewController {

    private final ReviewService reviewService;

    public OwnerReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/owner/reviews")
    public String list(HttpSession session, Model model) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";

        var reviews = reviewService.getForOwner(ownerId);
        double average = 0.0;
        if (!reviews.isEmpty()) {
            int sum = 0;
            for (var r : reviews) sum += r.getRating();
            average = (double) sum / reviews.size();
        }

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "reviews");
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", average);
        model.addAttribute("totalReviews", reviews.size());
        return "owner-reviews";
    }

    private Long currentOwnerId(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        return (Long) id;
    }
}