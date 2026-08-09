package com.yan.campuspass.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.campuspass.auth.dto.LoginRequest;
import com.yan.campuspass.auth.dto.LoginResponse;
import com.yan.campuspass.common.exception.UnauthorizedException;
import com.yan.campuspass.security.GeneratedToken;
import com.yan.campuspass.security.JwtService;
import com.yan.campuspass.user.domain.SysUser;
import com.yan.campuspass.user.domain.UserStatus;
import com.yan.campuspass.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.username())
        );

        if (user == null
                || user.getStatus() != UserStatus.ENABLED
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        GeneratedToken token = jwtService.generate(user);
        return LoginResponse.from(user, token.value(), token.expiresAt());
    }
}
