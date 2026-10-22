package com.artisticview.common.dto;

import com.artisticview.common.enums.ProfileVisibility;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Null fields are left unchanged. */
public record UpdateProfileRequest(
        @Size(max = 80) String displayName,
        @Size(max = 1000) String bio,
        String avatarKey,
        String coverKey,
        @Size(max = 15) List<@Size(max = 40) String> interests,
        @Size(max = 10) Map<String, String> links,
        ProfileVisibility profileVisibility) {
}
