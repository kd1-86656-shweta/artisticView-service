package com.artisticview.persistence.social;

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
public class BlockId implements Serializable {

    @Column(name = "blocker_id")
    private Long blockerId;

    @Column(name = "blocked_id")
    private Long blockedId;
}
