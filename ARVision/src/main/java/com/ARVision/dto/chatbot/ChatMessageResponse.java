package com.ARVision.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    /** The bot's reply text. Frontend reads `data.reply`. */
    private String reply;

    /** Echo of the session id — useful if frontend wants to confirm binding. */
    private String sessionId;

    /** Resolved intent key (e.g. "products", "shipping", "fallback"). */
    private String intent;
}