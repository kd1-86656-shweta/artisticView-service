package com.artisticview.common.dto;

import java.util.List;
import java.util.Set;

/**
 * Everything the frontend needs after login: who the user is, which privileges they hold and which
 * features/sub-features (with read/write/delete) to show.
 */
public record SessionDto(MeDto user, Set<String> permissions, List<FeatureAccessDto> features) {
}
