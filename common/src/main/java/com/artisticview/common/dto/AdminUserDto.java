package com.artisticview.common.dto;

import com.artisticview.common.enums.UserStatus;

import java.time.Instant;
import java.util.List;

public record AdminUserDto(Long id, String email, String username, String displayName, List<String> roles,
                           UserStatus status, boolean emailVerified, Instant createdAt) {
}
