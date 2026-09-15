package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.NotificationRecipient;
import com.aoop.renthubbd.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserNotificationController {

    private final NotificationService notificationService;

    public UserNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/owner/notifications")
    public String ownerList(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        model.addAttribute("activeNav", "notifications");
        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("notifications", notificationService.listForUser(userId));
        model.addAttribute("rolePath", "/owner");
        return "user-notifications";
    }

    @GetMapping("/renter/notifications")
    public String renterList(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("notifications", notificationService.listForUser(userId));
        model.addAttribute("rolePath", "/renter");
        return "user-notifications";
    }

    @PostMapping("/owner/notifications/{id}/read")
    public String ownerMarkRead(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        notificationService.markRead(id, userId, NotificationRecipient.USER);
        return "redirect:/owner/notifications";
    }

    @PostMapping("/renter/notifications/{id}/read")
    public String renterMarkRead(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        notificationService.markRead(id, userId, NotificationRecipient.USER);
        return "redirect:/renter/notifications";
    }

    @PostMapping("/owner/notifications/read-all")
    public String ownerMarkAllRead(HttpSession session, RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        notificationService.markAllReadForUser(userId);
        ra.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/owner/notifications";
    }

    @PostMapping("/renter/notifications/read-all")
    public String renterMarkAllRead(HttpSession session, RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        notificationService.markAllReadForUser(userId);
        ra.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/renter/notifications";
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}