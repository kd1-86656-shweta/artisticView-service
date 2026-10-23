package com.artisticview.core.permission;

import com.artisticview.security.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Platform moderators (CONTENT_MODERATE) may remove any post, comment or community. */
@Component
@RequiredArgsConstructor
public class StaffCheck {

    private final AccessService accessService;

    public boolean canModerateContent() {
        return CurrentUser.get()
                .map(u -> accessService.permissionsFor(u.roleIds()).contains("CONTENT_MODERATE"))
                .orElse(false);
    }
}
