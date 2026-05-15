package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    private Long id;
    private Long userId;
    private String type;   // TASK_ASSIGNED, TASK_DONE, KPI_ACHIEVED, BLOCKER_HIGH
    private String title;
    private String body;
    private String link;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public boolean isUnread() { return readAt == null; }
}
