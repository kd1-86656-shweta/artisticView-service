package com.artisticview.persistence.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Optional<Community> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Discover: public communities, optionally matching a search, biggest first. */
    @Query(value = """
            SELECT c.* FROM communities c
            WHERE c.visibility = 'PUBLIC'
              AND (CAST(:q AS text) IS NULL OR c.name ILIKE '%' || CAST(:q AS text) || '%'
                                            OR c.description ILIKE '%' || CAST(:q AS text) || '%')
            ORDER BY c.member_count DESC, c.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Community> discover(@Param("q") String q, @Param("limit") int limit, @Param("offset") int offset);

    /** Communities the user is an active member of, most recently joined first. */
    @Query(value = """
            SELECT c.* FROM communities c
            JOIN community_members m ON m.community_id = c.id
            WHERE m.user_id = :userId AND m.status = 'ACTIVE'
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<Community> findJoined(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE communities SET member_count = GREATEST(member_count + :delta, 0) WHERE id = :id", nativeQuery = true)
    int adjustMembers(@Param("id") Long id, @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE communities SET post_count = GREATEST(post_count + :delta, 0) WHERE id = :id", nativeQuery = true)
    int adjustPosts(@Param("id") Long id, @Param("delta") long delta);
}
