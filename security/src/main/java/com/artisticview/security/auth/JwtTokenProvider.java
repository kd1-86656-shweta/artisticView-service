package com.artisticview.security.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and parses JWTs (HS256), following agasty-service's provider: short-lived access tokens carrying
 * userId + roleIds, and refresh tokens identified by a random jti that the server tracks for rotation/revocation.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE_IDS = "roleIds";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private SecretKey key;

    @PostConstruct
    void init() {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be set and at least 32 bytes long");
        }
        key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, String email, List<Long> roleIds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE_IDS, roleIds)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    /** Returns the token and its jti; the caller stores a hash of the jti. */
    public IssuedRefreshToken generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.refreshTokenTtl());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_USER_ID, userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedRefreshToken(token, jti, expiresAt);
    }

    /** Parses and verifies signature and expiry. Throws {@link JwtException} when invalid or expired. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public AuthUser toAuthUser(Claims claims) {
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Not an access token");
        }
        List<?> rawRoles = claims.get(CLAIM_ROLE_IDS, List.class);
        List<Long> roleIds = rawRoles == null ? List.of()
                : rawRoles.stream().map(r -> ((Number) r).longValue()).toList();
        return new AuthUser(
                claims.get(CLAIM_USER_ID, Number.class).longValue(),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_EMAIL, String.class),
                roleIds);
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public record IssuedRefreshToken(String token, String jti, Instant expiresAt) {
    }
}
