package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.ProfileForm;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.service.ListingService;
import com.aoop.renthubbd.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/owner/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ListingService listingService;

    public ProfileController(ProfileService profileService, ListingService listingService) {
        this.profileService = profileService;
        this.listingService = listingService;
    }

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        User user = profileService.getById(ownerId);
        model.addAttribute("firstName", user.getFirstName());
        model.addAttribute("activeNav", "profile");
        model.addAttribute("user", user);
        model.addAttribute("stats", listingService.getDashboardStats(ownerId));
        return "owner-profile";
    }

    @PostMapping
    public String updateProfile(
            @ModelAttribute ProfileForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        String error = validate(form);
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/owner/profile";
        }

        try {
            User updated = profileService.updateProfile(ownerId, form);
            // Refresh session firstName so the sidebar / dashboard greeting stays in sync
            session.setAttribute("firstName", updated.getFirstName());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Could not upload photo. Please try again.");
            return "redirect:/owner/profile";
        }

        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/owner/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = requireOwner(session);
        if (ownerId == null) return "redirect:/login";

        try {
            profileService.changePassword(ownerId, currentPassword, newPassword, confirmPassword);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
            return "redirect:/owner/profile";
        }

        redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        return "redirect:/owner/profile";
    }

    private String validate(ProfileForm f) {
        if (f.getFirstName() == null || f.getFirstName().isBlank()) return "First name is required.";
        if (f.getLastName()  == null || f.getLastName().isBlank())  return "Last name is required.";
        if (f.getPhoneNumber() == null || f.getPhoneNumber().isBlank()) return "Phone number is required.";
        if (f.getAddress()   == null || f.getAddress().isBlank())   return "Address is required.";
        return null;
    }

    private Long requireOwner(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        return (Long) id;
    }
}