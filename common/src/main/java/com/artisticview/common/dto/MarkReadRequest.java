package com.artisticview.common.dto;

import java.util.List;

/** Marks the given ids as read, or everything when {@code all} is true. */
public record MarkReadRequest(List<Long> ids, boolean all) {
}
