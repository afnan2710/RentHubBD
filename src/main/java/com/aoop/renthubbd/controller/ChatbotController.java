package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.ChatbotRequest;
import com.aoop.renthubbd.dto.ChatbotResponse;
import com.aoop.renthubbd.service.ChatbotHistoryService;
import com.aoop.renthubbd.service.ChatbotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatbotHistoryService historyService;

    public ChatbotController(ChatbotService chatbotService,
                             ChatbotHistoryService historyService) {
        this.chatbotService = chatbotService;
        this.historyService = historyService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> message(@RequestBody ChatbotRequest request,
                                                   HttpSession session) {
        if (Boolean.TRUE.equals(request.getReset())) {
            historyService.reset(session);
        }
        return ResponseEntity.ok(chatbotService.handle(request.getMessage(), session));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(HttpSession session) {
        historyService.reset(session);
        return ResponseEntity.ok().build();
    }
}