package com.artisticview.notification;

import com.artisticview.security.auth.AuthUser;
import com.artisticview.security.auth.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Authenticates the STOMP CONNECT frame with the same access token as the REST API, and only lets clients
 * subscribe to their own queues. The principal's name is the user id, which is what user destinations use.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Missing access token");
            }
            try {
                AuthUser user = jwtTokenProvider.toAuthUser(jwtTokenProvider.parse(header.substring(7)));
                accessor.setUser(new ChatPrincipal(user));
            } catch (JwtException | IllegalArgumentException e) {
                throw new MessagingException("Invalid or expired access token");
            }
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (accessor.getUser() == null || destination == null || !destination.startsWith("/user/queue/")) {
                throw new MessagingException("You can only subscribe to your own queues");
            }
        } else if (command == StompCommand.SEND && accessor.getUser() == null) {
            throw new MessagingException("Not connected");
        }
        return message;
    }

    /** STOMP principal; {@link #getName()} is the user id so convertAndSendToUser(userId, ...) reaches them. */
    public record ChatPrincipal(AuthUser user) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(user.userId());
        }
    }
}
