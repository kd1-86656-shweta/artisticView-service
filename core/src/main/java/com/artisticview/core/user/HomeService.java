package com.artisticview.core.user;

import com.artisticview.common.dto.HomeDto;
import com.artisticview.core.event.EventService;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Home / Discover. Surfaces people, community size, promoted ("featured") events and what's coming up next;
 * live rooms and communities join this response as those features land.
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int SUGGESTION_COUNT = 8;
    private static final int EVENT_COUNT = 6;

    private final PeopleService peopleService;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public HomeDto home(Long viewerId) {
        boolean profileIncomplete = profileRepository.findById(viewerId)
                .map(p -> p.getBio() == null || p.getBio().isBlank() || p.getInterests().length == 0)
                .orElse(true);
        long newThisWeek = userRepository.countByCreatedAtAfter(Instant.now().minus(Duration.ofDays(7)));
        return new HomeDto(userRepository.count(), newThisWeek, profileIncomplete,
                peopleService.suggestions(viewerId, SUGGESTION_COUNT),
                eventService.upcoming(viewerId, true, null, EVENT_COUNT).items(),
                eventService.upcoming(viewerId, false, null, EVENT_COUNT).items());
    }

}
