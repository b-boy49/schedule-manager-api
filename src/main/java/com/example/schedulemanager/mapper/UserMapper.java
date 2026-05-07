package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.AppUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash, email, total_points, display_name, profile_bio, x_url, stream_url, profile_image_url, profile_icon_color,
                   profile_image_data, profile_image_content_type, enabled, created_at
            FROM app_user
            WHERE username = #{username}
            """)
    AppUser findByUsername(String username);

    @Select("""
            SELECT id, username, password_hash, email, total_points, display_name, profile_bio, x_url, stream_url, profile_image_url, profile_icon_color,
                   profile_image_data, profile_image_content_type, enabled, created_at
            FROM app_user
            WHERE id = #{id}
            """)
    AppUser findById(Long id);

    @Select("""
            SELECT id, username, password_hash, email, total_points, display_name, profile_bio, x_url, stream_url, profile_image_url, profile_icon_color,
                   profile_image_data, profile_image_content_type, enabled, created_at
            FROM app_user
            WHERE email = #{email}
            """)
    AppUser findByEmail(String email);

    @Select("""
            SELECT id, username, password_hash, email, total_points, display_name, profile_bio, x_url, stream_url, profile_image_url, profile_icon_color,
                   profile_image_data, profile_image_content_type, enabled, created_at
            FROM app_user
            WHERE enabled = TRUE
              AND email IS NOT NULL
              AND TRIM(email) <> ''
            ORDER BY id
            """)
    java.util.List<AppUser> findEnabledUsersWithEmail();

    @Insert("""
            INSERT INTO app_user (
                username, password_hash, email, total_points, display_name, profile_bio, x_url, stream_url, profile_image_url, profile_icon_color,
                profile_image_data, profile_image_content_type, enabled
            )
            VALUES (
                #{username}, #{passwordHash}, #{email}, #{totalPoints}, #{displayName}, #{profileBio}, #{xUrl}, #{streamUrl}, #{profileImageUrl}, #{profileIconColor},
                #{profileImageData}, #{profileImageContentType}, #{enabled}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AppUser user);

    @Update("""
            UPDATE app_user
            SET display_name = #{displayName},
                email = #{email},
                profile_bio = #{profileBio},
                x_url = #{xUrl},
                stream_url = #{streamUrl},
                profile_image_url = #{profileImageUrl},
                profile_icon_color = #{profileIconColor},
                profile_image_data = #{profileImageData},
                profile_image_content_type = #{profileImageContentType}
            WHERE id = #{id}
            """)
    int updateProfile(AppUser user);

    @Update("""
            UPDATE app_user
            SET total_points = COALESCE(total_points, 0) + #{points}
            WHERE id = #{userId}
            """)
    int addPoints(@org.apache.ibatis.annotations.Param("userId") Long userId, @org.apache.ibatis.annotations.Param("points") int points);

    @Update("""
            UPDATE app_user
            SET enabled = #{enabled}
            WHERE id = #{userId}
            """)
    int updateEnabled(@org.apache.ibatis.annotations.Param("userId") Long userId, @org.apache.ibatis.annotations.Param("enabled") boolean enabled);
}
