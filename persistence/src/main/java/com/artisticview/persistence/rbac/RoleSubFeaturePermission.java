package com.artisticview.persistence.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "role_sub_features_permissions")
@Getter
@Setter
@NoArgsConstructor
public class RoleSubFeaturePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_sub_feature_id", nullable = false)
    private Long permissionSubFeatureId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoleSubFeaturePermission(Long roleId, Long permissionSubFeatureId) {
        this.roleId = roleId;
        this.permissionSubFeatureId = permissionSubFeatureId;
    }
}
