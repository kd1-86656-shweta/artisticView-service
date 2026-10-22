package com.artisticview.persistence.event;

import com.artisticview.common.enums.CollaboratorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventCollaboratorRepository extends JpaRepository<EventCollaborator, EventCollaboratorId> {

    @Query("SELECT c FROM EventCollaborator c WHERE c.id.eventId IN :eventIds ORDER BY c.createdAt")
    List<EventCollaborator> findByEventIds(@Param("eventIds") Collection<Long> eventIds);

    @Query("SELECT c FROM EventCollaborator c WHERE c.id.userId = :userId AND c.status = :status ORDER BY c.createdAt DESC")
    List<EventCollaborator> findByUserAndStatus(@Param("userId") Long userId, @Param("status") CollaboratorStatus status);

    @Query("""
            SELECT COUNT(c) FROM EventCollaborator c
            WHERE c.id.eventId = :eventId AND c.status <> com.artisticview.common.enums.CollaboratorStatus.DECLINED
            """)
    long countActive(@Param("eventId") Long eventId);
}
