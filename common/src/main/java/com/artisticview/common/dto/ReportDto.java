package com.artisticview.common.dto;

import com.artisticview.common.enums.ReportStatus;
import com.artisticview.common.enums.ReportTargetType;

import java.time.Instant;

public record ReportDto(Long id, UserSummaryDto reporter, ReportTargetType targetType, Long targetId,
                        String reason, ReportStatus status, Long reviewedBy, Instant createdAt) {
}
