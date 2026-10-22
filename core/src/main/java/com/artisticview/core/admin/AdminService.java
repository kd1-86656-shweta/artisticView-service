package com.artisticview.core.admin;

import com.artisticview.common.dto.AdminUserDto;
import com.artisticview.common.dto.PlatformStatsDto;
import com.artisticview.common.enums.ReportStatus;
import com.artisticview.common.enums.UserStatus;
import com.artisticview.common.exception.AppException;
import com.artisticview.persistence.auth.RefreshTokenRepository;
import com.artisticview.persistence.moderation.ReportRepository;
import com.artisticview.persistence.rbac.Role;
import com.artisticview.persistence.rbac.RoleRepository;
import com.artisticview.persistence.rbac.UserRole;
import com.artisticview.persistence.rbac.UserRoleRepository;
import com.artisticview.persistence.user.Profile;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public PlatformStatsDto stats() {
        return new PlatformStatsDto(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.SUSPENDED),
                userRepository.countByCreatedAtAfter(Instant.now().minus(Duration.ofDays(7))),
                reportRepository.countByStatus(ReportStatus.OPEN));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> users(String q, UserStatus status, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        Page<User> users = userRepository.adminSearch(query, status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id")));
        List<Long> ids = users.map(User::getId).getContent();
        Map<Long, String> names = profileRepository.findAllByUserIdIn(ids).stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> Objects.toString(p.getDisplayName(), "")));
        Map<Long, List<String>> roles = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : userRoleRepository.findRoleNamesFor(ids)) {
                roles.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add((String) row[1]);
            }
        }
        return users.map(u -> new AdminUserDto(u.getId(), u.getEmail(), u.getUsername(), names.get(u.getId()),
                roles.getOrDefault(u.getId(), List.of()), u.getStatus(), u.isEmailVerified(), u.getCreatedAt()));
    }

    /** Suspending also ends every session of that user. A super admin cannot suspend themselves. */
    @Transactional
    public void updateStatus(Long actorId, Long userId, UserStatus status) {
        if (Objects.equals(actorId, userId)) {
            throw AppException.badRequest("You cannot change your own status");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> AppException.notFound("User"));
        user.setStatus(status);
        if (status != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        }
    }

    /**
     * Replaces a user's roles. New roles apply from the user's next token refresh (at most one access-token
     * lifetime). A super admin cannot remove their own SUPER_ADMIN role, so the platform never loses its last one.
     */
    @Transactional
    public void replaceRoles(Long actorId, Long userId, List<String> roleNames) {
        userRepository.findById(userId).orElseThrow(() -> AppException.notFound("User"));
        Map<String, Role> byName = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getInternalName, Function.identity()));
        List<Role> roles = roleNames.stream().distinct().map(name -> {
            Role role = byName.get(name.toUpperCase(Locale.ROOT));
            if (role == null) {
                throw AppException.badRequest("Unknown role: " + name);
            }
            return role;
        }).toList();
        if (Objects.equals(actorId, userId) && roles.stream().noneMatch(r -> Role.SUPER_ADMIN.equals(r.getInternalName()))) {
            throw AppException.badRequest("You cannot remove your own super admin role");
        }
        userRoleRepository.deleteAllForUser(userId);
        userRoleRepository.flush();
        roles.forEach(role -> userRoleRepository.save(new UserRole(userId, role.getId())));
    }
}
