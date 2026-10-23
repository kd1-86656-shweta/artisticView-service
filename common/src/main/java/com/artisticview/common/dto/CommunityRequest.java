package com.artisticview.common.dto;

import com.artisticview.common.enums.CommunityVisibility;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create or update a community. On create, name is required and slug defaults to one made from the name;
 * the slug cannot change later. On update, null fields stay as they are. coverKey "" removes the cover.
 */
public record CommunityRequest(
        @Size(min = 3, max = 80) String name,
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,38}[a-z0-9]$", message = "3-40 lower-case letters, numbers or dashes")
        String slug,
        @Size(max = 2000) String description,
        @Size(max = 4000) String rules,
        CommunityVisibility visibility,
        @Size(max = 300) String coverKey) {
}
