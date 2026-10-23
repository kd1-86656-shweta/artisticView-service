package com.artisticview.persistence.event;

import com.artisticview.common.enums.EventLocationType;
import com.artisticview.common.enums.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_id", nullable = false, updatable = false)
    private Long hostId;

    @Column(nullable = false)
    private String title;

    private String description;

    private String category;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private EventLocationType locationType;

    private String location;

    @Column(name = "meeting_url")
    private String meetingUrl;

    private Integer capacity;

    @Column(name = "cover_key")
    private String coverKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.SCHEDULED;

    @Column(name = "promoted_until")
    private Instant promotedUntil;

    /** Maintained by atomic UPDATEs in {@link EventRepository}; never written through the entity. */
    @Column(name = "rsvp_count", nullable = false, insertable = false, updatable = false)
    private long rsvpCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isPromotedAt(Instant now) {
        return promotedUntil != null && promotedUntil.isAfter(now);
    }
}
