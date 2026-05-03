package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.DmConversation;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DmConversationMapper {

    @Select("""
            SELECT id, user_a_id, user_b_id, created_at
            FROM dm_conversation
            WHERE (user_a_id = #{userA} AND user_b_id = #{userB})
               OR (user_a_id = #{userB} AND user_b_id = #{userA})
            """)
    DmConversation findPair(@Param("userA") Long userA, @Param("userB") Long userB);

    @Insert("""
            INSERT INTO dm_conversation (user_a_id, user_b_id)
            VALUES (#{userAId}, #{userBId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DmConversation conversation);

    @Select("""
            SELECT c.id, c.user_a_id, c.user_b_id, c.created_at,
                   p.username AS partner_username,
                   p.display_name AS partner_display_name,
                   m.body AS last_message_body,
                   m.created_at AS last_message_at
            FROM dm_conversation c
            JOIN app_user p ON p.id = CASE WHEN c.user_a_id = #{userId} THEN c.user_b_id ELSE c.user_a_id END
            LEFT JOIN direct_message m ON m.id = (
                SELECT dm2.id
                FROM direct_message dm2
                WHERE dm2.conversation_id = c.id
                ORDER BY dm2.created_at DESC, dm2.id DESC
                FETCH FIRST 1 ROW ONLY
            )
            WHERE c.user_a_id = #{userId}
               OR c.user_b_id = #{userId}
            ORDER BY COALESCE(m.created_at, c.created_at) DESC, c.id DESC
            """)
    List<DmConversation> findByUser(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_a_id, user_b_id, created_at
            FROM dm_conversation
            WHERE id = #{id}
            """)
    DmConversation findById(@Param("id") Long id);
}
