package com.artisticview.persistence.notification;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventIdAndRecipientId(Long eventId, Long recipientId);

    @Query("""
            SELECT n FROM Notification n WHERE n.recipientId = :recipientId
              AND (n.createdAt < :ts OR (n.createdAt = :ts AND n.id < :id))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findPage(@Param("recipientId") Long recipientId, @Param("ts") Instant ts,
                                @Param("id") long id, Limit limit);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n SET n.readAt = :now
            WHERE n.recipientId = :recipientId AND n.id IN :ids AND n.readAt IS NULL
            """)
    int markRead(@Param("recipientId") Long recipientId, @Param("ids") Collection<Long> ids,
                 @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientId = :recipientId AND n.readAt IS NULL")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);
}
