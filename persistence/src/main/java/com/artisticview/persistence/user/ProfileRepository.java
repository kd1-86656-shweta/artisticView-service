package com.artisticview.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findAllByUserIdIn(Collection<Long> userIds);

    /**
     * Applies a follow (+1) or unfollow (-1) to both sides in one atomic statement:
     * the follower's following_count and the followee's follower_count.
     */
    @Modifying
    @Query(value = """
            UPDATE profiles
            SET following_count = GREATEST(following_count + CASE WHEN user_id = :followerId THEN :delta ELSE 0 END, 0),
                follower_count  = GREATEST(follower_count  + CASE WHEN user_id = :followeeId THEN :delta ELSE 0 END, 0)
            WHERE user_id IN (:followerId, :followeeId)
            """, nativeQuery = true)
    int adjustFollowCounts(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId,
                           @Param("delta") long delta);

    /**
     * People the viewer might like to meet: active, not already followed, no block either way.
     * Shared interests first, then the newest members.
     */
    @Query(value = """
            SELECT p.user_id AS userId, u.username AS username, p.display_name AS displayName,
                   p.avatar_key AS avatarKey, p.bio AS bio, array_to_string(p.interests, ',') AS interestsCsv,
                   p.follower_count AS followerCount
            FROM profiles p
            JOIN users u ON u.id = p.user_id
            WHERE u.status = 'ACTIVE'
              AND p.user_id <> :viewerId
              AND NOT EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = :viewerId AND f.followee_id = p.user_id)
              AND NOT EXISTS (SELECT 1 FROM blocks b
                              WHERE (b.blocker_id = :viewerId AND b.blocked_id = p.user_id)
                                 OR (b.blocker_id = p.user_id AND b.blocked_id = :viewerId))
            ORDER BY (p.interests && (SELECT me.interests FROM profiles me WHERE me.user_id = :viewerId)) DESC,
                     u.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SuggestionRow> findSuggestions(@Param("viewerId") Long viewerId, @Param("limit") int limit);

    interface SuggestionRow {
        Long getUserId();

        String getUsername();

        String getDisplayName();

        String getAvatarKey();

        String getBio();

        String getInterestsCsv();

        Long getFollowerCount();
    }
}
