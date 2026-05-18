package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.AiChatHistory;
import com.ptkt.model.ChatMessage;
import com.ptkt.model.User;
import com.ptkt.service.AiChatService;
import com.ptkt.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AiChatService aiChatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getHistory(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getHistory(orgId, limit)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessage>> send(
            @RequestParam Long orgId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("메시지를 입력하세요."));
        }
        return ResponseEntity.ok(ApiResponse.ok(chatService.send(orgId, user, content)));
    }

    @GetMapping("/ai/history")
    public ResponseEntity<ApiResponse<List<AiChatHistory>>> getAiHistory(
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(aiChatService.getHistory(user.getId(), limit)));
    }

    @PostMapping("/ai")
    public ResponseEntity<ApiResponse<Map<String, String>>> askAi(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String question = body.getOrDefault("question", "").trim();
        if (question.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("질문을 입력하세요."));
        }
        String answer = aiChatService.ask(user.getId(), question);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("answer", answer)));
    }
}
