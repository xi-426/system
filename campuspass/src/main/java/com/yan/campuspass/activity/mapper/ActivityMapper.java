package com.yan.campuspass.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.campuspass.activity.domain.Activity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityMapper extends BaseMapper<Activity> {

    @Update("""
            UPDATE activity
            SET status = 'PUBLISHED',
                updated_at = #{updatedAt},
                version = version + 1
            WHERE id = #{activityId}
              AND organizer_id = #{organizerId}
              AND status = 'DRAFT'
            """)
    int publish(@Param("activityId") Long activityId,
                @Param("organizerId") Long organizerId,
                @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity
            SET registered_count = registered_count + 1,
                updated_at = #{updatedAt},
                version = version + 1
            WHERE id = #{activityId}
              AND status = 'PUBLISHED'
              AND registration_start_time <= #{updatedAt}
              AND registration_end_time > #{updatedAt}
              AND registered_count < capacity
            """)
    int reserveSeat(@Param("activityId") Long activityId,
                    @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity
            SET registered_count = registered_count - 1,
                updated_at = #{updatedAt},
                version = version + 1
            WHERE id = #{activityId}
              AND registered_count > 0
            """)
    int releaseSeat(@Param("activityId") Long activityId,
                    @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity
            SET status = 'REGISTRATION_CLOSED',
                updated_at = #{now}
            WHERE status = 'PUBLISHED'
              AND registration_end_time <= #{now}
            """)
    int closeRegistrations(@Param("now") LocalDateTime now);

    @Update("""
            UPDATE activity
            SET status = 'ONGOING',
                updated_at = #{now}
            WHERE status IN ('PUBLISHED', 'REGISTRATION_CLOSED')
              AND activity_start_time <= #{now}
              AND activity_end_time > #{now}
            """)
    int startActivities(@Param("now") LocalDateTime now);

    @Update("""
            UPDATE activity
            SET status = 'FINISHED',
                updated_at = #{now}
            WHERE status IN (
                    'PUBLISHED',
                    'REGISTRATION_CLOSED',
                    'ONGOING'
                  )
              AND activity_end_time <= #{now}
            """)
    int finishActivities(@Param("now") LocalDateTime now);

    @Select("""
            SELECT *
            FROM activity
            WHERE reminder_sent = 0
              AND status IN ('PUBLISHED', 'REGISTRATION_CLOSED')
              AND activity_start_time > #{now}
              AND activity_start_time <= #{cutoff}
            """)
    List<Activity> selectNeedingReminder(
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff);

    @Update("""
            UPDATE activity
            SET reminder_sent = 1,
                updated_at = #{now}
            WHERE id = #{activityId}
              AND reminder_sent = 0
            """)
    int markReminderSent(
            @Param("activityId") Long activityId,
            @Param("now") LocalDateTime now);
}
