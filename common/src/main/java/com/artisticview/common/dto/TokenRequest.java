package com.artisticview.common.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {
}
