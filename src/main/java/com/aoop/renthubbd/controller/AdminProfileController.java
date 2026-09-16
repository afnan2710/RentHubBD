package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.AdminPasswordForm;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.repository.AdminRepository;
import com.aoop.renthubbd.service.AdminAuthService;
import com.aoop.renthubbd.service.AuditLogService;
import com.aoop.renthubbd.service.FileExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final AdminRepository adminRepository;
    private final AdminAuthService authService;
    private final AuditLogService auditLogService;
    private final FileExportService exportService;

    public AdminProfileController(AdminRepository adminRepository,
                                  AdminAuthService authService,
                                  AuditLogService auditLogService,
                                  FileExportService exportService) {
        this.adminRepository = adminRepository;
        this.authService = authService;
        this.auditLogService = auditLogService;
        this.exportService = exportService;
    }

    @GetMapping
    public String profile(HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) return "redirect:/admin/login";

        Admin a = adminRepository.findById(adminId).orElse(null);
        if (a == null) return "redirect:/admin/login";

        if (!model.containsAttribute("form")) model.addAttribute("form", new AdminPasswordForm());
        model.addAttribute("activeNav", "profile");
        model.addAttribute("adminUsername", a.getUsername());
        model.addAttribute("adminRole", a.getRole().name());
        model.addAttribute("admin", a);
        model.addAttribute("auditWrites", auditLogService.getWriteCount());
        model.addAttribute("auditBuffered", auditLogService.getBufferedCount());
        return "admin-profile";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute("form") AdminPasswordForm form,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) return "redirect:/admin/login";

        Admin a = adminRepository.findById(adminId).orElse(null);
        if (a == null) return "redirect:/admin/login";

        try {
            authService.changePassword(a, form.getCurrentPassword(),
                    form.getNewPassword(), form.getConfirmPassword());
            ra.addFlashAttribute("success", "Password changed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @GetMapping("/export/users")
    public ResponseEntity<Resource> exportUsers() throws Exception {
        Path file = exportService.exportUsersCsv().get();
        return download(file, "users.csv");
    }

    @GetMapping("/export/listings")
    public ResponseEntity<Resource> exportListings() throws Exception {
        Path file = exportService.exportListingsCsv().get();
        return download(file, "listings.csv");
    }

    @GetMapping("/export/admins")
    public ResponseEntity<Resource> exportAdmins() throws Exception {
        Path file = exportService.exportAdminsCsv().get();
        return download(file, "admins.csv");
    }

    private ResponseEntity<Resource> download(Path file, String name) {
        Resource r = new FileSystemResource(file.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(r);
    }
}