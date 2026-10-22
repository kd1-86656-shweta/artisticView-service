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

/** A top-level area of the app (a menu entry / home card). */
@Entity
@Table(name = "features")
@Getter
@Setter
@NoArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    @Column(name = "display_name")
    private String displayName;

    private String description;

    private String icon;

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
