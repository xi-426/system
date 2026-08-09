package com.yan.campuspass.user.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.campuspass.user.domain.SysUser;
import com.yan.campuspass.user.domain.UserRole;
import com.yan.campuspass.user.domain.UserStatus;
import com.yan.campuspass.user.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(
        name = "app.demo-data.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DemoUserInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "CampusPass123!";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoUserInitializer(UserMapper userMapper,
                               PasswordEncoder passwordEncoder,
                               Clock clock) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("organizer", "活动组织者", UserRole.ORGANIZER);
        createIfMissing("student", "测试学生", UserRole.STUDENT);
        createIfMissing("student2", "测试学生二", UserRole.STUDENT);
    }

    private void createIfMissing(String username,
                                 String displayName,
                                 UserRole role) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (count > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
    }
}
