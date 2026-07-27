package com.ARVision.dto.chatbot;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId; // optional, for future conversation history
}
