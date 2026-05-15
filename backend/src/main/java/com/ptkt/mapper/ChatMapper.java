package com.ptkt.mapper;

import com.ptkt.model.ChatMessage;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ChatMapper {
    List<ChatMessage> findRecentByOrgId(@Param("orgId") Long orgId, @Param("limit") int limit);
    void insert(ChatMessage msg);
}
