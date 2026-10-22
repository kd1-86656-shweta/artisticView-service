package com.artisticview.common.dto;

import com.artisticview.common.enums.ProfileVisibility;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProfileDto(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String coverUrl,
        List<String> interests,
        Map<String, String> links,
        ProfileVisibility profileVisibility,
        long followerCount,
        long followingCount,
        boolean followedByMe,
        boolean blockedByMe,
        boolean me,
        Instant createdAt) {
}
