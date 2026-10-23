package com.artisticview.common.dto;

import com.artisticview.common.enums.CommunityRole;
import com.artisticview.common.enums.CommunityVisibility;
import com.artisticview.common.enums.MembershipStatus;

import java.time.Instant;

/**
 * A community as the viewer sees it. {@code myRole}/{@code myStatus} are null when the viewer is not a member
 * and has no invite; {@code canModerate} is true for its owner and moderators.
 */
public record CommunityDto(Long id, String slug, String name, String description, String rules,
                           CommunityVisibility visibility, String coverUrl, long memberCount, long postCount,
                           UserSummaryDto creator, CommunityRole myRole, MembershipStatus myStatus,
                           boolean canModerate, Instant createdAt) {
}
