package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.FriendRequestInfo;
import com.example.schedulemanager.model.FriendUser;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FriendshipMapper {

    @Insert("""
            INSERT INTO friendship (requester_user_id, addressee_user_id, status)
            VALUES (#{requesterUserId}, #{addresseeUserId}, #{status})
            """)
    int createRequest(
            @Param("requesterUserId") Long requesterUserId,
            @Param("addresseeUserId") Long addresseeUserId,
            @Param("status") String status);

    @Select("""
            SELECT COUNT(*)
            FROM friendship
            WHERE (requester_user_id = #{userA} AND addressee_user_id = #{userB})
               OR (requester_user_id = #{userB} AND addressee_user_id = #{userA})
            """)
    int countRelationship(@Param("userA") Long userA, @Param("userB") Long userB);

    @Select("""
            SELECT COUNT(*) > 0
            FROM friendship
            WHERE status = 'ACCEPTED'
              AND (
                (requester_user_id = #{userA} AND addressee_user_id = #{userB})
                OR (requester_user_id = #{userB} AND addressee_user_id = #{userA})
              )
            """)
    boolean existsAcceptedFriendship(@Param("userA") Long userA, @Param("userB") Long userB);

    @Select("""
            SELECT f.id, f.requester_user_id, u.username AS requester_username, u.display_name AS requester_display_name,
                   f.created_at
            FROM friendship f
            JOIN app_user u ON u.id = f.requester_user_id
            WHERE f.addressee_user_id = #{userId}
              AND f.status = 'PENDING'
            ORDER BY f.created_at DESC
            """)
    List<FriendRequestInfo> findIncomingPending(Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM friendship
            WHERE addressee_user_id = #{userId}
              AND status = 'PENDING'
            """)
    int countIncomingPending(@Param("userId") Long userId);

    @Select("""
            SELECT f.id, f.addressee_user_id AS requester_user_id, u.username AS requester_username,
                   u.display_name AS requester_display_name, f.created_at
            FROM friendship f
            JOIN app_user u ON u.id = f.addressee_user_id
            WHERE f.requester_user_id = #{userId}
              AND f.status = 'PENDING'
            ORDER BY f.created_at DESC
            """)
    List<FriendRequestInfo> findOutgoingPending(Long userId);

    @Update("""
            UPDATE friendship
            SET status = 'ACCEPTED',
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{requestId}
              AND addressee_user_id = #{userId}
              AND status = 'PENDING'
            """)
    int acceptRequest(@Param("requestId") Long requestId, @Param("userId") Long userId);

    @Select("""
            SELECT u.id, u.username, u.display_name
            FROM app_user u
            WHERE u.id IN (
                SELECT CASE
                           WHEN f.requester_user_id = #{userId} THEN f.addressee_user_id
                           ELSE f.requester_user_id
                       END
                FROM friendship f
                WHERE f.status = 'ACCEPTED'
                  AND (f.requester_user_id = #{userId} OR f.addressee_user_id = #{userId})
            )
            ORDER BY u.display_name, u.username
            """)
    List<FriendUser> findFriends(Long userId);
}
