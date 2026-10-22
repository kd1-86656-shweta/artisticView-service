package com.artisticview.common.dto;

/** The access token goes in the body; the refresh token is set as an httpOnly cookie. */
public record AuthResponse(String accessToken, long expiresInSeconds, SessionDto session) {
}
