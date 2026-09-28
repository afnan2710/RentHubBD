package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/admins")
public class AdminManagementController {

    private final AdminService adminService;

    public AdminManagementController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String role,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       HttpSession session, Model model) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        if (actor.getRole() != AdminRole.SUPERADMIN) return "redirect:/admin/dashboard";

        List<Admin> admins = applyFilters(adminService.listAll(), role, status, keyword);

        model.addAttribute("activeNav", "admins");
        model.addAttribute("adminUsername", actor.getUsername());
        model.addAttribute("adminRole", actor.getRole().name());
        model.addAttribute("admins", admins);
        model.addAttribute("selfId", actor.getId());
        model.addAttribute("filterRole", role);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("roles", AdminRole.values());
        model.addAttribute("statuses", AccountStatus.values());
        return "admin-admins";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id,
                         HttpSession session,
                         RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        try {
            adminService.toggleStatus(id, actor);
            ra.addFlashAttribute("success", "Admin status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/admins";
    }

    private List<Admin> applyFilters(List<Admin> input, String role, String status, String keyword) {
        List<Admin> out = new ArrayList<>();
        String r = role != null && !role.isBlank() && !"ALL".equals(role) ? role : null;
        String s = status != null && !status.isBlank() && !"ALL".equals(status) ? status : null;
        String kw = keyword != null && !keyword.isBlank() ? keyword.toLowerCase() : null;

        for (Admin a : input) {
            if (r != null && (a.getRole() == null || !a.getRole().name().equals(r))) continue;
            if (s != null && (a.getStatus() == null || !a.getStatus().name().equals(s))) continue;
            if (kw != null && (a.getUsername() == null
                    || !a.getUsername().toLowerCase().contains(kw))) continue;
            out.add(a);
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