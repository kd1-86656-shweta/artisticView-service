package com.artisticview.common.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code identifier} is either the email or the username. */
public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
}
