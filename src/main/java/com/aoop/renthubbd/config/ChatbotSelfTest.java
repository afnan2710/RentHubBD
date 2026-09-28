package com.aoop.renthubbd.config;

import com.aoop.renthubbd.service.ChatbotLlmClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ChatbotSelfTest {

    private final ChatbotLlmClient llmClient;

    public ChatbotSelfTest(ChatbotLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        System.out.println("[ChatbotSelfTest] === Chatbot LLM self-test starting ===");
        if (!llmClient.isEnabled()) {
            System.out.println("[ChatbotSelfTest] Chatbot is DISABLED in application.properties.");
            return;
        }

        List<Map<String, String>> probe = List.of(
                Map.of("role", "system", "content",
                        "You are a test. Reply with exactly the JSON: {\"reply\":\"pong\",\"recommendedListingIds\":[],\"intent\":\"other\"}"),
                Map.of("role", "user", "content", "ping")
        );

        long start = System.currentTimeMillis();
        String result = llmClient.complete(probe);
        long elapsed = System.currentTimeMillis() - start;

        if (result == null) {
            System.out.println("[ChatbotSelfTest] FAILED after " + elapsed + "ms. "
                    + "Check the ChatbotLlmClient logs above for the HTTP status or exception.");
        } else {
            System.out.println("[ChatbotSelfTest] SUCCESS after " + elapsed + "ms. Response: " + result);
        }
        System.out.println("[ChatbotSelfTest] === Chatbot LLM self-test complete ===");
    }
}