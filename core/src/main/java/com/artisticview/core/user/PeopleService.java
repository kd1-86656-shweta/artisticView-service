package com.artisticview.core.user;

import com.artisticview.common.dto.PersonCardDto;
import com.artisticview.common.dto.ProfileDto;
import com.artisticview.common.dto.UpdateProfileRequest;
import com.artisticview.common.enums.ProfileVisibility;
import com.artisticview.common.exception.AppException;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.persistence.social.BlockRepository;
import com.artisticview.persistence.social.FollowRepository;
import com.artisticview.persistence.user.Profile;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PeopleService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final MediaUrls mediaUrls;

    /** People to meet: shared interests first, then newest members. Skips people I follow or have blocked. */
    @Transactional(readOnly = true)
    public List<PersonCardDto> suggestions(Long viewerId, int limit) {
        return profileRepository.findSuggestions(viewerId, limit).stream()
                .map(p -> toCard(p, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(String username, Long viewerId) {
        User user = userRepository.findByUsername(username.toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> AppException.notFound("Profile"));
        boolean me = Objects.equals(user.getId(), viewerId);
        if (!me && viewerId != null && blockRepository.existsBetween(viewerId, user.getId())) {
            throw AppException.notFound("Profile");
        }
        Profile profile = profileRepository.findById(user.getId()).orElseThrow(() -> AppException.notFound("Profile"));
        boolean following = !me && viewerId != null
                && !followRepository.findFolloweeIdsAmong(viewerId, List.of(user.getId())).isEmpty();
        boolean restricted = !me && profile.getProfileVisibility() == ProfileVisibility.FOLLOWERS && !following;
        return new ProfileDto(user.getId(), user.getUsername(), profile.getDisplayName(),
                restricted ? null : profile.getBio(),
                mediaUrls.url(profile.getAvatarKey()), mediaUrls.url(profile.getCoverKey()),
                restricted ? List.of() : Arrays.asList(profile.getInterests()),
                restricted ? Map.of() : profile.getLinks(),
                profile.getProfileVisibility(), profile.getFollowerCount(), profile.getFollowingCount(),
                following, false, me, user.getCreatedAt());
    }

    @Transactional
    public ProfileDto updateMyProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(userId).orElseThrow(() -> AppException.notFound("Profile"));
        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio().trim());
        }
        if (request.interests() != null) {
            profile.setInterests(request.interests().stream()
                    .map(i -> i.trim().toLowerCase(Locale.ROOT))
                    .filter(i -> !i.isEmpty())
                    .distinct()
                    .toArray(String[]::new));
        }
        if (request.links() != null) {
            profile.setLinks(request.links());
        }
        if (request.profileVisibility() != null) {
            profile.setProfileVisibility(request.profileVisibility());
        }
        profileRepository.save(profile);
        User user = userRepository.findById(userId).orElseThrow();
        return profile(user.getUsername(), userId);
    }

    @Transactional
    public void follow(Long viewerId, Long targetId) {
        if (Objects.equals(viewerId, targetId)) {
            throw AppException.badRequest("You cannot follow yourself");
        }
        userRepository.findById(targetId).filter(User::isActive).orElseThrow(() -> AppException.notFound("User"));
        if (blockRepository.existsBetween(viewerId, targetId)) {
            throw AppException.forbidden("You cannot follow this person");
        }
        if (followRepository.insertIgnore(viewerId, targetId) == 1) {
            profileRepository.adjustFollowCounts(viewerId, targetId, 1);
        }
    }

    @Transactional
    public void unfollow(Long viewerId, Long targetId) {
        if (followRepository.deleteFollow(viewerId, targetId) == 1) {
            profileRepository.adjustFollowCounts(viewerId, targetId, -1);
        }
    }

    private PersonCardDto toCard(ProfileRepository.SuggestionRow row, boolean following) {
        return new PersonCardDto(row.getUserId(), row.getUsername(), row.getDisplayName(),
                mediaUrls.url(row.getAvatarKey()), row.getBio(),
                row.getInterestsCsv() == null || row.getInterestsCsv().isEmpty() ? List.of() : Arrays.asList(row.getInterestsCsv().split(",")),
                row.getFollowerCount() == null ? 0 : row.getFollowerCount(), following);
    }
}
