package com.ptkt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsEvent {
    private String type;     // TASK_CREATED, TASK_UPDATED, TASK_STATUS_CHANGED, TASK_DELETED,
                             // SCRUM_UPDATED, PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED,
                             // NOTIFICATION
    private Object payload;
    private Long orgId;
    private Long actorId;
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}
