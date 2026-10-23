package com.artisticview.persistence.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * One listing query for every view: a community, a person's profile, or the viewer's feed (communities they
     * belong to plus people they follow). Posts in private communities only show to their members, and posts by
     * anyone the viewer has blocked (or been blocked by) never show.
     * Sort: "new" = newest; "top" = highest score; "hot" = score decayed by age, like Reddit's front page.
     */
    @Query(value = """
            SELECT p.* FROM posts p
            LEFT JOIN communities c ON c.id = p.community_id
            WHERE (CAST(:communityId AS bigint) IS NULL OR p.community_id = :communityId)
              AND (CAST(:authorId AS bigint) IS NULL OR p.author_id = :authorId)
              AND (:feed = FALSE
                   OR p.author_id = :viewerId
                   OR p.community_id IN (SELECT m.community_id FROM community_members m
                                          WHERE m.user_id = :viewerId AND m.status = 'ACTIVE')
                   OR (p.community_id IS NULL AND p.author_id IN (SELECT f.followee_id FROM follows f WHERE f.follower_id = :viewerId)))
              AND (c.id IS NULL OR c.visibility = 'PUBLIC'
                   OR EXISTS (SELECT 1 FROM community_members m
                              WHERE m.community_id = c.id AND m.user_id = :viewerId AND m.status = 'ACTIVE'))
              AND NOT EXISTS (SELECT 1 FROM blocks b
                              WHERE (b.blocker_id = :viewerId AND b.blocked_id = p.author_id)
                                 OR (b.blocker_id = p.author_id AND b.blocked_id = :viewerId))
            ORDER BY
              CASE WHEN :sort = 'top' THEN p.score END DESC,
              CASE WHEN :sort = 'hot' THEN (p.score + 1) / POWER(EXTRACT(EPOCH FROM (now() - p.created_at)) / 3600 + 2, 1.5) END DESC,
              p.created_at DESC, p.id DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Post> list(@Param("viewerId") Long viewerId, @Param("communityId") Long communityId,
                    @Param("authorId") Long authorId, @Param("feed") boolean feed,
                    @Param("sort") String sort, @Param("limit") int limit, @Param("offset") int offset);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE posts SET score = score + :delta WHERE id = :id", nativeQuery = true)
    int adjustScore(@Param("id") Long id, @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE posts SET comment_count = GREATEST(comment_count + :delta, 0) WHERE id = :id", nativeQuery = true)
    int adjustComments(@Param("id") Long id, @Param("delta") long delta);
}
