package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.Conversation;
import com.aoop.renthubbd.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ChatPageController {

    private final ChatService chatService;

    public ChatPageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat/{id}")
    public String chat(@PathVariable Long id, HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        Conversation conv;
        try {
            conv = chatService.getForUser(id, userId);
        } catch (Exception e) {
            return "redirect:/";
        }

        chatService.markRead(id, userId);

        model.addAttribute("conversation", conv);
        model.addAttribute("messages", chatService.messages(id));
        model.addAttribute("currentUserId", userId);
        model.addAttribute("navBack",
                session.getAttribute("userType") != null
                        && "OWNER".equals(session.getAttribute("userType").toString())
                        ? "/owner/messages" : "/renter/messages");
        return "chat";
    }

    @GetMapping("/renter/messages")
    public String renterMessages(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        List<Conversation> conversations = chatService.listForRenter(userId);
        Map<Long, Long> unread = unreadMap(conversations, userId);

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "messages");
        model.addAttribute("conversations", conversations);
        model.addAttribute("unreadMap", unread);
        model.addAttribute("currentUserId", userId);
        return "renter-messages";
    }

    @GetMapping("/owner/messages")
    public String ownerMessages(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        List<Conversation> conversations = chatService.listForOwner(userId);
        Map<Long, Long> unread = unreadMap(conversations, userId);

        model.addAttribute("firstName", session.getAttribute("firstName"));
        model.addAttribute("activeNav", "messages");
        model.addAttribute("conversations", conversations);
        model.addAttribute("unreadMap", unread);
        model.addAttribute("currentUserId", userId);
        return "owner-messages";
    }

    private Map<Long, Long> unreadMap(List<Conversation> conversations, Long userId) {
        Map<Long, Long> map = new HashMap<>();
        for (Conversation c : conversations) {
            long count = chatService.unreadInConversation(c.getId(), userId);
            if (count > 0) map.put(c.getId(), count);
        }
        return map;
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}