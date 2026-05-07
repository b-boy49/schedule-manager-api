package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.ScheduleItem;
import com.example.schedulemanager.model.FriendUser;
import com.example.schedulemanager.model.TaskCompletionRankingRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ScheduleMapper {

    @Select("""
            SELECT s.id, s.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.profile_icon_color AS owner_profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS owner_has_profile_image,
                   s.schedule_date, s.priority, s.device_type, s.rank_band, s.completed, s.completed_at,
                   s.message_shareable, s.source_schedule_item_id, s.source_owner_user_id,
                   su.display_name AS source_owner_display_name,
                   s.title, s.start_time, s.end_time, s.description, s.shared_with_friends, s.joinable,
                   s.recruitment_limit,
                   s.created_at, s.updated_at
            FROM schedule_item s
            LEFT JOIN app_user u ON u.id = s.owner_user_id
            LEFT JOIN app_user su ON su.id = s.source_owner_user_id
            WHERE s.schedule_date = #{date}
              AND (
                  s.owner_user_id = #{viewerUserId}
                  OR (
                      s.shared_with_friends = TRUE
                      AND EXISTS (
                          SELECT 1
                          FROM friendship f
                          WHERE f.status = 'ACCEPTED'
                            AND (
                                (f.requester_user_id = s.owner_user_id AND f.addressee_user_id = #{viewerUserId})
                                OR (f.requester_user_id = #{viewerUserId} AND f.addressee_user_id = s.owner_user_id)
                            )
                      )
                  )
              )
            ORDER BY COALESCE(s.start_time, TIME '23:59:59'), s.id
            """)
    List<ScheduleItem> findVisibleByDate(@Param("date") LocalDate date, @Param("viewerUserId") Long viewerUserId);

    @Select("""
            SELECT s.id, s.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.profile_icon_color AS owner_profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS owner_has_profile_image,
                   s.schedule_date, s.priority, s.device_type, s.rank_band, s.completed, s.completed_at,
                   s.message_shareable, s.source_schedule_item_id, s.source_owner_user_id,
                   su.display_name AS source_owner_display_name,
                   s.title, s.start_time, s.end_time, s.description, s.shared_with_friends, s.joinable,
                   s.recruitment_limit,
                   s.created_at, s.updated_at
            FROM schedule_item s
            LEFT JOIN app_user u ON u.id = s.owner_user_id
            LEFT JOIN app_user su ON su.id = s.source_owner_user_id
            WHERE s.owner_user_id = #{ownerUserId}
              AND s.schedule_date = #{date}
            ORDER BY COALESCE(s.start_time, TIME '23:59:59'), s.id
            """)
    List<ScheduleItem> findOwnedByDate(@Param("date") LocalDate date, @Param("ownerUserId") Long ownerUserId);

    @Select("""
            SELECT s.id, s.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.profile_icon_color AS owner_profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS owner_has_profile_image,
                   s.schedule_date, s.priority, s.device_type, s.rank_band, s.completed, s.completed_at,
                   s.message_shareable, s.source_schedule_item_id, s.source_owner_user_id,
                   su.display_name AS source_owner_display_name,
                   s.title, s.start_time, s.end_time, s.description, s.shared_with_friends, s.joinable,
                   s.recruitment_limit,
                   s.created_at, s.updated_at
            FROM schedule_item s
            LEFT JOIN app_user u ON u.id = s.owner_user_id
            LEFT JOIN app_user su ON su.id = s.source_owner_user_id
            WHERE s.id = #{id}
              AND (
                  s.owner_user_id = #{viewerUserId}
                  OR (
                      s.shared_with_friends = TRUE
                      AND EXISTS (
                          SELECT 1
                          FROM friendship f
                          WHERE f.status = 'ACCEPTED'
                            AND (
                                (f.requester_user_id = s.owner_user_id AND f.addressee_user_id = #{viewerUserId})
                                OR (f.requester_user_id = #{viewerUserId} AND f.addressee_user_id = s.owner_user_id)
                            )
                      )
                  )
              )
            """)
    ScheduleItem findVisibleById(@Param("id") Long id, @Param("viewerUserId") Long viewerUserId);

    @Select("""
            SELECT s.id, s.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.profile_icon_color AS owner_profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS owner_has_profile_image,
                   s.schedule_date, s.priority, s.device_type, s.rank_band, s.completed, s.completed_at,
                   s.message_shareable, s.source_schedule_item_id, s.source_owner_user_id,
                   su.display_name AS source_owner_display_name,
                   s.title, s.start_time, s.end_time, s.description,
                   s.shared_with_friends, s.joinable, s.recruitment_limit, s.created_at, s.updated_at
            FROM schedule_item s
            LEFT JOIN app_user u ON u.id = s.owner_user_id
            LEFT JOIN app_user su ON su.id = s.source_owner_user_id
            WHERE s.id = #{id}
              AND s.owner_user_id = #{ownerUserId}
            """)
    ScheduleItem findOwnedById(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    @Select("""
            SELECT s.id, s.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.profile_icon_color AS owner_profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS owner_has_profile_image,
                   s.schedule_date, s.priority, s.device_type, s.rank_band, s.completed, s.completed_at,
                   s.message_shareable, s.source_schedule_item_id, s.source_owner_user_id,
                   su.display_name AS source_owner_display_name,
                   s.title, s.start_time, s.end_time, s.description, s.shared_with_friends, s.joinable,
                   s.recruitment_limit,
                   s.created_at, s.updated_at
            FROM schedule_item s
            LEFT JOIN app_user u ON u.id = s.owner_user_id
            LEFT JOIN app_user su ON su.id = s.source_owner_user_id
            WHERE s.schedule_date >= #{startDate}
              AND s.schedule_date < #{endExclusiveDate}
              AND (
                  s.owner_user_id = #{viewerUserId}
                  OR (
                      s.shared_with_friends = TRUE
                      AND EXISTS (
                          SELECT 1
                          FROM friendship f
                          WHERE f.status = 'ACCEPTED'
                            AND (
                                (f.requester_user_id = s.owner_user_id AND f.addressee_user_id = #{viewerUserId})
                                OR (f.requester_user_id = #{viewerUserId} AND f.addressee_user_id = s.owner_user_id)
                            )
                      )
                  )
              )
            ORDER BY s.schedule_date, COALESCE(s.start_time, TIME '23:59:59'), s.id
            """)
    List<ScheduleItem> findVisibleInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endExclusiveDate") LocalDate endExclusiveDate,
            @Param("viewerUserId") Long viewerUserId);

    @Insert("""
            INSERT INTO schedule_item (
                owner_user_id, schedule_date, priority, device_type, rank_band, completed, completed_at, message_shareable,
                source_schedule_item_id, source_owner_user_id,
                title, start_time, end_time, description, shared_with_friends, joinable, recruitment_limit
            )
            VALUES (
                #{ownerUserId}, #{scheduleDate}, #{priority}, #{deviceType}, #{rankBand}, #{completed}, #{completedAt}, #{messageShareable},
                #{sourceScheduleItemId}, #{sourceOwnerUserId},
                #{title}, #{startTime}, #{endTime}, #{description}, #{sharedWithFriends}, #{joinable}, #{recruitmentLimit}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleItem item);

    @Insert({
            "<script>",
            "INSERT INTO schedule_item (",
            "  owner_user_id, schedule_date, priority, device_type, rank_band, completed, completed_at, message_shareable,",
            "  source_schedule_item_id, source_owner_user_id,",
            "  title, start_time, end_time, description, shared_with_friends, joinable, recruitment_limit",
            ") VALUES ",
            "<foreach collection='items' item='item' separator=','>",
            "(",
            "  #{item.ownerUserId}, #{item.scheduleDate}, #{item.priority}, #{item.deviceType}, #{item.rankBand}, #{item.completed}, #{item.completedAt}, #{item.messageShareable},",
            "  #{item.sourceScheduleItemId}, #{item.sourceOwnerUserId},",
            "  #{item.title}, #{item.startTime}, #{item.endTime}, #{item.description}, #{item.sharedWithFriends}, #{item.joinable}, #{item.recruitmentLimit}",
            ")",
            "</foreach>",
            "</script>"
    })
    int bulkInsert(@Param("items") List<ScheduleItem> items);

    @Update("""
            UPDATE schedule_item
            SET schedule_date = #{scheduleDate},
                priority = #{priority},
                device_type = #{deviceType},
                rank_band = #{rankBand},
                title = #{title},
                start_time = #{startTime},
                end_time = #{endTime},
                description = #{description},
                shared_with_friends = #{sharedWithFriends},
                joinable = #{joinable},
                message_shareable = #{messageShareable},
                recruitment_limit = #{recruitmentLimit},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND owner_user_id = #{ownerUserId}
            """)
    int update(ScheduleItem item);

    @Update("""
            UPDATE schedule_item
            SET completed = TRUE,
                completed_at = #{completedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND owner_user_id = #{ownerUserId}
            """)
    int markCompleted(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId, @Param("completedAt") java.time.LocalDateTime completedAt);

    @Update("""
            UPDATE schedule_item
            SET completed = FALSE,
                completed_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND owner_user_id = #{ownerUserId}
            """)
    int markIncomplete(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    @Delete("""
            DELETE FROM schedule_item
            WHERE id = #{id}
              AND owner_user_id = #{ownerUserId}
            """)
    int delete(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    @Select("""
            SELECT COUNT(*)
            FROM schedule_participant
            WHERE schedule_item_id = #{scheduleId}
            """)
    int countParticipants(@Param("scheduleId") Long scheduleId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM schedule_participant
            WHERE schedule_item_id = #{scheduleId}
              AND participant_user_id = #{userId}
            """)
    boolean existsParticipant(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

    @Select("""
            SELECT u.id, u.username, u.display_name,
                   u.profile_icon_color AS profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS has_profile_image
            FROM schedule_participant sp
            INNER JOIN app_user u ON u.id = sp.participant_user_id
            WHERE sp.schedule_item_id = #{scheduleId}
            ORDER BY sp.created_at, sp.id
            """)
    List<FriendUser> findParticipants(@Param("scheduleId") Long scheduleId);

    @Insert("""
            INSERT INTO schedule_participant (schedule_item_id, participant_user_id)
            VALUES (#{scheduleId}, #{userId})
            """)
    int insertParticipant(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

    @Delete("""
            DELETE FROM schedule_participant
            WHERE schedule_item_id = #{scheduleId}
            """)
    int deleteAllParticipantsBySchedule(@Param("scheduleId") Long scheduleId);

    @Delete("""
            DELETE FROM schedule_participant
            WHERE schedule_item_id = #{scheduleId}
              AND participant_user_id = #{userId}
            """)
    int deleteParticipant(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM schedule_item
            WHERE owner_user_id = #{ownerUserId}
              AND source_schedule_item_id = #{sourceScheduleItemId}
            """)
    boolean existsSharedCopyBySource(
            @Param("ownerUserId") Long ownerUserId,
            @Param("sourceScheduleItemId") Long sourceScheduleItemId);

    @Select("""
            SELECT title
            FROM schedule_item
            WHERE owner_user_id = #{ownerUserId}
              AND title IS NOT NULL
              AND TRIM(title) <> ''
            GROUP BY title
            ORDER BY MAX(updated_at) DESC, title
            LIMIT #{limit}
            """)
    List<String> findRecentDistinctTitles(@Param("ownerUserId") Long ownerUserId, @Param("limit") int limit);

    @Select("""
            SELECT s.id, s.owner_user_id, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.profile_icon_color AS owner_profile_icon_color,
                   CASE WHEN u.profile_image_data IS NULL THEN FALSE ELSE TRUE END AS owner_has_profile_image,
                   s.schedule_date, s.priority, s.device_type, s.rank_band, s.completed, s.completed_at,
                   s.message_shareable, s.source_schedule_item_id, s.source_owner_user_id,
                   su.display_name AS source_owner_display_name,
                   s.title, s.start_time, s.end_time, s.description, s.shared_with_friends, s.joinable,
                   s.recruitment_limit,
                   s.created_at, s.updated_at
            FROM schedule_item s
            LEFT JOIN app_user u ON u.id = s.owner_user_id
            LEFT JOIN app_user su ON su.id = s.source_owner_user_id
            WHERE s.owner_user_id = #{ownerUserId}
              AND s.completed = FALSE
              AND s.schedule_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY s.schedule_date, COALESCE(s.start_time, TIME '23:59:59'), s.id
            """)
    List<ScheduleItem> findOwnedUncompletedInDateRange(
            @Param("ownerUserId") Long ownerUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT ranked.user_id AS user_id, ranked.username, ranked.display_name, ranked.rank_no AS rank, ranked.completed_count",
            "FROM (",
            "  SELECT u.id AS user_id, u.username, u.display_name,",
            "         COALESCE(c.completed_count, 0) AS completed_count,",
            "         RANK() OVER (ORDER BY COALESCE(c.completed_count, 0) DESC, u.id ASC) AS rank_no",
            "  FROM app_user u",
            "  LEFT JOIN (",
            "    SELECT s.owner_user_id, COUNT(*) AS completed_count",
            "    FROM schedule_item s",
            "    WHERE s.completed = TRUE",
            "      <if test='startAt != null'>AND s.completed_at &gt;= #{startAt}</if>",
            "      <if test='endAt != null'>AND s.completed_at &lt; #{endAt}</if>",
            "      AND s.owner_user_id IN",
            "      <foreach collection='userIds' item='uid' open='(' separator=',' close=')'>",
            "        #{uid}",
            "      </foreach>",
            "    GROUP BY s.owner_user_id",
            "  ) c ON c.owner_user_id = u.id",
            "  WHERE u.id IN",
            "  <foreach collection='userIds' item='uid' open='(' separator=',' close=')'>",
            "    #{uid}",
            "  </foreach>",
            ") ranked",
            "ORDER BY ranked.rank_no, ranked.user_id",
            "LIMIT 10",
            "</script>"
    })
    List<TaskCompletionRankingRow> findTaskCompletionRanking(
            @Param("userIds") List<Long> userIds,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);
}
