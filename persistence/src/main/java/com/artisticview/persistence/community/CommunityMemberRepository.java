package com.artisticview.persistence.community;

import com.artisticview.common.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMemberId> {

    /** Owners first, then moderators, then members by join date. */
    @Query("""
            SELECT m FROM CommunityMember m WHERE m.id.communityId = :communityId
            ORDER BY CASE m.role WHEN com.artisticview.common.enums.CommunityRole.OWNER THEN 0
                                 WHEN com.artisticview.common.enums.CommunityRole.MODERATOR THEN 1 ELSE 2 END,
                     m.createdAt
            """)
    List<CommunityMember> findByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT m FROM CommunityMember m WHERE m.id.userId = :userId AND m.status = :status ORDER BY m.createdAt DESC")
    List<CommunityMember> findByUserAndStatus(@Param("userId") Long userId, @Param("status") MembershipStatus status);

    @Query("SELECT m FROM CommunityMember m WHERE m.id.userId = :userId AND m.id.communityId IN :communityIds")
    List<CommunityMember> findForUserIn(@Param("userId") Long userId, @Param("communityIds") Collection<Long> communityIds);
}
