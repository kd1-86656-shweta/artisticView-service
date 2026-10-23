package com.artisticview.persistence.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByDirectKey(String directKey);

    /** The user's chats, most recently active first. */
    @Query(value = """
            SELECT c.* FROM conversations c
            JOIN conversation_members m ON m.conversation_id = c.id
            WHERE m.user_id = :userId
            ORDER BY COALESCE(c.last_message_at, c.created_at) DESC, c.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Conversation> findForUser(@Param("userId") Long userId, @Param("limit") int limit);
}
