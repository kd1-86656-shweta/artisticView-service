package com.artisticview.common.dto;

import com.artisticview.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
