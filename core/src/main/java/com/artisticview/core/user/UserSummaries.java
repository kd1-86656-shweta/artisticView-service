package com.artisticview.core.user;

import com.artisticview.common.dto.UserSummaryDto;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.persistence.user.Profile;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Name + avatar for a batch of users in two queries; used wherever lists show who did something. */
@Component
@RequiredArgsConstructor
public class UserSummaries {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final MediaUrls mediaUrls;

    public Map<Long, UserSummaryDto> of(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Profile> profiles = profileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> {
            Profile p = profiles.get(u.getId());
            return new UserSummaryDto(u.getId(), u.getUsername(),
                    p == null || p.getDisplayName() == null ? u.getUsername() : p.getDisplayName(),
                    p == null ? null : mediaUrls.url(p.getAvatarKey()));
        }));
    }
}
