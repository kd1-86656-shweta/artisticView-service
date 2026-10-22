package com.artisticview.common.dto;

import java.util.List;

/** Compact person tile used on Home/Discover and people lists. */
public record PersonCardDto(Long id, String username, String displayName, String avatarUrl, String bio,
                            List<String> interests, long followerCount, boolean followedByMe) {
}
