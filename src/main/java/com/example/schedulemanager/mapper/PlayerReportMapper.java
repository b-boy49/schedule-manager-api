package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.PlayerReport;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlayerReportMapper {

    @Insert("""
            INSERT INTO player_report (reporter_user_id, target_user_id, source_type, source_id, category, note, reason, status)
            VALUES (#{reporterUserId}, #{targetUserId}, #{sourceType}, #{sourceId}, #{category}, #{note}, #{reason}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlayerReport report);

    @Select("""
            SELECT pr.id, pr.reporter_user_id, ru.username AS reporter_username, ru.display_name AS reporter_display_name,
                   pr.target_user_id, tu.username AS target_username, tu.display_name AS target_display_name,
                   pr.source_type, pr.source_id, pr.category, pr.note, pr.reason, pr.status, pr.created_at
            FROM player_report pr
            JOIN app_user ru ON ru.id = pr.reporter_user_id
            JOIN app_user tu ON tu.id = pr.target_user_id
            WHERE (#{status} IS NULL OR pr.status = #{status})
            ORDER BY pr.created_at DESC, pr.id DESC
            LIMIT 300
            """)
    List<PlayerReport> findByStatus(@Param("status") String status);

    @Update("""
            UPDATE player_report
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
