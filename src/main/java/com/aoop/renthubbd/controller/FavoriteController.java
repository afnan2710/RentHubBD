package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.service.FavoriteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/renter/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "favorites");
        model.addAttribute("favorites", favoriteService.listForUser(userId));
        return "renter-favorites";
    }

    @PostMapping("/{propertyId}/remove")
    public String remove(@PathVariable Long propertyId,
                         HttpSession session,
                         RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        favoriteService.remove(userId, propertyId);
        ra.addFlashAttribute("success", "Removed from favorites.");
        return "redirect:/renter/favorites";
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}