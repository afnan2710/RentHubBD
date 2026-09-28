package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.VisitStatus;
import com.aoop.renthubbd.service.ChatService;
import com.aoop.renthubbd.service.FavoriteService;
import com.aoop.renthubbd.service.VisitRequestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Controller
public class RenterDashboardController {

    private final VisitRequestService visitService;
    private final FavoriteService favoriteService;
    private final ChatService chatService;

    public RenterDashboardController(VisitRequestService visitService,
                                     FavoriteService favoriteService,
                                     ChatService chatService) {
        this.visitService = visitService;
        this.favoriteService = favoriteService;
        this.chatService = chatService;
    }

    @GetMapping("/renter/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        var visits = visitService.listForRenter(userId);
        var conversations = chatService.listForRenter(userId);

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("totalVisits", visits.size());
        model.addAttribute("acceptedVisits", visitService.summaryForRenter(userId)
                .getOrDefault(VisitStatus.ACCEPTED, 0L));
        model.addAttribute("pendingVisits", visitService.summaryForRenter(userId)
                .getOrDefault(VisitStatus.PENDING, 0L));
        model.addAttribute("favoriteCount", favoriteService.countForUser(userId));
        model.addAttribute("conversationCount", conversations.size());
        model.addAttribute("recentVisits", visits.size() > 5 ? visits.subList(0, 5) : visits);
        model.addAttribute("recentConversations",
                conversations.size() > 5 ? conversations.subList(0, 5) : conversations);
        model.addAttribute("bdDateTime", ZonedDateTime.now(ZoneId.of("Asia/Dhaka")));
        return "renter-dashboard";
    }

    private Long currentUserId(HttpSession session) {
        Object type = session.getAttribute("userType");
        Object id = session.getAttribute("userId");
        if (type == null || id == null || !"RENTER".equals(type.toString())) return null;
        return (Long) id;
    }
}