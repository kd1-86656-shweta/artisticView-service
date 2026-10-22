package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionSubFeatureRepository extends JpaRepository<PermissionSubFeature, Long> {

    Optional<PermissionSubFeature> findBySubFeatureIdAndCanReadAndCanWriteAndCanDelete(
            Long subFeatureId, boolean canRead, boolean canWrite, boolean canDelete);
}
