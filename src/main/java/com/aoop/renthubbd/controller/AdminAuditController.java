package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.AuditEntry;
import com.aoop.renthubbd.dto.AuditFilter;
import com.aoop.renthubbd.model.AuditAction;
import com.aoop.renthubbd.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Controller
public class AdminAuditController {

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/admin/audit-log")
    public String view(@RequestParam(required = false) String admin,
                       @RequestParam(required = false) AuditAction action,
                       @RequestParam(required = false) String keyword,
                       HttpSession session,
                       Model model) {
        if (session.getAttribute("adminId") == null) return "redirect:/admin/login";

        AuditFilter filter = new AuditFilter();
        filter.setAdmin(admin);
        filter.setAction(action);
        filter.setKeyword(keyword);
        filter.setLimit(300);

        List<AuditEntry> entries = auditLogService.readFiltered(filter);

        Map<String, Long> byActor = auditLogService.countByActor();
        Map<AuditAction, Long> byAction = auditLogService.countByAction();

        model.addAttribute("activeNav", "audit");
        model.addAttribute("adminUsername", session.getAttribute("adminUsername"));
        model.addAttribute("adminRole", session.getAttribute("adminRole"));
        model.addAttribute("entries", entries);
        model.addAttribute("actors", auditLogService.distinctActors());
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("byActor", byActor);
        model.addAttribute("byAction", byAction);
        model.addAttribute("countToday", auditLogService.countToday());
        model.addAttribute("countLastHour", auditLogService.countLastHour());
        model.addAttribute("totalReceived", auditLogService.getReceivedCount());
        model.addAttribute("totalWritten", auditLogService.getWriteCount());
        model.addAttribute("buffered", auditLogService.getBufferedCount());
        model.addAttribute("filterAdmin", admin);
        model.addAttribute("filterAction", action);
        model.addAttribute("filterKeyword", keyword);
        return "admin-audit-log";
    }

    @GetMapping("/admin/audit-log/export")
    public ResponseEntity<Resource> export(HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/admin/login")
                    .build();
        }
        Path file = auditLogService.getLogFile();
        Resource r = new FileSystemResource(file.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activity-log.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(r);
    }
}