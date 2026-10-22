package com.artisticview.common.dto;

import java.util.List;

public record MeDto(Long id, String email, String username, String displayName, String avatarUrl,
                    List<String> roles, boolean emailVerified) {
}
