package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChatbotAiReply {
    private String reply;
    private List<Long> recommendedListingIds = new ArrayList<>();
    private String intent = "other";
}