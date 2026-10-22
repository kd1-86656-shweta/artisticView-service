package com.artisticview.security.auth;

import java.util.List;

/** The authenticated principal, built from access-token claims. */
public record AuthUser(Long userId, String username, String email, List<Long> roleIds) {
}
