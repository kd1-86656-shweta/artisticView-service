package com.artisticview.security.util;

import com.artisticview.common.exception.AppException;
import com.artisticview.security.auth.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static AuthUser require() {
        return get().orElseThrow(() -> AppException.unauthorized("Login required"));
    }

    public static Long id() {
        return require().userId();
    }

    /** Viewer id or null for anonymous requests. */
    public static Long idOrNull() {
        return get().map(AuthUser::userId).orElse(null);
    }
}
