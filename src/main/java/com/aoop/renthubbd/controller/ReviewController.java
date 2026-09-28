package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.ReviewForm;
import com.aoop.renthubbd.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/listing/{listingId}/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public String post(@PathVariable Long listingId,
                       @ModelAttribute ReviewForm form,
                       HttpSession session,
                       RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            reviewService.postReview(listingId, userId, form);
            ra.addFlashAttribute("success", "Thank you — your review was posted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/listing/" + listingId;
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}