package com.artisticview.core.user;

import com.artisticview.persistence.rbac.Role;
import com.artisticview.persistence.rbac.RoleRepository;
import com.artisticview.persistence.rbac.UserRole;
import com.artisticview.persistence.rbac.UserRoleRepository;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first super admin without any hard-coded password: sign up normally, set
 * {@code APP_BOOTSTRAP_SUPER_ADMIN_EMAIL} to that email and restart. Only runs while no super admin exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${app.bootstrap.super-admin-email:}")
    private String superAdminEmail;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void promote() {
        if (superAdminEmail.isBlank() || userRoleRepository.anyUserHasRole(Role.SUPER_ADMIN)) {
            return;
        }
        userRepository.findByEmail(superAdminEmail.trim().toLowerCase(Locale.ROOT)).ifPresentOrElse(user -> {
            Role role = roleRepository.findByInternalName(Role.SUPER_ADMIN).orElseThrow();
            userRoleRepository.save(new UserRole(user.getId(), role.getId()));
            log.info("Promoted {} to SUPER_ADMIN", user.getUsername());
        }, () -> log.warn("Super admin bootstrap: no user with email {} yet; sign up first", superAdminEmail));
    }
}
