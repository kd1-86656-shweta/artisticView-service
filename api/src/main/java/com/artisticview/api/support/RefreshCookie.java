package com.artisticview.api.support;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** The refresh token lives only in an httpOnly cookie scoped to /auth, so page scripts can never read it. */
@Component
public class RefreshCookie {

    public static final String NAME = "av_refresh";
    private static final String PATH = "/auth";

    private final boolean secure;

    public RefreshCookie(@Value("${app.cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    public void set(HttpServletResponse response, String token, Instant expiresAt) {
        ResponseCookie cookie = ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(Duration.between(Instant.now(), expiresAt))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
