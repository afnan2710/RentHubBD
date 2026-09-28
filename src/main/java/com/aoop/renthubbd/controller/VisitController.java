package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.VisitRequestForm;
import com.aoop.renthubbd.service.VisitRequestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class VisitController {

    private final VisitRequestService visitService;

    public VisitController(VisitRequestService visitService) {
        this.visitService = visitService;
    }

    @PostMapping("/listing/{id}/request-visit")
    public String create(@PathVariable Long id,
                         @ModelAttribute VisitRequestForm form,
                         HttpSession session,
                         RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            visitService.create(id, userId, form);
            ra.addFlashAttribute("success",
                    "Visit request sent. The owner will respond shortly.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/listing/" + id;
    }

    @GetMapping("/renter/visits")
    public String renterList(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "visits");
        model.addAttribute("visits", visitService.listForRenter(userId));
        model.addAttribute("summary", visitService.summaryForRenter(userId));
        return "renter-visits";
    }

    @PostMapping("/renter/visits/{id}/confirm")
    public String confirm(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            visitService.confirmReschedule(id, userId);
            ra.addFlashAttribute("success", "You confirmed the rescheduled visit.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/renter/visits";
    }

    @PostMapping("/renter/visits/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            visitService.cancel(id, userId);
            ra.addFlashAttribute("success", "Visit request cancelled.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/renter/visits";
    }

    @GetMapping("/owner/visit-requests")
    public String ownerList(@RequestParam(required = false) String status,
                            HttpSession session, Model model) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";

        var all = visitService.listForOwner(ownerId);
        var filtered = all;
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            filtered = all.stream()
                    .filter(v -> v.getStatus().name().equals(status))
                    .toList();
        }

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "visits");
        model.addAttribute("visits", filtered);
        model.addAttribute("summary", visitService.summaryForOwner(ownerId));
        model.addAttribute("filterStatus", status);
        model.addAttribute("statuses", com.aoop.renthubbd.model.VisitStatus.values());
        return "owner-visits";
    }

    @PostMapping("/owner/visit-requests/{id}/accept")
    public String accept(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         HttpSession session,
                         RedirectAttributes ra) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";
        try {
            visitService.accept(id, ownerId, note);
            ra.addFlashAttribute("success", "Visit accepted. The renter has been notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/visit-requests";
    }

    @PostMapping("/owner/visit-requests/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         HttpSession session,
                         RedirectAttributes ra) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";
        try {
            visitService.reject(id, ownerId, note);
            ra.addFlashAttribute("success", "Visit declined. The renter has been notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/visit-requests";
    }

    @PostMapping("/owner/visit-requests/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             LocalDate proposedDate,
                             @RequestParam String proposedTime,
                             @RequestParam(required = false) String note,
                             HttpSession session,
                             RedirectAttributes ra) {
        Long ownerId = currentOwnerId(session);
        if (ownerId == null) return "redirect:/login";
        try {
            visitService.reschedule(id, ownerId, proposedDate, proposedTime, note);
            ra.addFlashAttribute("success", "New time proposed. Waiting for renter confirmation.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/visit-requests";
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }

    private Long currentOwnerId(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"OWNER".equals(type.toString())) return null;
        return (Long) id;
    }
}