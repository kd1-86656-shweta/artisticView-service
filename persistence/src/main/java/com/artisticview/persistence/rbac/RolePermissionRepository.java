package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
    int deleteAllForRole(@Param("roleId") Long roleId);
}
