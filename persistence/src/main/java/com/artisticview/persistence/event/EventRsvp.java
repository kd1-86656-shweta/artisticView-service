package com.artisticview.persistence.event;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "event_rsvps")
@Getter
@NoArgsConstructor
public class EventRsvp {

    @EmbeddedId
    private EventRsvpId id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
