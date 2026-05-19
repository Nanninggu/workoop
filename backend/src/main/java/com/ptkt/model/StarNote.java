package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StarNote {
    private Long id;
    private Long userId;
    private String title;
    private String lpTag;
    private Long kpiId;
    private String situation;
    private String task;
    private String action;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
