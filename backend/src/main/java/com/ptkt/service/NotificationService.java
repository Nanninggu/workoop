package com.ptkt.service;

import com.ptkt.dto.WsEvent;
import com.ptkt.mapper.NotificationMapper;
import com.ptkt.model.Notification;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationMapper notifMapper;
    private final RealtimeService realtimeService;

    // @Lazy: RealtimeService → SimpMessagingTemplate이 WebSocket 빈 초기화 이후 주입
    public NotificationService(NotificationMapper notifMapper,
                                @Lazy RealtimeService realtimeService) {
        this.notifMapper      = notifMapper;
        this.realtimeService  = realtimeService;
    }

    public List<Notification> findByUser(Long userId) {
        return notifMapper.findByUserId(userId);
    }

    public int countUnread(Long userId) {
        return notifMapper.countUnread(userId);
    }

    @Transactional
    public void send(Long userId, String type, String title, String body, String link) {
        Notification notif = Notification.builder()
                .userId(userId).type(type).title(title).body(body).link(link)
                .build();
        notifMapper.insert(notif);

        // WebSocket으로 즉시 푸시
        realtimeService.sendToUser(userId, WsEvent.builder()
                .type("NOTIFICATION")
                .payload(notif)
                .build());
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notifMapper.markRead(id, userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifMapper.markAllRead(userId);
    }
}
