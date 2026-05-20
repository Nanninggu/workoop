package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.User;
import com.ptkt.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/slack")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncSlack(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) Map<String, Object> body) {

        Long orgId = (body != null && body.get("orgId") != null)
                ? Long.valueOf(body.get("orgId").toString()) : null;

        SyncService.SyncResult result = syncService.syncSlack(user.getId(), orgId);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("success", result.success());
        data.put("message", result.message());
        data.put("events",  result.events());

        return ResponseEntity.ok(ApiResponse.ok(result.message(), data));
    }
}
