package com.ptkt.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long orgId;
    private Long userId;
    private String userName;
    private String content;
    private LocalDateTime createdAt;
}
