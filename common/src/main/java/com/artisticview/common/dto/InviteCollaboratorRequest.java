package com.artisticview.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteCollaboratorRequest(@NotBlank @Size(max = 30) String username) {
}
