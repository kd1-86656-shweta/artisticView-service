package com.artisticview.common.dto;

import com.artisticview.common.enums.EventLocationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Create or update an event. On create, title, startsAt and locationType are required (checked in the service);
 * on update, null fields are left unchanged. {@code collaborators} are usernames to invite as co-hosts (create only).
 */
public record EventRequest(
        @Size(max = 120) String title,
        @Size(max = 4000) String description,
        @Size(max = 40) String category,
        Instant startsAt,
        Instant endsAt,
        EventLocationType locationType,
        @Size(max = 200) String location,
        @Size(max = 500) String meetingUrl,
        @Min(1) @Max(100000) Integer capacity,
        @Size(max = 10) List<@Size(max = 30) String> collaborators,
        @Min(0) @Max(30) Integer promoteDays) {
}
