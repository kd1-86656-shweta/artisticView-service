package com.artisticview.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Locks a batch of unpublished events; concurrent relays on other pods skip the locked rows. */
    @Query(value = """
            SELECT * FROM outbox_events WHERE published_at IS NULL
            ORDER BY id LIMIT :batchSize FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockUnpublished(@Param("batchSize") int batchSize);
}
