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
public class KpiRecord {
    private Long id;
    private Long kpiId;
    private BigDecimal actualValue;
    private Boolean booleanValue;
    private LocalDate recordedDate;
    private String note;
    private Long ownerId;
    private LocalDateTime createdAt;

    // JOIN field
    private Kpi kpi;
}
