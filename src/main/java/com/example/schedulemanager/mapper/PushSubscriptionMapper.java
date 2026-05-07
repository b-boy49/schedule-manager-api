package com.example.schedulemanager.mapper;

import com.example.schedulemanager.model.PushSubscription;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PushSubscriptionMapper {

    @Insert("""
            INSERT INTO push_subscription (user_id, endpoint, p256dh, auth)
            VALUES (#{userId}, #{endpoint}, #{p256dh}, #{auth})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PushSubscription subscription);

    @Delete("""
            DELETE FROM push_subscription
            WHERE endpoint = #{endpoint}
            """)
    int deleteByEndpoint(@Param("endpoint") String endpoint);

    @Select("""
            SELECT id, user_id, endpoint, p256dh, auth, created_at
            FROM push_subscription
            WHERE user_id = #{userId}
            """)
    List<PushSubscription> findByUserId(@Param("userId") Long userId);
}
