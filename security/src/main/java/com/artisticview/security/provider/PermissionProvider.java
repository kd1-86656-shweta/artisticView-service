package com.artisticview.security.provider;

import java.util.Set;

/** Supplies the privilege names granted to a role. Implemented by the Redis-backed permission cache. */
public interface PermissionProvider {

    Set<String> getPermissionsForRole(Long roleId);
}
