package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserManagementService userService;

    public AdminUserController(UserManagementService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String role,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       HttpSession session, Model model) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";

        List<User> all = userService.listAll();
        List<User> users = applyFilters(all, role, status, keyword);

        Map<String, Long> byStatus = users.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getStatus() == null ? "UNKNOWN" : u.getStatus().name(),
                        Collectors.counting()));

        model.addAttribute("activeNav", "users");
        model.addAttribute("adminUsername", actor.getUsername());
        model.addAttribute("adminRole", actor.getRole().name());
        model.addAttribute("users", users);
        model.addAttribute("byStatus", byStatus);
        model.addAttribute("filterRole", role);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("userTypes", com.aoop.renthubbd.model.UserType.values());
        model.addAttribute("statuses", AccountStatus.values());
        return "admin-users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id,
                         HttpSession session,
                         RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        try {
            userService.toggleStatus(id, actor);
            ra.addFlashAttribute("success", "User status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private List<User> applyFilters(List<User> input, String role, String status, String keyword) {
        List<User> out = new ArrayList<>();
        String r = role != null && !role.isBlank() && !"ALL".equals(role) ? role : null;
        String s = status != null && !status.isBlank() && !"ALL".equals(status) ? status : null;
        String kw = keyword != null && !keyword.isBlank() ? keyword.toLowerCase() : null;

        for (User u : input) {
            if (r != null && (u.getUserType() == null || !u.getUserType().name().equals(r))) continue;
            if (s != null && (u.getStatus() == null || !u.getStatus().name().equals(s))) continue;
            if (kw != null) {
                String haystack = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                        + (u.getLastName() == null ? "" : u.getLastName()) + " "
                        + (u.getEmail() == null ? "" : u.getEmail()) + " "
                        + (u.getPhoneNumber() == null ? "" : u.getPhoneNumber())).toLowerCase();
                if (!haystack.contains(kw)) continue;
            }
            out.add(u);
        }
        return out;
    }

    private Admin currentAdmin(HttpSession session) {
        Object id = session.getAttribute("adminId");
        Object role = session.getAttribute("adminRole");
        if (id == null || role == null) return null;
        Admin a = new Admin();
        a.setId((Long) id);
        a.setUsername(session.getAttribute("adminUsername").toString());
        a.setRole(AdminRole.valueOf(role.toString()));
        a.setStatus(AccountStatus.ACTIVE);
        return a;
    }
}