package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleSubFeaturePermissionRepository extends JpaRepository<RoleSubFeaturePermission, Long> {

    @Modifying
    @Query("DELETE FROM RoleSubFeaturePermission r WHERE r.roleId = :roleId")
    int deleteAllForRole(@Param("roleId") Long roleId);
}
