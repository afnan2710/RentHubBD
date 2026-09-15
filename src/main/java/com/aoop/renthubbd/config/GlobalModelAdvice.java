package com.aoop.renthubbd.config;

import com.aoop.renthubbd.model.UserType;
import com.aoop.renthubbd.repository.AdminRepository;
import com.aoop.renthubbd.repository.UserRepository;
import com.aoop.renthubbd.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public GlobalModelAdvice(NotificationService notificationService,
                             UserRepository userRepository,
                             AdminRepository adminRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        model.addAttribute("navLoggedIn", false);

        Object adminId = session.getAttribute("adminId");
        if (adminId instanceof Long) {
            Long id = (Long) adminId;
            model.addAttribute("adminUnreadCount", notificationService.countUnreadForAdmin(id));
            adminRepository.findById(id).ifPresent(a -> {
                model.addAttribute("navLoggedIn", true);
                model.addAttribute("navRole", "ADMIN");
                model.addAttribute("navDisplayName", a.getUsername());
                model.addAttribute("navProfilePhoto", null);
                model.addAttribute("navInitials",
                        a.getUsername().substring(0, Math.min(2, a.getUsername().length())).toUpperCase());
                model.addAttribute("navDashboardUrl", "/admin/dashboard");
                model.addAttribute("navLogoutUrl", "/admin/logout");
            });
            return;
        }

        Object userId = session.getAttribute("userId");
        if (userId instanceof Long) {
            Long id = (Long) userId;
            model.addAttribute("userUnreadCount", notificationService.countUnreadForUser(id));
            userRepository.findById(id).ifPresent(u -> {
                String first = u.getFirstName() == null ? "" : u.getFirstName();
                String last = u.getLastName() == null ? "" : u.getLastName();

                model.addAttribute("navLoggedIn", true);
                model.addAttribute("navRole", u.getUserType() != null ? u.getUserType().name() : "USER");
                model.addAttribute("navDisplayName", (first + " " + last).trim());
                model.addAttribute("navProfilePhoto", u.getProfilePhoto());
                model.addAttribute("navInitials",
                        ((first.isEmpty() ? "U" : first.substring(0, 1))
                                + (last.isEmpty() ? "" : last.substring(0, 1))).toUpperCase());
                model.addAttribute("navDashboardUrl",
                        u.getUserType() == UserType.OWNER ? "/owner/dashboard" : "/renter/dashboard");
                model.addAttribute("navLogoutUrl", "/logout");
            });
        }
    }
}