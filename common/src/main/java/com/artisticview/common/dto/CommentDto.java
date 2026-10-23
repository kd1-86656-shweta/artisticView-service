package com.artisticview.common.dto;

import java.time.Instant;

/** A comment; deleted ones keep their place with a null author and body so replies still make sense. */
public record CommentDto(Long id, Long postId, Long parentId, UserSummaryDto author, String body, boolean deleted,
                         boolean canEdit, boolean canDelete, Instant createdAt, Instant editedAt) {
}
