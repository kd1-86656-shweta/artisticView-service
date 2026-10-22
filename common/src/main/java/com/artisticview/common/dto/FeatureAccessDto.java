package com.artisticview.common.dto;

import java.util.List;

public record FeatureAccessDto(Long id, String name, String featureName, String displayName, String description,
                               String icon, String route, Integer sequenceNumber,
                               List<SubFeatureAccessDto> subFeatures) {
}
