package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.dto.MessagePayload;
import com.aoop.renthubbd.model.Message;
import com.aoop.renthubbd.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;

    public ChatApiController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessagePayload>> poll(@PathVariable Long id,
                                                     @RequestParam(required = false) Long afterId,
                                                     HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            chatService.getForUser(id, userId);
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }

        chatService.markRead(id, userId);
        List<Message> messages = chatService.messagesAfter(id, afterId);
        return ResponseEntity.ok(chatService.toPayloads(messages, userId));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<MessagePayload> send(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            chatService.getForUser(id, userId);
            Message saved = chatService.send(id, userId, body.get("content"));
            List<MessagePayload> payloads = chatService.toPayloads(List.of(saved), userId);
            return ResponseEntity.ok(payloads.get(0));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) return null;
        return (Long) id;
    }
}