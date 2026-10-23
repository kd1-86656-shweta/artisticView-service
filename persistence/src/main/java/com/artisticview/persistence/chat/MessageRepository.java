package com.artisticview.persistence.chat;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Messages older than {@code beforeId}, newest first (the client reverses them for display). */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :id AND m.id < :beforeId ORDER BY m.id DESC")
    List<Message> findPage(@Param("id") Long conversationId, @Param("beforeId") long beforeId, Limit limit);

    /** The newest message of each of the given conversations. */
    @Query(value = """
            SELECT DISTINCT ON (conversation_id) * FROM messages
            WHERE conversation_id IN (:ids)
            ORDER BY conversation_id, id DESC
            """, nativeQuery = true)
    List<Message> findLatest(@Param("ids") Collection<Long> conversationIds);

    /** Unread count per conversation: messages from others after the member's read position. */
    @Query(value = """
            SELECT m.conversation_id AS conversationId, COUNT(*) AS unread
            FROM messages m
            JOIN conversation_members cm ON cm.conversation_id = m.conversation_id AND cm.user_id = :userId
            WHERE m.conversation_id IN (:ids)
              AND m.id > COALESCE(cm.last_read_message_id, 0)
              AND m.sender_id IS NOT NULL AND m.sender_id <> :userId
              AND m.deleted = FALSE
            GROUP BY m.conversation_id
            """, nativeQuery = true)
    List<UnreadRow> countUnread(@Param("userId") Long userId, @Param("ids") Collection<Long> conversationIds);

    interface UnreadRow {
        Long getConversationId();

        Long getUnread();
    }
}
