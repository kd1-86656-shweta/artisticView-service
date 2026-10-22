package com.artisticview.core.auth;

import com.artisticview.common.dto.AuthResponse;

import java.time.Instant;

/** What the controller needs: the JSON body plus the refresh token to put in the cookie. */
public record AuthResult(AuthResponse body, String refreshToken, Instant refreshExpiresAt) {
}
