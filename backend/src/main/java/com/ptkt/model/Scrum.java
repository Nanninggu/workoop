package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Scrum {
    private Long id;
    private Long userId;
    private Long orgId;
    private LocalDate scrumDate;
    private String tasksJson;       // JSON 배열
    private String blocker;
    private String blockerSeverity; // LOW, MEDIUM, HIGH
    private Integer energy;         // 0~5
    private String focus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN fields
    private String userName;
}
