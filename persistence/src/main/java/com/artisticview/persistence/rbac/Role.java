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

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String MODERATOR = "MODERATOR";
    public static final String MEMBER = "MEMBER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "internal_name", nullable = false, unique = true)
    private String internalName;

    private String description;

    /** Seeded roles cannot be deleted. */
    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
