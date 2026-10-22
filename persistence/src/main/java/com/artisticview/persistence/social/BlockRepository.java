package com.artisticview.persistence.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

    @Modifying
    @Query(value = """
            INSERT INTO blocks (blocker_id, blocked_id) VALUES (:blockerId, :blockedId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Modifying
    @Query("DELETE FROM Block b WHERE b.id.blockerId = :blockerId AND b.id.blockedId = :blockedId")
    int deleteBlock(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /** True if either user has blocked the other. */
    @Query("""
            SELECT COUNT(b) > 0 FROM Block b
            WHERE (b.id.blockerId = :a AND b.id.blockedId = :b) OR (b.id.blockerId = :b AND b.id.blockedId = :a)
            """)
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT b.id.blockedId FROM Block b WHERE b.id.blockerId = :blockerId AND b.id.blockedId IN :ids")
    List<Long> findBlockedIdsAmong(@Param("blockerId") Long blockerId, @Param("ids") Collection<Long> ids);
}
