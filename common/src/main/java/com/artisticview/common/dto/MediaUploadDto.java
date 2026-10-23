package com.artisticview.common.dto;

/** A stored image: send {@code key} back when saving a profile, event or post; show {@code url}. */
public record MediaUploadDto(String key, String url) {
}
