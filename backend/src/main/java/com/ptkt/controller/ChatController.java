package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.ChatMessage;
import com.ptkt.model.User;
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
}
