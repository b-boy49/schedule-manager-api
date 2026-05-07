package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardPostInterest;
import com.example.schedulemanager.model.BoardThread;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BoardMapper {

    @Insert("""
            INSERT INTO board_thread (owner_user_id, game_title)
            VALUES (#{ownerUserId}, #{gameTitle})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertThread(BoardThread thread);

    @Select("""
            <script>
            SELECT bt.id, bt.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   bt.game_title, bt.created_at, bt.updated_at,
                   COALESCE(COUNT(bp.id), 0) AS post_count,
                   COALESCE(MAX(bp.created_at), bt.created_at) AS latest_post_at
            FROM board_thread bt
            JOIN app_user u ON u.id = bt.owner_user_id
            LEFT JOIN board_post bp ON bp.thread_id = bt.id
            <where>
                <if test="keyword != null and keyword != ''">
                    bt.game_title LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            GROUP BY bt.id, bt.owner_user_id, u.username, u.display_name, bt.game_title, bt.created_at, bt.updated_at
            ORDER BY latest_post_at DESC, bt.id DESC
            </script>
            """)
    List<BoardThread> findAllThreads(@Param("keyword") String keyword);

    @Select("""
            SELECT id, owner_user_id, game_title, created_at, updated_at
            FROM board_thread
            WHERE id = #{threadId}
            """)
    BoardThread findThreadById(@Param("threadId") Long threadId);

    @Select("""
            SELECT bt.id, bt.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   bt.game_title, bt.created_at, bt.updated_at,
                   COALESCE(COUNT(bp.id), 0) AS post_count,
                   COALESCE(MAX(bp.created_at), bt.created_at) AS latest_post_at
            FROM board_thread bt
            JOIN app_user u ON u.id = bt.owner_user_id
            LEFT JOIN board_post bp ON bp.thread_id = bt.id
            WHERE bt.id = #{threadId}
            GROUP BY bt.id, bt.owner_user_id, u.username, u.display_name, bt.game_title, bt.created_at, bt.updated_at
            """)
    BoardThread findThreadViewById(@Param("threadId") Long threadId);

    @Insert("""
            INSERT INTO board_post (thread_id, author_user_id, body, schedule_date, start_time, rank_band, recruitment_limit)
            VALUES (#{threadId}, #{authorUserId}, #{body}, #{scheduleDate}, #{startTime}, #{rankBand}, #{recruitmentLimit})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPost(BoardPost post);

    @Select("""
            SELECT bp.id, bp.thread_id, bp.author_user_id, u.username AS author_username, u.display_name AS author_display_name,
                   bp.body, bp.schedule_date, bp.start_time, bp.rank_band, bp.recruitment_limit, bp.created_at
            FROM board_post bp
            JOIN app_user u ON u.id = bp.author_user_id
            WHERE bp.thread_id = #{threadId}
            ORDER BY bp.created_at DESC, bp.id DESC
            """)
    List<BoardPost> findPostsByThreadId(@Param("threadId") Long threadId);

    @Select("""
            SELECT bp.id, bp.thread_id, bp.author_user_id, u.username AS author_username, u.display_name AS author_display_name,
                   bp.body, bp.schedule_date, bp.start_time, bp.rank_band, bp.recruitment_limit, bp.created_at
            FROM board_post bp
            JOIN app_user u ON u.id = bp.author_user_id
            WHERE bp.id = #{postId}
            """)
    BoardPost findPostById(@Param("postId") Long postId);

    @Insert("""
            INSERT INTO board_post_interest (post_id, requester_user_id, comment)
            VALUES (#{postId}, #{requesterUserId}, #{comment})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPostInterest(BoardPostInterest interest);

    @Select("""
            SELECT bpi.id, bpi.post_id, bpi.requester_user_id, u.username AS requester_username,
                   u.display_name AS requester_display_name, bpi.comment, bpi.created_at
            FROM board_post_interest bpi
            JOIN app_user u ON u.id = bpi.requester_user_id
            WHERE bpi.post_id = #{postId}
            ORDER BY bpi.created_at DESC, bpi.id DESC
            """)
    List<BoardPostInterest> findInterestsByPostId(@Param("postId") Long postId);

    @Update("""
            UPDATE board_thread
            SET updated_at = CURRENT_TIMESTAMP
            WHERE id = #{threadId}
            """)
    int touchThreadUpdatedAt(@Param("threadId") Long threadId);
}
