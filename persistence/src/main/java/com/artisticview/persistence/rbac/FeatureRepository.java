package com.artisticview.persistence.rbac;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureRepository extends JpaRepository<Feature, Long> {

    List<Feature> findAllByOrderBySequenceNumberAsc();
}
