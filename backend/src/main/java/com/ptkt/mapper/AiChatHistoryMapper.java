package com.ptkt.mapper;

import com.ptkt.model.AiChatHistory;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface AiChatHistoryMapper {
    List<AiChatHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    void insert(AiChatHistory history);
}
