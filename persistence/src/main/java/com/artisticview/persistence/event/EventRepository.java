package com.artisticview.persistence.event;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Scheduled events that start after {@code since}, soonest first, skipping hosts the viewer has blocked or been
     * blocked by. With {@code promotedOnly}, only events whose promotion is still running. Keyset on (starts_at, id).
     */
    @Query(value = """
            SELECT e.* FROM events e
            WHERE e.status = 'SCHEDULED'
              AND e.starts_at >= :since
              AND (e.starts_at > :ts OR (e.starts_at = :ts AND e.id > :id))
              AND (:promotedOnly = FALSE OR e.promoted_until > now())
              AND NOT EXISTS (SELECT 1 FROM blocks b
                              WHERE (b.blocker_id = :viewerId AND b.blocked_id = e.host_id)
                                 OR (b.blocker_id = e.host_id AND b.blocked_id = :viewerId))
            ORDER BY e.starts_at, e.id
            LIMIT :limit
            """, nativeQuery = true)
    List<Event> findUpcoming(@Param("viewerId") Long viewerId, @Param("since") Instant since,
                             @Param("ts") Instant ts, @Param("id") long id,
                             @Param("promotedOnly") boolean promotedOnly, @Param("limit") int limit);

    /** Events the user hosts or co-hosts (accepted), latest start first. Keyset on (starts_at, id) descending. */
    @Query("""
            SELECT e FROM Event e
            WHERE (e.hostId = :userId OR EXISTS (SELECT 1 FROM EventCollaborator c
                                                 WHERE c.id.eventId = e.id AND c.id.userId = :userId
                                                   AND c.status = com.artisticview.common.enums.CollaboratorStatus.ACCEPTED))
              AND (e.startsAt < :ts OR (e.startsAt = :ts AND e.id < :id))
            ORDER BY e.startsAt DESC, e.id DESC
            """)
    List<Event> findManagedBy(@Param("userId") Long userId, @Param("ts") Instant ts, @Param("id") long id, Limit limit);

    /** Takes a seat only while there is room. Returns 0 when the event is full. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE events SET rsvp_count = rsvp_count + 1
            WHERE id = :id AND (capacity IS NULL OR rsvp_count < capacity)
            """, nativeQuery = true)
    int takeSeat(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE events SET rsvp_count = GREATEST(rsvp_count - 1, 0) WHERE id = :id", nativeQuery = true)
    int releaseSeat(@Param("id") Long id);
}
