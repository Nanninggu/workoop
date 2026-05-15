package com.ptkt.service;

import com.ptkt.dto.WsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final SimpMessagingTemplate messaging;

    /** 조직 전체 구독자에게 브로드캐스트 */
    public void broadcastToOrg(Long orgId, String channel, WsEvent event) {
        if (orgId == null) return;
        messaging.convertAndSend("/topic/org/" + orgId + "/" + channel, event);
    }

    /** 특정 사용자에게 개인 알림 전송 */
    public void sendToUser(Long userId, WsEvent event) {
        if (userId == null) return;
        messaging.convertAndSendToUser(userId.toString(), "/queue/notifications", event);
    }
}
