package com.ARVision.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotBlank(message = "Message must not be blank")
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    /** Optional — frontend (crypto.randomUUID) generates one per session for context. */
    private String sessionId;
}