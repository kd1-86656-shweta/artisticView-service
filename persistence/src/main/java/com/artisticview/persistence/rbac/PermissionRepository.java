package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findAllByNameIn(Collection<String> names);

    @Query("""
            SELECT p.name FROM Permission p, RolePermission rp
            WHERE rp.permissionId = p.id AND rp.roleId = :roleId ORDER BY p.name
            """)
    List<String> findNamesForRole(@Param("roleId") Long roleId);

    /** Rows of [roleId, permissionName] for every role; used to warm the permission cache. */
    @Query("SELECT rp.roleId, p.name FROM Permission p, RolePermission rp WHERE rp.permissionId = p.id")
    List<Object[]> findAllRolePermissionNames();
}
