package com.ptkt.mapper;

import com.ptkt.model.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    void insert(Notification n);
    List<Notification> findByUserId(@Param("userId") Long userId);
    int countUnread(@Param("userId") Long userId);
    void markRead(@Param("id") Long id, @Param("userId") Long userId);
    void markAllRead(@Param("userId") Long userId);
}
