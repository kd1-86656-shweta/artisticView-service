package com.artisticview.persistence.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.Id> {

    @Query("SELECT m FROM ConversationMember m WHERE m.id.conversationId IN :ids ORDER BY m.joinedAt")
    List<ConversationMember> findByConversationIds(@Param("ids") Collection<Long> conversationIds);

    @Query("SELECT m.id.userId FROM ConversationMember m WHERE m.id.conversationId = :id")
    List<Long> findUserIds(@Param("id") Long conversationId);

    @Query("SELECT COUNT(m) FROM ConversationMember m WHERE m.id.conversationId = :id")
    long countMembers(@Param("id") Long conversationId);
}
