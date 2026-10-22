package com.artisticview.common.dto;

import java.util.List;

/**
 * Home / Discover payload. {@code featuredEvents} are promoted events; live rooms and communities are added here
 * as those features land.
 */
public record HomeDto(long memberCount, long newMembersThisWeek, boolean profileIncomplete,
                      List<PersonCardDto> suggestedPeople, List<EventDto> featuredEvents,
                      List<EventDto> upcomingEvents) {
}
