package com.artisticview.common.dto;

import java.time.Instant;

/** A chat message. {@code system} notes ("Riya joined") have no sender. Deleted messages keep their place. */
public record MessageDto(Long id, Long conversationId, UserSummaryDto sender, String body, String imageUrl,
                         boolean deleted, boolean system, Instant createdAt) {
}
