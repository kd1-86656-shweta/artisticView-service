package com.artisticview.core.user;

import com.artisticview.common.dto.MeDto;
import com.artisticview.common.dto.SessionDto;
import com.artisticview.common.exception.AppException;
import com.artisticview.core.permission.AccessService;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.persistence.rbac.UserRoleRepository;
import com.artisticview.persistence.user.Profile;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final AccessService accessService;
    private final MediaUrls mediaUrls;

    @Transactional(readOnly = true)
    public SessionDto currentSession(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> AppException.unauthorized("Login required"));
        return buildSession(user, userRoleRepository.findRoleIds(userId));
    }

    public SessionDto buildSession(User user, List<Long> roleIds) {
        Profile profile = profileRepository.findById(user.getId()).orElse(null);
        MeDto me = new MeDto(user.getId(), user.getEmail(), user.getUsername(),
                profile == null ? user.getUsername() : profile.getDisplayName(),
                profile == null ? null : mediaUrls.url(profile.getAvatarKey()),
                userRoleRepository.findRoleNames(user.getId()), user.isEmailVerified());
        return new SessionDto(me, accessService.permissionsFor(roleIds), accessService.featuresFor(roleIds));
    }
}
