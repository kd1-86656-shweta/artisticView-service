package com.artisticview.common.dto;

public record SubFeatureAccessDto(Long id, String name, String displayName, String description, String route,
                                  Integer sequenceNumber, boolean canRead, boolean canWrite, boolean canDelete) {
}
