package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardPostInterest;
import com.example.schedulemanager.model.DirectMessage;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminModerationMapper {

    @Select("""
            SELECT id, username, password_hash, email, total_points, display_name, profile_bio, x_url, stream_url,
                   profile_image_url, profile_icon_color, profile_image_data, profile_image_content_type, enabled, created_at
            FROM app_user
            ORDER BY enabled DESC, created_at DESC, id DESC
            LIMIT 500
            """)
    List<AppUser> findUsers();

    @Select("""
            <script>
            SELECT bp.id, bp.thread_id, bt.game_title AS game_title, bp.author_user_id,
                   u.username AS author_username, u.display_name AS author_display_name,
                   bp.body, bp.schedule_date, bp.start_time, bp.rank_band, bp.recruitment_limit, bp.created_at
            FROM board_post bp
            JOIN board_thread bt ON bt.id = bp.thread_id
            JOIN app_user u ON u.id = bp.author_user_id
            <where>
              <if test='targetUserId != null'>bp.author_user_id = #{targetUserId}</if>
              <if test='keyword != null and keyword != ""'>
                <if test='targetUserId != null'> AND </if>
                (bp.body LIKE CONCAT('%', #{keyword}, '%') OR bt.game_title LIKE CONCAT('%', #{keyword}, '%')
                 OR u.username LIKE CONCAT('%', #{keyword}, '%') OR u.display_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY bp.created_at DESC, bp.id DESC
            LIMIT 200
            </script>
            """)
    List<BoardPost> findBoardPosts(@Param("keyword") String keyword, @Param("targetUserId") Long targetUserId);

    @Select("""
            <script>
            SELECT bpi.id, bpi.post_id, bpi.requester_user_id, u.username AS requester_username,
                   u.display_name AS requester_display_name, bpi.comment, bpi.created_at
            FROM board_post_interest bpi
            JOIN app_user u ON u.id = bpi.requester_user_id
            <where>
              <if test='targetUserId != null'>bpi.requester_user_id = #{targetUserId}</if>
              <if test='keyword != null and keyword != ""'>
                <if test='targetUserId != null'> AND </if>
                (bpi.comment LIKE CONCAT('%', #{keyword}, '%')
                 OR u.username LIKE CONCAT('%', #{keyword}, '%') OR u.display_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY bpi.created_at DESC, bpi.id DESC
            LIMIT 200
            </script>
            """)
    List<BoardPostInterest> findBoardInterests(@Param("keyword") String keyword, @Param("targetUserId") Long targetUserId);

    @Select("""
            <script>
            SELECT dm.id, dm.conversation_id, dm.sender_user_id, dm.recipient_user_id,
                   su.username AS sender_username, su.display_name AS sender_display_name,
                   ru.username AS recipient_username, ru.display_name AS recipient_display_name,
                   dm.body, dm.related_schedule_item_id, dm.is_read AS read, dm.created_at
            FROM direct_message dm
            JOIN app_user su ON su.id = dm.sender_user_id
            JOIN app_user ru ON ru.id = dm.recipient_user_id
            <where>
              <if test='targetUserId != null'>(dm.sender_user_id = #{targetUserId} OR dm.recipient_user_id = #{targetUserId})</if>
              <if test='keyword != null and keyword != ""'>
                <if test='targetUserId != null'> AND </if>
                (dm.body LIKE CONCAT('%', #{keyword}, '%')
                 OR su.username LIKE CONCAT('%', #{keyword}, '%') OR su.display_name LIKE CONCAT('%', #{keyword}, '%')
                 OR ru.username LIKE CONCAT('%', #{keyword}, '%') OR ru.display_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY dm.created_at DESC, dm.id DESC
            LIMIT 200
            </script>
            """)
    List<DirectMessage> findDirectMessages(@Param("keyword") String keyword, @Param("targetUserId") Long targetUserId);

    @Delete("DELETE FROM board_post_interest WHERE id = #{id}")
    int deleteBoardInterest(@Param("id") Long id);

    @Delete("DELETE FROM board_post_interest WHERE post_id = #{postId}")
    int deleteBoardInterestsByPostId(@Param("postId") Long postId);

    @Delete("DELETE FROM board_post WHERE id = #{id}")
    int deleteBoardPost(@Param("id") Long id);

    @Delete("DELETE FROM direct_message WHERE id = #{id}")
    int deleteDirectMessage(@Param("id") Long id);
}
