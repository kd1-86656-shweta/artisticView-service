package com.artisticview.persistence.social;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
