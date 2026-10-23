package com.artisticview.common.dto;

import com.artisticview.common.enums.CollaboratorStatus;
import com.artisticview.common.enums.EventLocationType;
import com.artisticview.common.enums.EventStatus;

import java.time.Instant;
import java.util.List;

/**
 * An event as the viewer sees it. {@code canManage} is true for the host and accepted co-hosts;
 * {@code myCollaboration} is the viewer's own co-host invite status, or null if they were never invited.
 */
public record EventDto(Long id, String title, String description, String category,
                       Instant startsAt, Instant endsAt, EventLocationType locationType, String location,
                       String meetingUrl, Integer capacity, String coverUrl, EventStatus status,
                       UserSummaryDto host, List<CollaboratorDto> collaborators,
                       long rsvpCount, boolean going, boolean promoted, Instant promotedUntil,
                       boolean isHost, boolean canManage, CollaboratorStatus myCollaboration,
                       Instant createdAt) {

    public record CollaboratorDto(UserSummaryDto user, CollaboratorStatus status) {
    }
}
