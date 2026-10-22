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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** One read/write/delete combination on a sub-feature; roles are granted these rows. */
@Entity
@Table(name = "permission_sub_features")
@Getter
@Setter
@NoArgsConstructor
public class PermissionSubFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_feature_id", nullable = false)
    private Long subFeatureId;

    @Column(name = "can_read", nullable = false)
    private boolean canRead;

    @Column(name = "can_write", nullable = false)
    private boolean canWrite;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
