package com.aicyber.jgmoli.ai.controller;

import com.aicyber.jgmoli.ai.dto.ChatRequest;
import com.aicyber.jgmoli.ai.dto.ChatResponse;
import com.aicyber.jgmoli.ai.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.answer(request == null ? null : request.message(), request == null ? null : request.history());
    }
}
