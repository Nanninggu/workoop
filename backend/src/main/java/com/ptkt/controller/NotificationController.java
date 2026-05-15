package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.Notification;
import com.ptkt.model.User;
import com.ptkt.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.findByUser(user.getId())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", notifService.countUnread(user.getId()))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        notifService.markRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("읽음 처리되었습니다.", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal User user) {
        notifService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("모두 읽음 처리되었습니다.", null));
    }
}
