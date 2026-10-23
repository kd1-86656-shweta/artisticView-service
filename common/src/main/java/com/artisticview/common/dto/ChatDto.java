package com.artisticview.common.dto;

import com.artisticview.common.enums.ChatRole;
import com.artisticview.common.enums.ConversationType;

import java.time.Instant;
import java.util.List;

/**
 * A chat as the viewer sees it. For DIRECT chats {@code title} and {@code avatarUrl} are the other person's.
 * Each member's {@code lastReadMessageId} drives the read ticks.
 */
public record ChatDto(Long id, ConversationType type, String title, String avatarUrl, List<Member> members,
                      MessageDto lastMessage, long unreadCount, ChatRole myRole, Instant createdAt) {

    public record Member(UserSummaryDto user, ChatRole role, boolean online, Long lastReadMessageId) {
    }
}
