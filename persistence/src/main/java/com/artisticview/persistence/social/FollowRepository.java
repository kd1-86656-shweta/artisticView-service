package com.artisticview.persistence.social;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /** Returns 1 if a new follow was created, 0 if it already existed. */
    @Modifying
    @Query(value = """
            INSERT INTO follows (follower_id, followee_id) VALUES (:followerId, :followeeId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.id.followerId = :followerId AND f.id.followeeId = :followeeId")
    int deleteFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Query("SELECT f.id.followeeId FROM Follow f WHERE f.id.followerId = :followerId AND f.id.followeeId IN :ids")
    List<Long> findFolloweeIdsAmong(@Param("followerId") Long followerId, @Param("ids") Collection<Long> ids);

    @Query("""
            SELECT f FROM Follow f WHERE f.id.followeeId = :userId
              AND (f.createdAt < :ts OR (f.createdAt = :ts AND f.id.followerId < :id))
            ORDER BY f.createdAt DESC, f.id.followerId DESC
            """)
    List<Follow> findFollowers(@Param("userId") Long userId, @Param("ts") Instant ts, @Param("id") long id,
                               Limit limit);

    @Query("""
            SELECT f FROM Follow f WHERE f.id.followerId = :userId
              AND (f.createdAt < :ts OR (f.createdAt = :ts AND f.id.followeeId < :id))
            ORDER BY f.createdAt DESC, f.id.followeeId DESC
            """)
    List<Follow> findFollowing(@Param("userId") Long userId, @Param("ts") Instant ts, @Param("id") long id,
                               Limit limit);
}
