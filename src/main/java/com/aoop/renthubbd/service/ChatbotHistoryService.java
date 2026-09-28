package com.aoop.renthubbd.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotHistoryService {

    private static final String SESSION_KEY = "chatbotHistory";
    private static final int MAX_TURNS = 8;

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> history(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof List) return (List<Map<String, String>>) existing;
        List<Map<String, String>> fresh = new ArrayList<>();
        session.setAttribute(SESSION_KEY, fresh);
        return fresh;
    }

    public void addUser(HttpSession session, String message) {
        List<Map<String, String>> h = history(session);
        h.add(turn("user", message));
        trim(session, h);
    }

    public void addAssistant(HttpSession session, String message) {
        List<Map<String, String>> h = history(session);
        h.add(turn("assistant", message));
        trim(session, h);
    }

    public void reset(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    private Map<String, String> turn(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private void trim(HttpSession session, List<Map<String, String>> list) {
        while (list.size() > MAX_TURNS * 2) {
            list.remove(0);
        }
    }
}