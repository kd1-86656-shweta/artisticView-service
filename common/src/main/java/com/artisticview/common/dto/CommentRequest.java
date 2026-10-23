package com.artisticview.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code parentId} replies to another comment on the same post (create only). */
public record CommentRequest(@NotBlank @Size(max = 5000) String body, Long parentId) {
}
