package com.yan.campuspass.waitlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.campuspass.waitlist.domain.ActivityWaitlist;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface WaitlistMapper extends BaseMapper<ActivityWaitlist> {

    @Select("""
            SELECT *
            FROM activity_waitlist
            WHERE activity_id = #{activityId}
              AND status = 'WAITING'
            ORDER BY joined_at ASC, id ASC
            LIMIT 1
            FOR UPDATE
            """)
    ActivityWaitlist selectFirstWaitingForUpdate(
            @Param("activityId") Long activityId);

    @Select("""
            SELECT COUNT(*)
            FROM activity_waitlist
            WHERE activity_id = #{activityId}
              AND status = 'WAITING'
            """)
    int countWaiting(@Param("activityId") Long activityId);

    @Update("""
            UPDATE activity_waitlist
            SET status = 'PROMOTED',
                promoted_at = #{updatedAt},
                updated_at = #{updatedAt}
            WHERE id = #{waitlistId}
              AND status = 'WAITING'
            """)
    int promote(@Param("waitlistId") Long waitlistId,
                @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity_waitlist
            SET status = 'CANCELLED',
                cancelled_at = #{updatedAt},
                updated_at = #{updatedAt}
            WHERE id = #{waitlistId}
              AND user_id = #{userId}
              AND status = 'WAITING'
            """)
    int cancel(@Param("waitlistId") Long waitlistId,
               @Param("userId") Long userId,
               @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity_waitlist
            SET status = 'WAITING',
                joined_at = #{updatedAt},
                promoted_at = NULL,
                cancelled_at = NULL,
                updated_at = #{updatedAt}
            WHERE id = #{waitlistId}
              AND user_id = #{userId}
              AND status IN ('CANCELLED', 'PROMOTED', 'EXPIRED')
            """)
    int rejoin(@Param("waitlistId") Long waitlistId,
               @Param("userId") Long userId,
               @Param("updatedAt") LocalDateTime updatedAt);
}
