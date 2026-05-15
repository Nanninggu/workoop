package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.User;
import com.ptkt.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "") String orgName,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ApiResponse.error("질문을 입력하세요.");
        }

        String answer = aiService.ask(orgId, orgName, question);
        return ApiResponse.ok(Map.of("answer", answer));
    }
}
