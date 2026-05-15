package com.ptkt.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kpi {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String unit;
    private String kpiType;     // NUMERIC, PERCENTAGE, BOOLEAN
    private BigDecimal targetValue;
    private String frequency;   // DAILY, WEEKLY, MONTHLY
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;      // ACTIVE, PAUSED, COMPLETED
    private Integer sortOrder;
    private Long ownerId;
    private Long orgId;
    private String scope;  // PERSONAL, TEAM
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN field
    private Category category;
}
