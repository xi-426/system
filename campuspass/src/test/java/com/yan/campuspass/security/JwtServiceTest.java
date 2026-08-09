package com.yan.campuspass.security;

import com.yan.campuspass.user.domain.SysUser;
import com.yan.campuspass.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtService jwtService = new JwtService(
                Clock.systemUTC(),
                "campuspass-test-secret-key-with-more-than-thirty-two-bytes",
                120
        );
        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername("organizer");
        user.setDisplayName("活动组织者");
        user.setRole(UserRole.ORGANIZER);

        GeneratedToken token = jwtService.generate(user);
        CurrentUser currentUser = jwtService.parse(token.value());

        assertThat(currentUser.userId()).isEqualTo(1001L);
        assertThat(currentUser.username()).isEqualTo("organizer");
        assertThat(currentUser.role()).isEqualTo(UserRole.ORGANIZER);
        assertThat(currentUser.tokenId()).isNotBlank();
        assertThat(currentUser.expiresAt()).isEqualTo(token.expiresAt());
    }
}
