package com.artisticview.security.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param secret          HMAC-SHA256 signing secret, at least 32 bytes. Always from the environment in prod.
 * @param accessTokenTtl  lifetime of access tokens (sent as Bearer header)
 * @param refreshTokenTtl lifetime of refresh tokens (httpOnly cookie, rotated on every use)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
    }
}
