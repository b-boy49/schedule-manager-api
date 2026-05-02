package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.LabelColorSetting;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LabelColorMapper {

    @Select("""
            SELECT id, user_id, label_key, color_hex, created_at, updated_at
            FROM label_colors
            WHERE user_id = #{userId}
            ORDER BY label_key
            """)
    List<LabelColorSetting> findByUserId(@Param("userId") Long userId);

    @Insert("""
            MERGE INTO label_colors (user_id, label_key, color_hex, updated_at)
            KEY (user_id, label_key)
            VALUES (#{userId}, #{labelKey}, #{colorHex}, CURRENT_TIMESTAMP)
            """)
    int upsert(@Param("userId") Long userId, @Param("labelKey") String labelKey, @Param("colorHex") String colorHex);

    @Delete("""
            DELETE FROM label_colors
            WHERE user_id = #{userId}
              AND label_key = #{labelKey}
            """)
    int deleteOne(@Param("userId") Long userId, @Param("labelKey") String labelKey);

    @Delete("""
            DELETE FROM label_colors
            WHERE user_id = #{userId}
            """)
    int deleteAll(@Param("userId") Long userId);
}
