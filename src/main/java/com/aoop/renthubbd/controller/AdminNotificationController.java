package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.model.NotificationRecipient;
import com.aoop.renthubbd.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long adminId = currentAdminId(session);
        if (adminId == null) return "redirect:/admin/login";

        model.addAttribute("activeNav", "notifications");
        model.addAttribute("adminUsername", session.getAttribute("adminUsername"));
        model.addAttribute("adminRole", session.getAttribute("adminRole"));
        model.addAttribute("notifications", notificationService.listForAdmin(adminId));
        return "admin-notifications";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id,
                           @RequestParam(defaultValue = "list") String from,
                           HttpSession session,
                           RedirectAttributes ra) {
        Long adminId = currentAdminId(session);
        if (adminId == null) return "redirect:/admin/login";
        notificationService.markRead(id, adminId, NotificationRecipient.ADMIN);
        if ("dashboard".equals(from)) return "redirect:/admin/dashboard";
        return "redirect:/admin/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(HttpSession session, RedirectAttributes ra) {
        Long adminId = currentAdminId(session);
        if (adminId == null) return "redirect:/admin/login";
        notificationService.markAllReadForAdmin(adminId);
        ra.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/admin/notifications";
    }

    private Long currentAdminId(HttpSession session) {
        Object id = session.getAttribute("adminId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}