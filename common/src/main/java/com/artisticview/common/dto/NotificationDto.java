package com.artisticview.common.dto;

import com.artisticview.common.enums.NotificationType;

import java.time.Instant;
import java.util.Map;

public record NotificationDto(Long id, NotificationType type, UserSummaryDto actor, String entityType,
                              Long entityId, Map<String, Object> payload, boolean read, Instant createdAt) {
}
