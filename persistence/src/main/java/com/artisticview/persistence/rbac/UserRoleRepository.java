package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT ur.id.roleId FROM UserRole ur WHERE ur.id.userId = :userId ORDER BY ur.id.roleId")
    List<Long> findRoleIds(@Param("userId") Long userId);

    @Query("""
            SELECT r.internalName FROM UserRole ur, Role r
            WHERE r.id = ur.id.roleId AND ur.id.userId = :userId ORDER BY r.id
            """)
    List<String> findRoleNames(@Param("userId") Long userId);

    /** Rows of [userId, internalName] for a page of users. */
    @Query("""
            SELECT ur.id.userId, r.internalName FROM UserRole ur, Role r
            WHERE r.id = ur.id.roleId AND ur.id.userId IN :userIds ORDER BY r.id
            """)
    List<Object[]> findRoleNamesFor(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur, Role r WHERE r.id = ur.id.roleId AND r.internalName = :internalName")
    boolean anyUserHasRole(@Param("internalName") String internalName);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.id.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
