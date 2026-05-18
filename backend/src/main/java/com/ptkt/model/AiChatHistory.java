package com.ptkt.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiChatHistory {
    private Long id;
    private Long userId;
    private String role; // "user" | "assistant"
    private String content;
    private LocalDateTime createdAt;
}
