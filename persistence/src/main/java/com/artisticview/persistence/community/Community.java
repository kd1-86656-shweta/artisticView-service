package com.artisticview.persistence.community;

import com.artisticview.common.enums.CommunityVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "communities")
@Getter
@Setter
@NoArgsConstructor
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String description;

    private String rules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityVisibility visibility = CommunityVisibility.PUBLIC;

    @Column(name = "cover_key")
    private String coverKey;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;

    /** Counters are maintained by atomic UPDATEs in {@link CommunityRepository}, never through the entity. */
    @Column(name = "member_count", nullable = false, insertable = false, updatable = false)
    private long memberCount;

    @Column(name = "post_count", nullable = false, insertable = false, updatable = false)
    private long postCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
