package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {
    private Long id;
    private Long orgId;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;

    // JOIN
    private String ownerName;
    private int taskCount;
}
