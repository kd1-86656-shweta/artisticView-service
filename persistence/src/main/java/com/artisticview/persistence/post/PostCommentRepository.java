package com.artisticview.persistence.post;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    /** Every comment on a post in posting order; the client builds the reply tree. */
    @Query("SELECT c FROM PostComment c WHERE c.postId = :postId ORDER BY c.createdAt, c.id")
    List<PostComment> findThread(@Param("postId") Long postId, Limit limit);
}
