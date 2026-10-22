package com.artisticview.core.permission;

import com.artisticview.common.dto.FeatureAccessDto;
import com.artisticview.common.dto.SubFeatureAccessDto;
import com.artisticview.persistence.rbac.SubFeatureAccessRow;
import com.artisticview.persistence.rbac.SubFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves what a set of roles may do: privilege names and the feature/sub-feature tree for the UI. */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final PermissionCacheService permissionCache;
    private final SubFeatureRepository subFeatureRepository;

    public Set<String> permissionsFor(Collection<Long> roleIds) {
        Set<String> names = new LinkedHashSet<>();
        roleIds.forEach(roleId -> names.addAll(permissionCache.getPermissionsForRole(roleId)));
        return names;
    }

    public List<FeatureAccessDto> featuresFor(Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        Map<Long, FeatureBuilder> features = new LinkedHashMap<>();
        for (SubFeatureAccessRow row : subFeatureRepository.findAccessForRoles(roleIds)) {
            if (!Boolean.TRUE.equals(row.getCanRead())) {
                continue;
            }
            features.computeIfAbsent(row.getFeatureId(), id -> new FeatureBuilder(row)).subFeatures.add(
                    new SubFeatureAccessDto(row.getSubFeatureId(), row.getSubFeatureKey(),
                            row.getSubFeatureDisplayName(), row.getSubFeatureDescription(), row.getSubFeatureRoute(),
                            row.getSubFeatureSequence(), true, Boolean.TRUE.equals(row.getCanWrite()),
                            Boolean.TRUE.equals(row.getCanDelete())));
        }
        return features.values().stream().map(FeatureBuilder::build).toList();
    }

    private static final class FeatureBuilder {
        private final SubFeatureAccessRow first;
        private final List<SubFeatureAccessDto> subFeatures = new ArrayList<>();

        private FeatureBuilder(SubFeatureAccessRow first) {
            this.first = first;
        }

        private FeatureAccessDto build() {
            return new FeatureAccessDto(first.getFeatureId(), first.getFeatureKey(), first.getFeatureName(),
                    first.getFeatureDisplayName(), first.getFeatureDescription(), first.getFeatureIcon(),
                    first.getFeatureRoute(), first.getFeatureSequence(), subFeatures);
        }
    }
}
