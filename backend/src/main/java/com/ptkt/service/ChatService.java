package com.ptkt.service;

import com.ptkt.dto.WsEvent;
import com.ptkt.mapper.ChatMapper;
import com.ptkt.model.ChatMessage;
import com.ptkt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final RealtimeService realtimeService;

    public List<ChatMessage> getHistory(Long orgId, int limit) {
        List<ChatMessage> msgs = chatMapper.findRecentByOrgId(orgId, limit);
        // 최신순 → 오래된순 정렬
        java.util.Collections.reverse(msgs);
        return msgs;
    }

    public ChatMessage send(Long orgId, User actor, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setOrgId(orgId);
        msg.setUserId(actor.getId());
        msg.setUserName(actor.getName());
        msg.setContent(content);
        msg.setCreatedAt(java.time.LocalDateTime.now()); // insert 전에 세팅 → 브로드캐스트 시 null 방지
        chatMapper.insert(msg);

        realtimeService.broadcastToOrg(orgId, "chat",
            WsEvent.builder()
                .type("CHAT_MESSAGE")
                .payload(msg)
                .orgId(orgId)
                .actorId(actor.getId())
                .build()
        );
        return msg;
    }
}
