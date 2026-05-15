package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private Long assigneeId;
    private String status;       // BACKLOG, TODO, IN_PROGRESS, REVIEW, DONE
    private String priority;     // P1, P2, P3
    private Long kpiId;
    private BigDecimal kpiContribution;
    private LocalDate dueDate;
    private BigDecimal estimatedHours;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN fields
    private String assigneeName;
    private String kpiName;
    private String kpiUnit;
    private String kpiType;
}
