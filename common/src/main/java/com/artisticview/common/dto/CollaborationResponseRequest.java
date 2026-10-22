package com.artisticview.common.dto;

import jakarta.validation.constraints.NotNull;

/** A co-host's answer to an invite. */
public record CollaborationResponseRequest(@NotNull Boolean accept) {
}
