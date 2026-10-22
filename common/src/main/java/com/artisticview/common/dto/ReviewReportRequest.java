package com.artisticview.common.dto;

import jakarta.validation.constraints.NotNull;

/** {@code removeContent} hides the reported artwork or comment, or suspends the reported user. */
public record ReviewReportRequest(@NotNull Boolean removeContent) {
}
