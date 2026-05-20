package com.ptkt.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Schedule {
    private Long id;
    private Long userId;
    private Long orgId;
    private String title;
    private LocalDate eventDate;
    private String eventTime;
    private String source;
    private LocalDateTime createdAt;
}
