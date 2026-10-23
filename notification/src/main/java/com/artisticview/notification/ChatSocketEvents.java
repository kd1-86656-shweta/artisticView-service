package com.artisticview.notification;

import com.artisticview.core.chat.ChatPush;
import com.artisticview.core.chat.ChatService;
import com.artisticview.core.chat.PresenceTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/** Delivers chat events to members, keeps presence up to date and relays typing indicators. */
@Controller
@RequiredArgsConstructor
public class ChatSocketEvents {

    static final String QUEUE = "/queue/chat";

    private final SimpMessagingTemplate messaging;
    private final PresenceTracker presence;
    private final ChatService chatService;

    /** After the transaction commits, so a rolled-back message is never announced. */
    @TransactionalEventListener(fallbackExecution = true)
    public void deliver(ChatPush push) {
        for (Long userId : push.recipientIds()) {
            messaging.convertAndSendToUser(String.valueOf(userId), QUEUE, push.event());
        }
    }

    /** Client sends to /app/chats/{id}/typing while the user types (throttled on the client). */
    @MessageMapping("/chats/{id}/typing")
    public void typing(@DestinationVariable Long id, Principal principal) {
        chatService.typing(id, Long.valueOf(principal.getName()));
    }

    @EventListener
    public void connected(SessionConnectedEvent event) {
        if (event.getUser() != null) presence.connected(Long.valueOf(event.getUser().getName()));
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        if (event.getUser() != null) presence.disconnected(Long.valueOf(event.getUser().getName()));
    }
}
