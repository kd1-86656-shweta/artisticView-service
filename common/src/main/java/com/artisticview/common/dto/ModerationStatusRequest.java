package com.artisticview.common.dto;

import jakarta.validation.constraints.NotNull;

public record ModerationStatusRequest(@NotNull Boolean hidden) {
}
