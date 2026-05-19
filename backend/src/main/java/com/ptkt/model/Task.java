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

    private String assigneeIds;   // JSON 배열 "[1,3,7]" — 다수 담당자

    // JOIN fields
    private String assigneeName;  // 첫 번째 담당자 이름 (하위 호환)
    private String assigneeNames; // 쉼표 구분 전체 담당자 이름
    private String kpiName;
    private String kpiUnit;
    private String kpiType;
}
