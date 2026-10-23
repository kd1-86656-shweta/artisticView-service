package com.artisticview.persistence.community;

import com.artisticview.common.enums.CommunityRole;
import com.artisticview.common.enums.MembershipStatus;
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
@Table(name = "community_members")
@Getter
@Setter
@NoArgsConstructor
public class CommunityMember {

    @EmbeddedId
    private CommunityMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityRole role = CommunityRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "invited_by")
    private Long invitedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CommunityMember(Long communityId, Long userId, CommunityRole role, MembershipStatus status, Long invitedBy) {
        this.id = new CommunityMemberId(communityId, userId);
        this.role = role;
        this.status = status;
        this.invitedBy = invitedBy;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }
}
