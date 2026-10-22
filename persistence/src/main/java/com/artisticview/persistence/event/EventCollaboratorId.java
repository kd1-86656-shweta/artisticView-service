package com.artisticview.persistence.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventCollaboratorId implements Serializable {

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "user_id")
    private Long userId;
}
