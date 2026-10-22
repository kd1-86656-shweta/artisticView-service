package com.artisticview.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(@NotBlank @Email String email) {
}
