package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.NotificationEvent;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationEventMapper {

    @Insert("""
            INSERT INTO notification_event (recipient_user_id, actor_user_id, event_type, title, body)
            VALUES (#{recipientUserId}, #{actorUserId}, #{eventType}, #{title}, #{body})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotificationEvent event);

    @Select("""
            SELECT id, recipient_user_id, actor_user_id, event_type, title, body, created_at
            FROM notification_event
            WHERE recipient_user_id = #{recipientUserId}
              AND id > #{sinceId}
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<NotificationEvent> findSince(
            @Param("recipientUserId") Long recipientUserId,
            @Param("sinceId") Long sinceId,
            @Param("limit") int limit);
}
