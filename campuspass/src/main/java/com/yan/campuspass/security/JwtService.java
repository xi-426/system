package com.yan.campuspass.security;

import com.yan.campuspass.user.domain.SysUser;
import com.yan.campuspass.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Clock clock;
    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(Clock clock,
                      @Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT密钥长度不能少于32字节");
        }
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public GeneratedToken generate(SysUser user) {
        Instant issuedAt = clock.instant()
                .truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(tokenId)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("displayName", user.getDisplayName())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new GeneratedToken(token, expiresAt);
    }

    public CurrentUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new MalformedJwtException("Token缺少jti");
        }

        Number userId = (Number) claims.get("userId");
        return new CurrentUser(
                userId.longValue(),
                claims.getSubject(),
                claims.get("displayName", String.class),
                UserRole.valueOf(claims.get("role", String.class)),
                tokenId,
                claims.getExpiration().toInstant()
        );
    }
}
