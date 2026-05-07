package com.example.schedulemanager.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendNotificationPreferenceMapper {

    @Insert("""
            INSERT INTO friend_notification_preference (user_id, friend_user_id, enabled)
            VALUES (#{userId}, #{friendUserId}, TRUE)
            """)
    int insertEnabled(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    @Delete("""
            DELETE FROM friend_notification_preference
            WHERE user_id = #{userId}
              AND friend_user_id = #{friendUserId}
            """)
    int deletePreference(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    @Select("""
            SELECT friend_user_id
            FROM friend_notification_preference
            WHERE user_id = #{userId}
              AND enabled = TRUE
            """)
    List<Long> findEnabledFriendUserIds(@Param("userId") Long userId);

    @Select("""
            SELECT p.user_id
            FROM friend_notification_preference p
            WHERE p.friend_user_id = #{actorUserId}
              AND p.enabled = TRUE
              AND EXISTS (
                  SELECT 1
                  FROM friendship f
                  WHERE f.status = 'ACCEPTED'
                    AND (
                        (f.requester_user_id = p.user_id AND f.addressee_user_id = p.friend_user_id)
                        OR (f.requester_user_id = p.friend_user_id AND f.addressee_user_id = p.user_id)
                    )
              )
            """)
    List<Long> findRecipientUserIdsByActor(@Param("actorUserId") Long actorUserId);
}
