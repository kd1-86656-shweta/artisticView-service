package com.artisticview.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$", message = "3-30 letters, digits or underscore") String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 80) String displayName) {
}
