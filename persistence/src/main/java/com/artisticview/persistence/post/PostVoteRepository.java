package com.artisticview.persistence.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostVoteRepository extends JpaRepository<PostVote, PostVote.Id> {

    /** The viewer's current vote (+1 / -1), or null. */
    @Query(value = "SELECT value FROM post_votes WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
    Short findValue(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO post_votes (post_id, user_id, value) VALUES (:postId, :userId, :value)
            ON CONFLICT (post_id, user_id) DO UPDATE SET value = EXCLUDED.value
            """, nativeQuery = true)
    int upsert(@Param("postId") Long postId, @Param("userId") Long userId, @Param("value") short value);

    @Modifying
    @Query(value = "DELETE FROM post_votes WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
    int remove(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("SELECT v FROM PostVote v WHERE v.id.userId = :userId AND v.id.postId IN :postIds")
    List<PostVote> findForUserIn(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
