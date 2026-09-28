package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.ProfileForm;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.model.UserType;
import com.aoop.renthubbd.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/owner/profile")
    public String ownerProfile(HttpSession session, Model model) {
        User user = requireOwner(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("firstName", user.getFirstName());
        model.addAttribute("activeNav", "profile");
        model.addAttribute("user", user);
        return "owner-profile";
    }

    @PostMapping("/owner/profile")
    public String updateOwnerProfile(@ModelAttribute ProfileForm form,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        return handleUpdate(session, form, ra, "owner");
    }

    @PostMapping("/owner/profile/password")
    public String changeOwnerPassword(@RequestParam String currentPassword,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        return handlePassword(session, currentPassword, newPassword, confirmPassword, ra, "owner");
    }

    @GetMapping("/renter/profile")
    public String renterProfile(HttpSession session, Model model) {
        User user = requireRenter(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("firstName", user.getFirstName());
        model.addAttribute("activeNav", "profile");
        model.addAttribute("user", user);
        return "renter-profile";
    }

    @PostMapping("/renter/profile")
    public String updateRenterProfile(@ModelAttribute ProfileForm form,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        return handleUpdate(session, form, ra, "renter");
    }

    @PostMapping("/renter/profile/password")
    public String changeRenterPassword(@RequestParam String currentPassword,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        return handlePassword(session, currentPassword, newPassword, confirmPassword, ra, "renter");
    }

    private String handleUpdate(HttpSession session, ProfileForm form,
                                RedirectAttributes ra, String base) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        String error = validate(form);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/" + base + "/profile";
        }

        try {
            User updated = profileService.updateProfile(userId, form);
            session.setAttribute("firstName", updated.getFirstName());
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Could not upload photo. Please try again.");
            return "redirect:/" + base + "/profile";
        }

        ra.addFlashAttribute("success", "Profile updated.");
        return "redirect:/" + base + "/profile";
    }

    private String handlePassword(HttpSession session,
                                  String currentPassword, String newPassword, String confirmPassword,
                                  RedirectAttributes ra, String base) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        try {
            profileService.changePassword(userId, currentPassword, newPassword, confirmPassword);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("passwordError", e.getMessage());
            return "redirect:/" + base + "/profile";
        }

        ra.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        return "redirect:/" + base + "/profile";
    }

    private User requireOwner(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        try {
            User u = profileService.getById((Long) id);
            return u.getUserType() == UserType.OWNER ? u : null;
        } catch (Exception e) {
            return null;
        }
    }

    private User requireRenter(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"RENTER".equals(type.toString())) return null;
        try {
            User u = profileService.getById((Long) id);
            return u.getUserType() == UserType.RENTER ? u : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String validate(ProfileForm f) {
        if (f.getFirstName() == null || f.getFirstName().isBlank()) return "First name is required.";
        if (f.getLastName()  == null || f.getLastName().isBlank())  return "Last name is required.";
        if (f.getPhoneNumber() == null || f.getPhoneNumber().isBlank()) return "Phone number is required.";
        if (f.getAddress()   == null || f.getAddress().isBlank())   return "Address is required.";
        return null;
    }
}