package com.ARVision.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private String sessionId;
}
