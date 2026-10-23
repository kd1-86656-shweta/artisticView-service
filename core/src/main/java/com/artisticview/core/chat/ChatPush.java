package com.artisticview.core.chat;

import com.artisticview.common.dto.ChatEventDto;

import java.util.Collection;

/**
 * Raised by {@link ChatService}; the WebSocket module delivers it to each recipient after the transaction commits,
 * so nobody is told about a message that was rolled back.
 */
public record ChatPush(Collection<Long> recipientIds, ChatEventDto event) {
}
