package com.artisticview.persistence.post;

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

import java.time.Instant;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    /** Null when the post is on the author's own profile. */
    @Column(name = "community_id", updatable = false)
    private Long communityId;

    @Column(nullable = false)
    private String title;

    private String body;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "video_key")
    private String videoKey;

    /** Counters are maintained by atomic UPDATEs in {@link PostRepository}, never through the entity. */
    @Column(nullable = false, insertable = false, updatable = false)
    private long score;

    @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
    private long commentCount;

    @Column(name = "edited_at")
    private Instant editedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
