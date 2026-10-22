package com.artisticview.common.dto;

public record PlatformStatsDto(long totalUsers, long activeUsers, long suspendedUsers, long newUsersLast7Days,
                               long openReports) {
}
