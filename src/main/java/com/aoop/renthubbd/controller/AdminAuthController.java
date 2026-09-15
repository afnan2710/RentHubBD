package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.AdminRegisterForm;
import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.model.AuditAction;
import com.aoop.renthubbd.service.AdminAuthService;
import com.aoop.renthubbd.service.AdminService;
import com.aoop.renthubbd.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private final AdminAuthService authService;
    private final AdminService adminService;
    private final AuditLogService auditLogService;

    public AdminAuthController(AdminAuthService authService,
                               AdminService adminService,
                               AuditLogService auditLogService) {
        this.authService = authService;
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        if (session.getAttribute("adminId") != null) return "redirect:/admin/dashboard";
        return "admin-login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          RedirectAttributes ra) {
        Admin a = authService.authenticate(username, password);
        if (a == null) {
            ra.addFlashAttribute("error", "Invalid credentials or inactive account.");
            return "redirect:/admin/login";
        }
        session.setAttribute("adminId", a.getId());
        session.setAttribute("adminUsername", a.getUsername());
        session.setAttribute("adminRole", a.getRole().name());
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Object u = session.getAttribute("adminUsername");
        if (u != null) auditLogService.record(u.toString(), AuditAction.LOGOUT, "session ended");
        session.invalidate();
        return "redirect:/admin/login";
    }

    @GetMapping("/admins/register")
    public String showRegisterForm(HttpSession session, Model model) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        if (actor.getRole() != AdminRole.SUPERADMIN) return "redirect:/admin/dashboard";

        if (!model.containsAttribute("form")) model.addAttribute("form", new AdminRegisterForm());
        model.addAttribute("activeNav", "register-admin");
        model.addAttribute("adminUsername", actor.getUsername());
        model.addAttribute("adminRole", actor.getRole().name());
        model.addAttribute("sharedEmail", Admin.SHARED_EMAIL);
        return "admin-register";
    }

    @PostMapping("/admins/register")
    public String registerAdmin(@ModelAttribute("form") AdminRegisterForm form,
                                HttpSession session,
                                RedirectAttributes ra) {
        Admin actor = currentAdmin(session);
        if (actor == null) return "redirect:/admin/login";
        if (actor.getRole() != AdminRole.SUPERADMIN) return "redirect:/admin/dashboard";
        try {
            adminService.register(form, actor);
            ra.addFlashAttribute("success", "Admin account created. Welcome email queued.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
        }
        return "redirect:/admin/admins";
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