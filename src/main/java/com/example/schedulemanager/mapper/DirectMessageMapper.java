package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.DirectMessage;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DirectMessageMapper {

    @Insert("""
            INSERT INTO direct_message (
                conversation_id, sender_user_id, recipient_user_id, body, related_schedule_item_id, is_read
            )
            VALUES (
                #{conversationId}, #{senderUserId}, #{recipientUserId}, #{body}, #{relatedScheduleItemId}, FALSE
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DirectMessage message);

    @Select("""
            SELECT dm.id, dm.conversation_id, dm.sender_user_id, dm.recipient_user_id,
                   su.username AS sender_username, su.display_name AS sender_display_name,
                   ru.username AS recipient_username, ru.display_name AS recipient_display_name,
                   dm.body, dm.related_schedule_item_id, dm.is_read AS read, dm.created_at
            FROM direct_message dm
            JOIN app_user su ON su.id = dm.sender_user_id
            JOIN app_user ru ON ru.id = dm.recipient_user_id
            WHERE dm.sender_user_id = #{userId}
               OR dm.recipient_user_id = #{userId}
            ORDER BY dm.created_at DESC, dm.id DESC
            LIMIT #{limit}
            """)
    List<DirectMessage> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("""
            SELECT dm.id, dm.conversation_id, dm.sender_user_id, dm.recipient_user_id,
                   su.username AS sender_username, su.display_name AS sender_display_name,
                   ru.username AS recipient_username, ru.display_name AS recipient_display_name,
                   dm.body, dm.related_schedule_item_id, dm.is_read AS read, dm.created_at
            FROM direct_message dm
            JOIN app_user su ON su.id = dm.sender_user_id
            JOIN app_user ru ON ru.id = dm.recipient_user_id
            WHERE dm.conversation_id = #{conversationId}
            ORDER BY dm.created_at ASC, dm.id ASC
            """)
    List<DirectMessage> findByConversationId(@Param("conversationId") Long conversationId);

    @Update("""
            UPDATE direct_message
            SET is_read = TRUE
            WHERE recipient_user_id = #{userId}
              AND is_read = FALSE
            """)
    int markAllReceivedAsRead(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM direct_message
            WHERE recipient_user_id = #{userId}
              AND is_read = FALSE
            """)
    int countUnreadByRecipient(@Param("userId") Long userId);
}
