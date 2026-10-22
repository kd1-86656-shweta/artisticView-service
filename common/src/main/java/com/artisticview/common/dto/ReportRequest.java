package com.artisticview.common.dto;

import com.artisticview.common.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(@NotNull ReportTargetType targetType, @NotNull Long targetId,
                            @NotBlank @Size(max = 1000) String reason) {
}
