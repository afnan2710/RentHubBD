package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChatbotResponse {
    private String reply;
    private List<ChatbotListing> listings = new ArrayList<>();
    private boolean found;
    private boolean greeting;
    private boolean error;
    private String source;
}