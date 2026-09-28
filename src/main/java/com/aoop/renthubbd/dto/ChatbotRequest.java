package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotRequest {
    private String message;
    private Boolean reset;
}