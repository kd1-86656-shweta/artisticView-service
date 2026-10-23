package com.artisticview.common.dto;

import com.artisticview.common.enums.CommunityRole;
import jakarta.validation.constraints.NotNull;

/** Make a member a moderator or back to member. Ownership is not transferable through this request. */
public record MemberRoleRequest(@NotNull CommunityRole role) {
}
