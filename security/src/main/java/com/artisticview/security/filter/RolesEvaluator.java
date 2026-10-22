package com.artisticview.security.filter;

import com.artisticview.security.auth.AuthUser;
import com.artisticview.security.provider.PermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Backs {@code @PreAuthorize("hasPermission('FEATURE', 'PRIVILEGE')")}, as in agasty-service: the user passes
 * when any of their roles holds the privilege. The first argument names the feature and documents intent.
 */
@Component
@RequiredArgsConstructor
public class RolesEvaluator implements PermissionEvaluator {

    private final PermissionProvider permissionProvider;

    @Override
    public boolean hasPermission(Authentication authentication, Object feature, Object privilege) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)
                || !(privilege instanceof String privilegeName)) {
            return false;
        }
        return user.roleIds().stream()
                .anyMatch(roleId -> permissionProvider.getPermissionsForRole(roleId).contains(privilegeName));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        return false;
    }
}
