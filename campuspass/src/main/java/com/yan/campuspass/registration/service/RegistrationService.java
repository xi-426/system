package com.yan.campuspass.registration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.domain.ActivityStatus;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.common.exception.BusinessException;
import com.yan.campuspass.notification.event.NotificationEventPublisher;
import com.yan.campuspass.registration.domain.ActivityRegistration;
import com.yan.campuspass.registration.domain.RegistrationStatus;
import com.yan.campuspass.registration.dto.ReservationResultResponse;
import com.yan.campuspass.registration.dto.RegistrationResponse;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import com.yan.campuspass.waitlist.domain.ActivityWaitlist;
import com.yan.campuspass.waitlist.domain.WaitlistStatus;
import com.yan.campuspass.waitlist.dto.WaitlistResponse;
import com.yan.campuspass.waitlist.mapper.WaitlistMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final WaitlistMapper waitlistMapper;
    private final NotificationEventPublisher notificationPublisher;
    private final Clock clock;

    public RegistrationService(ActivityMapper activityMapper,
                               RegistrationMapper registrationMapper,
                               WaitlistMapper waitlistMapper,
                               NotificationEventPublisher notificationPublisher,
                               Clock clock) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.waitlistMapper = waitlistMapper;
        this.notificationPublisher = notificationPublisher;
        this.clock = clock;
    }

    @Transactional
    public ReservationResultResponse reserve(Long userId, Long activityId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Activity activity = getActivity(activityId);
        validateRegistrationTime(activity, now);

        ActivityWaitlist waitlist = findWaitlist(userId, activityId);
        if (waitlist != null && waitlist.getStatus() == WaitlistStatus.WAITING) {
            throw new BusinessException("你已经在该活动的候补名单中");
        }

        ActivityRegistration existing = findRegistration(userId, activityId);
        if (existing != null) {
            if (existing.getStatus() == RegistrationStatus.RESERVED) {
                throw new BusinessException("请勿重复预约同一活动");
            }
            if (existing.getStatus() == RegistrationStatus.CANCELLED) {
                int seatRows = activityMapper.reserveSeat(activityId, now);
                if (seatRows == 1) {
                    return reactivate(userId, activityId, existing, now);
                }
                validateRegistrationTime(getActivity(activityId), now);
                return joinWaitlist(userId, activityId, waitlist, now);
            }
            throw new BusinessException("当前预约状态不能重新预约");
        }

        int affectedRows = activityMapper.reserveSeat(activityId, now);
        if (affectedRows != 1) {
            validateRegistrationTime(getActivity(activityId), now);
            return joinWaitlist(userId, activityId, waitlist, now);
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(userId);
        registration.setStatus(RegistrationStatus.RESERVED);
        registration.setReservedAt(now);
        registration.setCreatedAt(now);
        registration.setUpdatedAt(now);

        try {
            registrationMapper.insert(registration);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("请勿重复预约同一活动");
        }
        notificationPublisher.publish(userId,"RESERVED","预约成功","你已成功预约活动 "+activityId);
        return ReservationResultResponse.reserved(
                activityId,
                userId,
                registration.getId(),
                now
        );
    }

    @Transactional
    public RegistrationResponse cancel(Long userId, Long activityId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Activity activity = getActivity(activityId);
        if (!now.isBefore(activity.getActivityStartTime())) {
            throw new BusinessException("活动已经开始，不能取消预约");
        }

        ActivityRegistration registration = findRegistration(userId, activityId);
        if (registration == null
                || registration.getStatus() != RegistrationStatus.RESERVED) {
            throw new BusinessException("当前没有可取消的预约");
        }

        int cancelledRows = registrationMapper.cancel(
                registration.getId(),
                userId,
                now
        );
        if (cancelledRows != 1) {
            throw new BusinessException("预约状态已经变化，请刷新后重试");
        }

        ActivityWaitlist firstWaiting =
                waitlistMapper.selectFirstWaitingForUpdate(activityId);
        if (firstWaiting == null) {
            int releasedRows = activityMapper.releaseSeat(activityId, now);
            if (releasedRows != 1) {
                throw new BusinessException("活动名额数据异常，取消失败");
            }
        } else {
            promote(firstWaiting, now);
        }
        notificationPublisher.publish(userId,"CANCELLED","预约已取消","你已取消活动 "+activityId+" 的预约");

        return RegistrationResponse.from(
                registrationMapper.selectById(registration.getId())
        );
    }

    @Transactional
    public WaitlistResponse cancelWaitlist(Long userId, Long activityId) {
        ActivityWaitlist waitlist = findWaitlist(userId, activityId);
        if (waitlist == null || waitlist.getStatus() != WaitlistStatus.WAITING) {
            throw new BusinessException("当前没有可取消的候补记录");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int affectedRows = waitlistMapper.cancel(waitlist.getId(), userId, now);
        if (affectedRows != 1) {
            throw new BusinessException("候补状态已经变化，请刷新后重试");
        }
        notificationPublisher.publish(userId,"WAITLIST_CANCELLED","已退出候补","你已退出活动 "+activityId+" 的候补");
        return WaitlistResponse.from(waitlistMapper.selectById(waitlist.getId()));
    }

    private ReservationResultResponse reactivate(
            Long userId,
            Long activityId,
            ActivityRegistration existing,
            LocalDateTime now) {
        int registrationRows = registrationMapper.reactivate(
                existing.getId(),
                userId,
                now
        );
        if (registrationRows != 1) {
            throw new BusinessException("预约状态已经变化，请刷新后重试");
        }
        notificationPublisher.publish(userId,"RESERVED","预约成功","你已重新预约活动 "+activityId);
        return ReservationResultResponse.reserved(
                activityId,
                userId,
                existing.getId(),
                now
        );
    }

    private ReservationResultResponse joinWaitlist(
            Long userId,
            Long activityId,
            ActivityWaitlist existing,
            LocalDateTime now) {
        ActivityWaitlist waitlist = existing;
        if (waitlist == null) {
            waitlist = new ActivityWaitlist();
            waitlist.setActivityId(activityId);
            waitlist.setUserId(userId);
            waitlist.setStatus(WaitlistStatus.WAITING);
            waitlist.setJoinedAt(now);
            waitlist.setCreatedAt(now);
            waitlist.setUpdatedAt(now);
            try {
                waitlistMapper.insert(waitlist);
            } catch (DuplicateKeyException exception) {
                throw new BusinessException("你已经加入该活动候补");
            }
        } else {
            int affectedRows = waitlistMapper.rejoin(
                    waitlist.getId(),
                    userId,
                    now
            );
            if (affectedRows != 1) {
                throw new BusinessException("候补状态已经变化，请刷新后重试");
            }
        }
        int position = waitlistMapper.countWaiting(activityId);
        notificationPublisher.publish(userId,"WAITLISTED","已进入候补","你已进入活动 "+activityId+" 的候补名单");
        return ReservationResultResponse.waitlisted(
                activityId,
                userId,
                waitlist.getId(),
                position,
                now
        );
    }

    private void promote(ActivityWaitlist waitlist, LocalDateTime now) {
        int promotedRows = waitlistMapper.promote(waitlist.getId(), now);
        if (promotedRows != 1) {
            throw new BusinessException("候补状态已经变化，递补失败");
        }

        ActivityRegistration promotedRegistration =
                findRegistration(waitlist.getUserId(), waitlist.getActivityId());
        if (promotedRegistration == null) {
            promotedRegistration = new ActivityRegistration();
            promotedRegistration.setActivityId(waitlist.getActivityId());
            promotedRegistration.setUserId(waitlist.getUserId());
            promotedRegistration.setStatus(RegistrationStatus.RESERVED);
            promotedRegistration.setReservedAt(now);
            promotedRegistration.setCreatedAt(now);
            promotedRegistration.setUpdatedAt(now);
            registrationMapper.insert(promotedRegistration);
        } else if (promotedRegistration.getStatus()
                == RegistrationStatus.CANCELLED) {
            int rows = registrationMapper.reactivate(
                    promotedRegistration.getId(),
                    promotedRegistration.getUserId(),
                    now
            );
            if (rows != 1) {
                throw new BusinessException("候补预约状态异常，递补失败");
            }
        } else {
            throw new BusinessException("候补用户预约状态异常，递补失败");
        }
        notificationPublisher.publish(waitlist.getUserId(),"PROMOTED","候补递补成功",
                "活动 "+waitlist.getActivityId()+" 已为你递补预约");
    }

    private Activity getActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private ActivityRegistration findRegistration(Long userId, Long activityId) {
        return registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId)
        );
    }

    private ActivityWaitlist findWaitlist(Long userId, Long activityId) {
        return waitlistMapper.selectOne(
                new LambdaQueryWrapper<ActivityWaitlist>()
                        .eq(ActivityWaitlist::getActivityId, activityId)
                        .eq(ActivityWaitlist::getUserId, userId)
        );
    }

    private void validateRegistrationTime(Activity activity, LocalDateTime now) {
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException("活动当前不可预约");
        }
        if (now.isBefore(activity.getRegistrationStartTime())) {
            throw new BusinessException("活动报名尚未开始");
        }
        if (!now.isBefore(activity.getRegistrationEndTime())) {
            throw new BusinessException("活动报名已经截止");
        }
    }
}
