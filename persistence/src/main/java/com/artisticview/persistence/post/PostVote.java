package com.artisticview.persistence.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "post_votes")
@Getter
@NoArgsConstructor
public class PostVote {

    @EmbeddedId
    private Id id;

    /** +1 upvote or -1 downvote. */
    @Column(nullable = false)
    private short value;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "post_id")
        private Long postId;

        @Column(name = "user_id")
        private Long userId;
    }
}
