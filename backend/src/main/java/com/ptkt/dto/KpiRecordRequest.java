package com.ptkt.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class KpiRecordRequest {
    private Long kpiId;
    private BigDecimal actualValue;
    private Boolean booleanValue;
    private LocalDate recordedDate;
    private String note;
}
