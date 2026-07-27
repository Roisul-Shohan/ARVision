package com.ARVision.controller;

import com.ARVision.dto.chatbot.ChatRequest;
import com.ARVision.dto.chatbot.ChatResponse;
import com.ARVision.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // POST /api/chatbot/message
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> message(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String reply = chatbotService.chat(request.getMessage());
        return ResponseEntity.ok(new ChatResponse(reply, sessionId));
    }
}
