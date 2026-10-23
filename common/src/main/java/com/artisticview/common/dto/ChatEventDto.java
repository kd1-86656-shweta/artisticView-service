package com.artisticview.common.dto;

/**
 * Pushed live to chat members over the WebSocket (/user/queue/chat).
 * <ul>
 *   <li>MESSAGE: {@code message} is new</li>
 *   <li>MESSAGE_DELETED: {@code message} was deleted</li>
 *   <li>READ: {@code userId} has read up to {@code messageId}</li>
 *   <li>TYPING: {@code userId} is typing (clients hide it after a few seconds)</li>
 *   <li>CHAT_UPDATED: members, title or picture changed; refetch the chat</li>
 *   <li>REMOVED: the recipient is no longer in the chat</li>
 * </ul>
 */
public record ChatEventDto(String type, Long conversationId, MessageDto message, Long userId, Long messageId) {

    public static ChatEventDto message(MessageDto m) {
        return new ChatEventDto("MESSAGE", m.conversationId(), m, null, null);
    }

    public static ChatEventDto deleted(MessageDto m) {
        return new ChatEventDto("MESSAGE_DELETED", m.conversationId(), m, null, null);
    }

    public static ChatEventDto read(Long conversationId, Long userId, Long messageId) {
        return new ChatEventDto("READ", conversationId, null, userId, messageId);
    }

    public static ChatEventDto typing(Long conversationId, Long userId) {
        return new ChatEventDto("TYPING", conversationId, null, userId, null);
    }

    public static ChatEventDto updated(Long conversationId) {
        return new ChatEventDto("CHAT_UPDATED", conversationId, null, null, null);
    }

    public static ChatEventDto removed(Long conversationId) {
        return new ChatEventDto("REMOVED", conversationId, null, null, null);
    }
}
