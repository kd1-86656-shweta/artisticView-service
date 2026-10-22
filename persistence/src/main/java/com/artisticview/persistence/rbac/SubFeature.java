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

/** A screen or action inside a feature; access to it is granted per role as read/write/delete. */
@Entity
@Table(name = "sub_features")
@Getter
@Setter
@NoArgsConstructor
public class SubFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    private String description;

    private String route;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
