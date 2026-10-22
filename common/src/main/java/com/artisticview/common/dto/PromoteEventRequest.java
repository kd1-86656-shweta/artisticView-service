package com.artisticview.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Feature the event for this many days from now (or from the end of a promotion already running). */
public record PromoteEventRequest(@NotNull @Min(1) @Max(30) Integer days) {
}
