package com.artisticview.common.dto;

import com.artisticview.common.enums.CommunityRole;
import com.artisticview.common.enums.MembershipStatus;

import java.time.Instant;

public record CommunityMemberDto(UserSummaryDto user, CommunityRole role, MembershipStatus status, Instant since) {
}
