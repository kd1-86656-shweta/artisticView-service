package com.artisticview.persistence.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, EventRsvpId> {

    /** Returns 1 if a new RSVP was created, 0 if the viewer was already going. */
    @Modifying
    @Query(value = "INSERT INTO event_rsvps (event_id, user_id) VALUES (:eventId, :userId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIgnore(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EventRsvp r WHERE r.id.eventId = :eventId AND r.id.userId = :userId")
    int deleteRsvp(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT r.id.eventId FROM EventRsvp r WHERE r.id.userId = :userId AND r.id.eventId IN :eventIds")
    List<Long> findGoingAmong(@Param("userId") Long userId, @Param("eventIds") Collection<Long> eventIds);
}
