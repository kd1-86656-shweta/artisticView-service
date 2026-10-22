package com.artisticview.persistence.event;

import com.artisticview.common.enums.CollaboratorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "event_collaborators")
@Getter
@Setter
@NoArgsConstructor
public class EventCollaborator {

    @EmbeddedId
    private EventCollaboratorId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaboratorStatus status = CollaboratorStatus.INVITED;

    @Column(name = "invited_by", nullable = false, updatable = false)
    private Long invitedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EventCollaborator(Long eventId, Long userId, Long invitedBy) {
        this.id = new EventCollaboratorId(eventId, userId);
        this.invitedBy = invitedBy;
    }
}
