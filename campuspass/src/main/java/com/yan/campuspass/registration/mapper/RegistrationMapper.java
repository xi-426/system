package com.yan.campuspass.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.campuspass.registration.domain.ActivityRegistration;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistrationMapper extends BaseMapper<ActivityRegistration> {

    @Update("""
            UPDATE activity_registration
            SET status = 'CANCELLED',
                cancelled_at = #{updatedAt},
                updated_at = #{updatedAt}
            WHERE id = #{registrationId}
              AND user_id = #{userId}
              AND status = 'RESERVED'
            """)
    int cancel(@Param("registrationId") Long registrationId,
               @Param("userId") Long userId,
               @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity_registration
            SET status = 'RESERVED',
                reserved_at = #{updatedAt},
                cancelled_at = NULL,
                updated_at = #{updatedAt}
            WHERE id = #{registrationId}
              AND user_id = #{userId}
              AND status = 'CANCELLED'
            """)
    int reactivate(@Param("registrationId") Long registrationId,
                   @Param("userId") Long userId,
                   @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE activity_registration
            SET status = 'CHECKED_IN',
                checked_in_at = #{updatedAt},
                updated_at = #{updatedAt}
            WHERE activity_id = #{activityId}
              AND user_id = #{userId}
              AND status = 'RESERVED'
            """)
    int checkIn(@Param("activityId") Long activityId,
                @Param("userId") Long userId,
                @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            SELECT user_id
            FROM activity_registration
            WHERE activity_id = #{activityId}
              AND status = 'RESERVED'
            """)
    List<Long> selectReservedUserIds(
            @Param("activityId") Long activityId);

    @Update("""
            UPDATE activity_registration registration
            JOIN activity
              ON activity.id = registration.activity_id
            SET registration.status = 'ABSENT',
                registration.updated_at = #{now}
            WHERE registration.status = 'RESERVED'
              AND activity.status = 'FINISHED'
            """)
    int markAbsences(@Param("now") LocalDateTime now);
}
