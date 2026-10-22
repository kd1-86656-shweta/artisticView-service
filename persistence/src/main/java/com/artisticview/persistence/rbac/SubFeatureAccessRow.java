package com.artisticview.persistence.rbac;

/** Projection for {@link SubFeatureRepository#findAccessForRoles}. */
public interface SubFeatureAccessRow {

    Long getFeatureId();

    String getFeatureKey();

    String getFeatureName();

    String getFeatureDisplayName();

    String getFeatureDescription();

    String getFeatureIcon();

    String getFeatureRoute();

    Integer getFeatureSequence();

    Long getSubFeatureId();

    String getSubFeatureKey();

    String getSubFeatureDisplayName();

    String getSubFeatureDescription();

    String getSubFeatureRoute();

    Integer getSubFeatureSequence();

    Boolean getCanRead();

    Boolean getCanWrite();

    Boolean getCanDelete();
}
