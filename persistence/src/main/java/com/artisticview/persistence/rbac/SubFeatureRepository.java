package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SubFeatureRepository extends JpaRepository<SubFeature, Long> {

    List<SubFeature> findAllByOrderByFeatureIdAscSequenceNumberAsc();

    /**
     * Effective access of a set of roles: each active sub-feature they can see, with read/write/delete OR-ed
     * across the roles. Ordered for direct rendering as menus.
     */
    @Query(value = """
            SELECT f.id AS featureId, f.name AS featureKey, f.feature_name AS featureName,
                   f.display_name AS featureDisplayName, f.description AS featureDescription, f.icon AS featureIcon,
                   f.route AS featureRoute, f.sequence_number AS featureSequence,
                   sf.id AS subFeatureId, sf.name AS subFeatureKey, sf.display_name AS subFeatureDisplayName,
                   sf.description AS subFeatureDescription, sf.route AS subFeatureRoute,
                   sf.sequence_number AS subFeatureSequence,
                   bool_or(psf.can_read) AS canRead, bool_or(psf.can_write) AS canWrite,
                   bool_or(psf.can_delete) AS canDelete
            FROM role_sub_features_permissions rsfp
            JOIN permission_sub_features psf ON psf.id = rsfp.permission_sub_feature_id
            JOIN sub_features sf ON sf.id = psf.sub_feature_id AND sf.active
            JOIN features f ON f.id = sf.feature_id AND f.active
            WHERE rsfp.role_id IN (:roleIds)
            GROUP BY f.id, sf.id
            ORDER BY f.sequence_number, sf.sequence_number
            """, nativeQuery = true)
    List<SubFeatureAccessRow> findAccessForRoles(@Param("roleIds") Collection<Long> roleIds);
}
