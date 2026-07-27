package com.ARVision.controller;

import com.ARVision.dto.chatbot.ChatMessageRequest;
import com.ARVision.dto.chatbot.ChatMessageResponse;
import com.ARVision.service.ChatBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    /**
     * Public endpoint — no auth required (the widget must work for guests).
     * <p>
     * NOTE: response is returned as the raw {@link ChatMessageResponse} (not
     * wrapped in {@code ApiResponse}) because the frontend reads {@code data.reply}
     * directly off this shape.
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> message(
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatBotService.reply(request));
    }
}