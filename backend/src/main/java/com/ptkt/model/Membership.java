package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {
    private Long id;
    private Long orgId;
    private Long userId;
    private String role;  // OWNER, ADMIN, MEMBER
    private LocalDateTime joinedAt;

    // JOIN fields
    private String userName;
    private String userEmail;
}
