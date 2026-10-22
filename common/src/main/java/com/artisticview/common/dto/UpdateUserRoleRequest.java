package com.artisticview.common.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Replaces the user's roles with the given role internal names (e.g. MEMBER, MODERATOR). */
public record UpdateUserRoleRequest(@NotEmpty List<String> roles) {
}
