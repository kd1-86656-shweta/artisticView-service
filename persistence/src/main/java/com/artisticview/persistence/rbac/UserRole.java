package com.artisticview.persistence.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public UserRole(Long userId, Long roleId) {
        this.id = new UserRoleId(userId, roleId);
    }
}
