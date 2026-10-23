package com.artisticview.persistence.community;

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
public class CommunityMemberId implements Serializable {

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "user_id")
    private Long userId;
}
