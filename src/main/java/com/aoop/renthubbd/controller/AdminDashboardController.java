package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.service.AdminDashboardService;
import com.aoop.renthubbd.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Controller
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AuditLogService auditLogService;

    public AdminDashboardController(AdminDashboardService dashboardService,
                                    AuditLogService auditLogService) {
        this.dashboardService = dashboardService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute("adminId") == null) return "redirect:/admin/login";

        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("adminUsername", session.getAttribute("adminUsername"));
        model.addAttribute("adminRole", session.getAttribute("adminRole"));
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("snapshot", dashboardService.getPlatformSnapshot());
        model.addAttribute("bdDateTime", ZonedDateTime.now(ZoneId.of("Asia/Dhaka")));
        model.addAttribute("recentAudit", auditLogService.readFiltered(defaultFilter()));
        return "admin-dashboard";
    }

    private com.aoop.renthubbd.dto.AuditFilter defaultFilter() {
        com.aoop.renthubbd.dto.AuditFilter f = new com.aoop.renthubbd.dto.AuditFilter();
        f.setLimit(8);
        return f;
    }
}