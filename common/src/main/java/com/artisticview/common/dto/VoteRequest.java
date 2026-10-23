package com.artisticview.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 1 = upvote, -1 = downvote, 0 = remove my vote. */
public record VoteRequest(@NotNull @Min(-1) @Max(1) Integer value) {
}
