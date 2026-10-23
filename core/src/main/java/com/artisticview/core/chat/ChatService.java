package com.artisticview.core.chat;

import com.artisticview.common.dto.ChatDto;
import com.artisticview.common.dto.ChatEventDto;
import com.artisticview.common.dto.ChatRequests;
import com.artisticview.common.dto.MessageDto;
import com.artisticview.common.dto.UserSummaryDto;
import com.artisticview.common.enums.ChatRole;
import com.artisticview.common.enums.ConversationType;
import com.artisticview.common.enums.MediaKind;
import com.artisticview.common.exception.AppException;
import com.artisticview.common.util.Limits;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.core.user.UserSummaries;
import com.artisticview.persistence.chat.Conversation;
import com.artisticview.persistence.chat.ConversationMember;
import com.artisticview.persistence.chat.ConversationMemberRepository;
import com.artisticview.persistence.chat.ConversationRepository;
import com.artisticview.persistence.chat.Message;
import com.artisticview.persistence.chat.MessageRepository;
import com.artisticview.persistence.social.BlockRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WhatsApp-style chat: one-to-one chats (one per pair) and groups with admins. Every change is pushed live to
 * the chat's members through {@link ChatPush} events, delivered by the WebSocket module after commit.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CHATS = 100;
    private static final int MAX_GROUP_SIZE = 256;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final UserSummaries userSummaries;
    private final MediaUrls mediaUrls;
    private final PresenceTracker presence;
    private final ApplicationEventPublisher events;

    // ------------------------------------------------------------------ chats

    @Transactional(readOnly = true)
    public List<ChatDto> list(Long viewerId) {
        return toDtos(conversationRepository.findForUser(viewerId, MAX_CHATS), viewerId);
    }

    @Transactional(readOnly = true)
    public ChatDto get(Long chatId, Long viewerId) {
        Conversation c = load(chatId);
        requireMember(c, viewerId);
        return toDtos(List.of(c), viewerId).get(0);
    }

    /** Opens the one-to-one chat with this person, creating it the first time. */
    @Transactional
    public ChatDto direct(Long viewerId, String rawUsername) {
        User other = findUser(rawUsername);
        if (Objects.equals(other.getId(), viewerId)) {
            throw AppException.badRequest("You cannot message yourself");
        }
        if (blockRepository.existsBetween(viewerId, other.getId())) {
            throw AppException.forbidden("You cannot message @" + other.getUsername());
        }
        String key = Conversation.directKey(viewerId, other.getId());
        Conversation c = conversationRepository.findByDirectKey(key).orElseGet(() -> {
            Conversation created = new Conversation();
            created.setType(ConversationType.DIRECT);
            created.setDirectKey(key);
            created.setCreatedBy(viewerId);
            conversationRepository.save(created);
            memberRepository.save(new ConversationMember(created.getId(), viewerId, ChatRole.MEMBER));
            memberRepository.save(new ConversationMember(created.getId(), other.getId(), ChatRole.MEMBER));
            return created;
        });
        return get(c.getId(), viewerId);
    }

    @Transactional
    public ChatDto createGroup(Long viewerId, ChatRequests.CreateGroup request) {
        Set<Long> memberIds = new LinkedHashSet<>();
        for (String username : request.usernames()) {
            User u = findUser(username);
            if (!Objects.equals(u.getId(), viewerId) && blockRepository.existsBetween(viewerId, u.getId())) {
                throw AppException.badRequest("You cannot add @" + u.getUsername());
            }
            memberIds.add(u.getId());
        }
        memberIds.remove(viewerId);
        if (memberIds.isEmpty()) {
            throw AppException.badRequest("Add at least one other person");
        }
        if (memberIds.size() + 1 > MAX_GROUP_SIZE) {
            throw AppException.badRequest("A group can have up to " + MAX_GROUP_SIZE + " people");
        }
        Conversation c = new Conversation();
        c.setType(ConversationType.GROUP);
        c.setTitle(request.title().trim());
        c.setCreatedBy(viewerId);
        conversationRepository.save(c);
        memberRepository.save(new ConversationMember(c.getId(), viewerId, ChatRole.ADMIN));
        memberIds.forEach(id -> memberRepository.save(new ConversationMember(c.getId(), id, ChatRole.MEMBER)));
        systemNote(c, name(viewerId) + " created the group");
        return get(c.getId(), viewerId);
    }

    @Transactional
    public ChatDto updateGroup(Long chatId, Long viewerId, ChatRequests.UpdateGroup request) {
        Conversation c = load(chatId);
        requireGroupAdmin(c, viewerId);
        if (request.title() != null && !request.title().isBlank() && !request.title().trim().equals(c.getTitle())) {
            c.setTitle(request.title().trim());
            systemNote(c, name(viewerId) + " renamed the group to \"" + c.getTitle() + "\"");
        }
        if (request.avatarKey() != null) {
            if (!request.avatarKey().isBlank() && !request.avatarKey().equals(c.getAvatarKey())
                    && !MediaKind.AVATAR.ownedBy(request.avatarKey(), viewerId)) {
                throw AppException.badRequest("Upload the picture again and retry");
            }
            c.setAvatarKey(request.avatarKey().isBlank() ? null : request.avatarKey());
        }
        conversationRepository.save(c);
        push(c.getId(), ChatEventDto.updated(c.getId()), null);
        return get(chatId, viewerId);
    }

    @Transactional
    public ChatDto addMember(Long chatId, Long viewerId, String username) {
        Conversation c = load(chatId);
        requireGroupAdmin(c, viewerId);
        User u = findUser(username);
        if (memberRepository.existsById(new ConversationMember.Id(chatId, u.getId()))) {
            throw AppException.conflict("@" + u.getUsername() + " is already in this group");
        }
        if (blockRepository.existsBetween(viewerId, u.getId())) {
            throw AppException.badRequest("You cannot add @" + u.getUsername());
        }
        if (memberRepository.countMembers(chatId) >= MAX_GROUP_SIZE) {
            throw AppException.badRequest("This group is full");
        }
        memberRepository.save(new ConversationMember(chatId, u.getId(), ChatRole.MEMBER));
        systemNote(c, name(viewerId) + " added " + name(u.getId()));
        push(chatId, ChatEventDto.updated(chatId), null);
        return get(chatId, viewerId);
    }

    /** Admins remove members; anyone can remove themselves (leave). The last admin to leave hands over to the oldest member. */
    @Transactional
    public void removeMember(Long chatId, Long viewerId, Long userId) {
        Conversation c = load(chatId);
        if (c.getType() != ConversationType.GROUP) {
            throw AppException.badRequest("You can only leave group chats");
        }
        ConversationMember me = requireMember(c, viewerId);
        boolean leaving = Objects.equals(viewerId, userId);
        if (!leaving && me.getRole() != ChatRole.ADMIN) {
            throw AppException.forbidden("Only group admins can remove people");
        }
        ConversationMember target = memberRepository.findById(new ConversationMember.Id(chatId, userId))
                .orElseThrow(() -> AppException.notFound("Member"));
        memberRepository.delete(target);
        memberRepository.flush();

        List<ConversationMember> rest = memberRepository.findByConversationIds(List.of(chatId));
        if (rest.isEmpty()) {
            conversationRepository.delete(c);
            return;
        }
        if (rest.stream().noneMatch(m -> m.getRole() == ChatRole.ADMIN)) {
            ConversationMember next = rest.stream().min(Comparator.comparing(ConversationMember::getJoinedAt)).orElseThrow();
            next.setRole(ChatRole.ADMIN);
            memberRepository.save(next);
        }
        systemNote(c, leaving ? name(userId) + " left" : name(viewerId) + " removed " + name(userId));
        events.publishEvent(new ChatPush(List.of(userId), ChatEventDto.removed(chatId)));
        push(chatId, ChatEventDto.updated(chatId), null);
    }

    // ------------------------------------------------------------------ messages

    /** Newest first; pass the oldest id you have as {@code beforeId} to load earlier messages. */
    @Transactional(readOnly = true)
    public List<MessageDto> messages(Long chatId, Long viewerId, Long beforeId, Integer size) {
        requireMember(load(chatId), viewerId);
        List<Message> rows = messageRepository.findPage(chatId, beforeId == null ? Long.MAX_VALUE : beforeId,
                Limit.of(Limits.pageSize(size == null ? 40 : size)));
        Map<Long, UserSummaryDto> people = userSummaries.of(rows.stream().map(Message::getSenderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return rows.stream().map(m -> toDto(m, people)).toList();
    }

    @Transactional
    public MessageDto send(Long chatId, Long viewerId, ChatRequests.Send request) {
        Conversation c = load(chatId);
        requireMember(c, viewerId);
        String body = request.body() == null || request.body().isBlank() ? null : request.body().strip();
        String imageKey = request.imageKey() == null || request.imageKey().isBlank() ? null : request.imageKey();
        if (body == null && imageKey == null) {
            throw AppException.badRequest("Write a message or add a photo");
        }
        if (imageKey != null && !MediaKind.POST.ownedBy(imageKey, viewerId)) {
            throw AppException.badRequest("Upload the photo again and retry");
        }
        if (c.getType() == ConversationType.DIRECT) {
            Long other = memberRepository.findUserIds(chatId).stream().filter(id -> !id.equals(viewerId)).findFirst().orElse(null);
            if (other != null && blockRepository.existsBetween(viewerId, other)) {
                throw AppException.forbidden("You cannot message this person");
            }
        }
        Message m = new Message();
        m.setConversationId(chatId);
        m.setSenderId(viewerId);
        m.setBody(body);
        m.setImageKey(imageKey);
        messageRepository.save(m);
        c.setLastMessageAt(Instant.now());
        conversationRepository.save(c);
        // Sending a message means you have seen everything before it.
        memberRepository.findById(new ConversationMember.Id(chatId, viewerId)).ifPresent(me -> {
            me.setLastReadMessageId(m.getId());
            memberRepository.save(me);
        });
        MessageDto dto = toDto(m, userSummaries.of(List.of(viewerId)));
        push(chatId, ChatEventDto.message(dto), null);
        return dto;
    }

    @Transactional
    public void deleteMessage(Long messageId, Long viewerId) {
        Message m = messageRepository.findById(messageId).orElseThrow(() -> AppException.notFound("Message"));
        if (!Objects.equals(m.getSenderId(), viewerId)) {
            throw AppException.forbidden("You can only delete your own messages");
        }
        if (m.isDeleted()) {
            return;
        }
        m.setDeleted(true);
        m.setBody(null);
        m.setImageKey(null);
        messageRepository.save(m);
        push(m.getConversationId(), ChatEventDto.deleted(toDto(m, Map.of())), null);
    }

    /** Moves the viewer's read position forward (never back) and tells the others, for the blue ticks. */
    @Transactional
    public void markRead(Long chatId, Long viewerId, Long messageId) {
        ConversationMember me = requireMember(load(chatId), viewerId);
        if (me.getLastReadMessageId() != null && me.getLastReadMessageId() >= messageId) {
            return;
        }
        Message m = messageRepository.findById(messageId).filter(x -> x.getConversationId().equals(chatId))
                .orElseThrow(() -> AppException.badRequest("That message is not in this chat"));
        me.setLastReadMessageId(m.getId());
        memberRepository.save(me);
        push(chatId, ChatEventDto.read(chatId, viewerId, m.getId()), viewerId);
    }

    /** Called from the WebSocket when someone types; relayed to the others without touching the database. */
    @Transactional(readOnly = true)
    public void typing(Long chatId, Long viewerId) {
        if (!memberRepository.existsById(new ConversationMember.Id(chatId, viewerId))) {
            return;
        }
        List<Long> others = memberRepository.findUserIds(chatId).stream().filter(id -> !id.equals(viewerId)).toList();
        events.publishEvent(new ChatPush(others, ChatEventDto.typing(chatId, viewerId)));
    }

    // ------------------------------------------------------------------ helpers

    private Conversation load(Long chatId) {
        return conversationRepository.findById(chatId).orElseThrow(() -> AppException.notFound("Chat"));
    }

    private ConversationMember requireMember(Conversation c, Long viewerId) {
        return memberRepository.findById(new ConversationMember.Id(c.getId(), viewerId))
                .orElseThrow(() -> AppException.notFound("Chat"));
    }

    private void requireGroupAdmin(Conversation c, Long viewerId) {
        if (c.getType() != ConversationType.GROUP) {
            throw AppException.badRequest("Only group chats can be changed");
        }
        if (requireMember(c, viewerId).getRole() != ChatRole.ADMIN) {
            throw AppException.forbidden("Only group admins can do this");
        }
    }

    private User findUser(String rawUsername) {
        String username = rawUsername.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
        return userRepository.findByUsername(username).filter(User::isActive)
                .orElseThrow(() -> AppException.badRequest("No member called @" + username));
    }

    private String name(Long userId) {
        UserSummaryDto u = userSummaries.of(List.of(userId)).get(userId);
        return u == null ? "Someone" : u.displayName();
    }

    /** A sender-less line in the chat such as "Riya joined", pushed like any message. */
    private void systemNote(Conversation c, String text) {
        Message m = new Message();
        m.setConversationId(c.getId());
        m.setBody(text);
        messageRepository.save(m);
        c.setLastMessageAt(Instant.now());
        conversationRepository.save(c);
        push(c.getId(), ChatEventDto.message(toDto(m, Map.of())), null);
    }

    /** Sends an event to every current member except {@code skip}. */
    private void push(Long chatId, ChatEventDto event, Long skip) {
        List<Long> recipients = memberRepository.findUserIds(chatId).stream().filter(id -> !id.equals(skip)).toList();
        events.publishEvent(new ChatPush(recipients, event));
    }

    private MessageDto toDto(Message m, Map<Long, UserSummaryDto> people) {
        return new MessageDto(m.getId(), m.getConversationId(), m.getSenderId() == null ? null : people.get(m.getSenderId()),
                m.isDeleted() ? null : m.getBody(), m.isDeleted() ? null : mediaUrls.url(m.getImageKey()),
                m.isDeleted(), m.getSenderId() == null, m.getCreatedAt());
    }

    /** Builds chat DTOs with one query each for members, people, last messages and unread counts. */
    private List<ChatDto> toDtos(Collection<Conversation> chats, Long viewerId) {
        if (chats.isEmpty()) {
            return List.of();
        }
        List<Long> ids = chats.stream().map(Conversation::getId).toList();
        Map<Long, List<ConversationMember>> members = memberRepository.findByConversationIds(ids).stream()
                .collect(Collectors.groupingBy(m -> m.getId().getConversationId()));
        Map<Long, Message> latest = messageRepository.findLatest(ids).stream()
                .collect(Collectors.toMap(Message::getConversationId, Function.identity()));
        Map<Long, Long> unread = messageRepository.countUnread(viewerId, ids).stream()
                .collect(Collectors.toMap(MessageRepository.UnreadRow::getConversationId, MessageRepository.UnreadRow::getUnread));
        Set<Long> userIds = new HashSet<>();
        members.values().forEach(list -> list.forEach(m -> userIds.add(m.getId().getUserId())));
        latest.values().forEach(m -> { if (m.getSenderId() != null) userIds.add(m.getSenderId()); });
        Map<Long, UserSummaryDto> people = userSummaries.of(userIds);

        List<ChatDto> out = new ArrayList<>();
        for (Conversation c : chats) {
            List<ConversationMember> ms = members.getOrDefault(c.getId(), List.of());
            ChatRole myRole = ms.stream().filter(m -> m.getId().getUserId().equals(viewerId)).map(ConversationMember::getRole).findFirst().orElse(null);
            List<ChatDto.Member> memberDtos = ms.stream()
                    .map(m -> new ChatDto.Member(people.get(m.getId().getUserId()), m.getRole(),
                            presence.isOnline(m.getId().getUserId()), m.getLastReadMessageId()))
                    .toList();
            String title = c.getTitle();
            String avatar = mediaUrls.url(c.getAvatarKey());
            if (c.getType() == ConversationType.DIRECT) {
                UserSummaryDto other = ms.stream().map(m -> m.getId().getUserId()).filter(id -> !id.equals(viewerId))
                        .findFirst().map(people::get).orElse(null);
                title = other == null ? "Chat" : other.displayName();
                avatar = other == null ? null : other.avatarUrl();
            }
            Message last = latest.get(c.getId());
            out.add(new ChatDto(c.getId(), c.getType(), title, avatar, memberDtos,
                    last == null ? null : toDto(last, people), unread.getOrDefault(c.getId(), 0L), myRole, c.getCreatedAt()));
        }
        return out;
    }
}
