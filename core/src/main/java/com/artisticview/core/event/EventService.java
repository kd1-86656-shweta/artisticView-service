package com.artisticview.core.event;

import com.artisticview.common.api.CursorPage;
import com.artisticview.common.dto.EventDto;
import com.artisticview.common.dto.EventRequest;
import com.artisticview.common.dto.UserSummaryDto;
import com.artisticview.common.enums.CollaboratorStatus;
import com.artisticview.common.enums.EventLocationType;
import com.artisticview.common.enums.EventStatus;
import com.artisticview.common.exception.AppException;
import com.artisticview.common.util.Cursor;
import com.artisticview.common.util.Limits;
import com.artisticview.core.storage.MediaUrls;
import com.artisticview.persistence.event.Event;
import com.artisticview.persistence.event.EventCollaborator;
import com.artisticview.persistence.event.EventCollaboratorId;
import com.artisticview.persistence.event.EventCollaboratorRepository;
import com.artisticview.persistence.event.EventRepository;
import com.artisticview.persistence.event.EventRsvpRepository;
import com.artisticview.persistence.social.BlockRepository;
import com.artisticview.persistence.user.Profile;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
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
 * Events: hosts schedule sessions, invite co-hosts (collaborators), members RSVP, and hosts can promote an event
 * so it is featured on Home. The host and accepted co-hosts can edit; only the host can delete, invite or promote.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    /** An event stays in "upcoming" lists until this long after it starts. */
    private static final Duration ONGOING_GRACE = Duration.ofHours(3);
    private static final int MAX_COLLABORATORS = 10;

    private final EventRepository eventRepository;
    private final EventCollaboratorRepository collaboratorRepository;
    private final EventRsvpRepository rsvpRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BlockRepository blockRepository;
    private final MediaUrls mediaUrls;

    // ------------------------------------------------------------------ listings

    /** Upcoming events, soonest first. The cursor is opaque; null starts from now. */
    @Transactional(readOnly = true)
    public CursorPage<EventDto> upcoming(Long viewerId, boolean promotedOnly, String rawCursor, Integer size) {
        int limit = Limits.pageSize(size);
        Instant since = Instant.now().minus(ONGOING_GRACE);
        Cursor cursor = rawCursor == null || rawCursor.isBlank() ? new Cursor(since, 0) : Cursor.decode(rawCursor);
        List<Event> rows = eventRepository.findUpcoming(viewerId, since, cursor.ts(), cursor.id(), promotedOnly, limit + 1);
        return page(rows, limit, viewerId);
    }

    /** Events I host or co-host, including past ones. */
    @Transactional(readOnly = true)
    public CursorPage<EventDto> mine(Long viewerId, String rawCursor, Integer size) {
        int limit = Limits.pageSize(size);
        Cursor cursor = Cursor.decode(rawCursor);
        List<Event> rows = eventRepository.findManagedBy(viewerId, cursor.ts(), cursor.id(), Limit.of(limit + 1));
        return page(rows, limit, viewerId);
    }

    /** Events where I have a pending co-host invite. */
    @Transactional(readOnly = true)
    public List<EventDto> invites(Long viewerId) {
        List<Long> ids = collaboratorRepository.findByUserAndStatus(viewerId, CollaboratorStatus.INVITED).stream()
                .map(c -> c.getId().getEventId())
                .toList();
        return toDtos(eventRepository.findAllById(ids), viewerId);
    }

    @Transactional(readOnly = true)
    public EventDto get(Long eventId, Long viewerId) {
        Event event = load(eventId);
        if (viewerId != null && !Objects.equals(event.getHostId(), viewerId)
                && blockRepository.existsBetween(viewerId, event.getHostId())) {
            throw AppException.notFound("Event");
        }
        return toDtos(List.of(event), viewerId).get(0);
    }

    // ------------------------------------------------------------------ create / update / delete

    @Transactional
    public EventDto create(Long hostId, EventRequest request) {
        if (isBlank(request.title())) {
            throw AppException.badRequest("Give your event a title");
        }
        if (request.startsAt() == null) {
            throw AppException.badRequest("Choose when your event starts");
        }
        if (request.startsAt().isBefore(Instant.now())) {
            throw AppException.badRequest("The start time must be in the future");
        }
        if (request.locationType() == null) {
            throw AppException.badRequest("Choose online or in person");
        }
        Event event = new Event();
        event.setHostId(hostId);
        apply(event, request);
        if (request.promoteDays() != null && request.promoteDays() > 0) {
            event.setPromotedUntil(Instant.now().plus(Duration.ofDays(request.promoteDays())));
        }
        eventRepository.save(event);

        if (request.collaborators() != null) {
            for (String username : new LinkedHashSet<>(request.collaborators())) {
                if (!isBlank(username)) {
                    invite(event, hostId, username);
                }
            }
        }
        return toDtos(List.of(event), hostId).get(0);
    }

    @Transactional
    public EventDto update(Long eventId, Long viewerId, EventRequest request) {
        Event event = load(eventId);
        requireManager(event, viewerId);
        if (request.title() != null && request.title().isBlank()) {
            throw AppException.badRequest("Title cannot be empty");
        }
        apply(event, request);
        if (event.getCapacity() != null && event.getCapacity() < event.getRsvpCount()) {
            throw AppException.badRequest("Capacity cannot be lower than the " + event.getRsvpCount() + " people already going");
        }
        eventRepository.save(event);
        return toDtos(List.of(event), viewerId).get(0);
    }

    /** Deletes the event with its RSVPs and co-host invites (cascade). Host only. */
    @Transactional
    public void delete(Long eventId, Long viewerId) {
        Event event = load(eventId);
        requireHost(event, viewerId);
        eventRepository.delete(event);
    }

    /** Copies the non-null request fields onto the event and checks the times still make sense. */
    private void apply(Event event, EventRequest r) {
        if (r.title() != null) event.setTitle(r.title().trim());
        if (r.description() != null) event.setDescription(blankToNull(r.description()));
        if (r.category() != null) event.setCategory(blankToNull(r.category()));
        if (r.startsAt() != null) event.setStartsAt(r.startsAt());
        if (r.endsAt() != null) event.setEndsAt(r.endsAt());
        if (r.locationType() != null) event.setLocationType(r.locationType());
        if (r.location() != null) event.setLocation(blankToNull(r.location()));
        if (r.meetingUrl() != null) event.setMeetingUrl(blankToNull(r.meetingUrl()));
        if (r.capacity() != null) event.setCapacity(r.capacity());
        if (event.getEndsAt() != null && !event.getEndsAt().isAfter(event.getStartsAt())) {
            throw AppException.badRequest("The end time must be after the start time");
        }
        if (event.getLocationType() == EventLocationType.IN_PERSON && isBlank(event.getLocation())) {
            throw AppException.badRequest("Add a venue or address for an in-person event");
        }
    }

    // ------------------------------------------------------------------ RSVP

    @Transactional
    public EventDto rsvp(Long eventId, Long viewerId) {
        Event event = load(eventId);
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw AppException.badRequest("This event is not taking RSVPs");
        }
        if (blockRepository.existsBetween(viewerId, event.getHostId())) {
            throw AppException.notFound("Event");
        }
        if (rsvpRepository.insertIgnore(eventId, viewerId) == 1 && eventRepository.takeSeat(eventId) == 0) {
            throw AppException.conflict("This event is full");
        }
        return get(eventId, viewerId);
    }

    @Transactional
    public EventDto cancelRsvp(Long eventId, Long viewerId) {
        load(eventId);
        if (rsvpRepository.deleteRsvp(eventId, viewerId) == 1) {
            eventRepository.releaseSeat(eventId);
        }
        return get(eventId, viewerId);
    }

    // ------------------------------------------------------------------ collaborators

    @Transactional
    public EventDto inviteCollaborator(Long eventId, Long viewerId, String username) {
        Event event = load(eventId);
        requireHost(event, viewerId);
        invite(event, viewerId, username);
        return get(eventId, viewerId);
    }

    /** The host removes a co-host, or a co-host removes themselves (leaves). */
    @Transactional
    public EventDto removeCollaborator(Long eventId, Long viewerId, Long userId) {
        Event event = load(eventId);
        if (!Objects.equals(viewerId, userId)) {
            requireHost(event, viewerId);
        }
        collaboratorRepository.deleteById(new EventCollaboratorId(eventId, userId));
        return get(eventId, viewerId);
    }

    @Transactional
    public EventDto respondToInvite(Long eventId, Long viewerId, boolean accept) {
        EventCollaborator invite = collaboratorRepository.findById(new EventCollaboratorId(eventId, viewerId))
                .orElseThrow(() -> AppException.notFound("Invite"));
        invite.setStatus(accept ? CollaboratorStatus.ACCEPTED : CollaboratorStatus.DECLINED);
        collaboratorRepository.save(invite);
        return get(eventId, viewerId);
    }

    private void invite(Event event, Long hostId, String rawUsername) {
        User user = userRepository.findByUsername(rawUsername.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .orElseThrow(() -> AppException.badRequest("No member called @" + rawUsername.trim().replaceFirst("^@", "")));
        if (Objects.equals(user.getId(), event.getHostId())) {
            throw AppException.badRequest("You are already the host");
        }
        if (blockRepository.existsBetween(hostId, user.getId())) {
            throw AppException.badRequest("You cannot invite @" + user.getUsername());
        }
        EventCollaboratorId id = new EventCollaboratorId(event.getId(), user.getId());
        EventCollaborator existing = collaboratorRepository.findById(id).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == CollaboratorStatus.DECLINED) {
                existing.setStatus(CollaboratorStatus.INVITED);
                collaboratorRepository.save(existing);
            }
            return;
        }
        if (collaboratorRepository.countActive(event.getId()) >= MAX_COLLABORATORS) {
            throw AppException.badRequest("An event can have up to " + MAX_COLLABORATORS + " co-hosts");
        }
        collaboratorRepository.save(new EventCollaborator(event.getId(), user.getId(), hostId));
    }

    // ------------------------------------------------------------------ promotion

    /** Features the event on Home for {@code days}, extending a promotion that is already running. Host only. */
    @Transactional
    public EventDto promote(Long eventId, Long viewerId, int days) {
        Event event = load(eventId);
        requireHost(event, viewerId);
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw AppException.badRequest("Only scheduled events can be promoted");
        }
        Instant now = Instant.now();
        Instant from = event.isPromotedAt(now) ? event.getPromotedUntil() : now;
        event.setPromotedUntil(from.plus(Duration.ofDays(days)));
        eventRepository.save(event);
        return get(eventId, viewerId);
    }

    @Transactional
    public EventDto stopPromotion(Long eventId, Long viewerId) {
        Event event = load(eventId);
        requireHost(event, viewerId);
        event.setPromotedUntil(null);
        eventRepository.save(event);
        return get(eventId, viewerId);
    }

    // ------------------------------------------------------------------ helpers

    private Event load(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> AppException.notFound("Event"));
    }

    private void requireHost(Event event, Long viewerId) {
        if (!Objects.equals(event.getHostId(), viewerId)) {
            throw AppException.forbidden("Only the host can do this");
        }
    }

    private void requireManager(Event event, Long viewerId) {
        if (Objects.equals(event.getHostId(), viewerId)) {
            return;
        }
        boolean coHost = collaboratorRepository.findById(new EventCollaboratorId(event.getId(), viewerId))
                .map(c -> c.getStatus() == CollaboratorStatus.ACCEPTED)
                .orElse(false);
        if (!coHost) {
            throw AppException.forbidden("Only the host and co-hosts can edit this event");
        }
    }

    private CursorPage<EventDto> page(List<Event> rows, int limit, Long viewerId) {
        boolean more = rows.size() > limit;
        List<Event> items = more ? rows.subList(0, limit) : rows;
        String next = more ? new Cursor(items.get(items.size() - 1).getStartsAt(), items.get(items.size() - 1).getId()).encode() : null;
        return new CursorPage<>(toDtos(items, viewerId), next);
    }

    /** Builds DTOs for a batch of events with one query each for co-hosts, people and the viewer's RSVPs. */
    private List<EventDto> toDtos(Collection<Event> events, Long viewerId) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, List<EventCollaborator>> collaborators = collaboratorRepository.findByEventIds(eventIds).stream()
                .collect(Collectors.groupingBy(c -> c.getId().getEventId()));
        Set<Long> userIds = new HashSet<>();
        events.forEach(e -> userIds.add(e.getHostId()));
        collaborators.values().forEach(list -> list.forEach(c -> userIds.add(c.getId().getUserId())));
        Map<Long, UserSummaryDto> people = summaries(userIds);
        Set<Long> going = viewerId == null ? Set.of() : new HashSet<>(rsvpRepository.findGoingAmong(viewerId, eventIds));
        Instant now = Instant.now();

        return events.stream().map(e -> {
            List<EventCollaborator> cs = collaborators.getOrDefault(e.getId(), List.of());
            boolean isHost = Objects.equals(e.getHostId(), viewerId);
            CollaboratorStatus mine = cs.stream()
                    .filter(c -> Objects.equals(c.getId().getUserId(), viewerId))
                    .map(EventCollaborator::getStatus)
                    .findFirst().orElse(null);
            boolean canManage = isHost || mine == CollaboratorStatus.ACCEPTED;
            // Hosts see every invite; everyone else only sees co-hosts who said yes.
            List<EventDto.CollaboratorDto> visible = cs.stream()
                    .filter(c -> canManage || c.getStatus() == CollaboratorStatus.ACCEPTED)
                    .map(c -> new EventDto.CollaboratorDto(people.get(c.getId().getUserId()), c.getStatus()))
                    .toList();
            boolean isGoing = going.contains(e.getId());
            // The session link is for people who RSVP'd and the hosts, not for anyone browsing.
            String meetingUrl = canManage || isGoing ? e.getMeetingUrl() : null;
            return new EventDto(e.getId(), e.getTitle(), e.getDescription(), e.getCategory(),
                    e.getStartsAt(), e.getEndsAt(), e.getLocationType(), e.getLocation(), meetingUrl,
                    e.getCapacity(), e.getStatus(), people.get(e.getHostId()), visible,
                    e.getRsvpCount(), isGoing, e.isPromotedAt(now),
                    isHost ? e.getPromotedUntil() : null, isHost, canManage, mine, e.getCreatedAt());
        }).toList();
    }

    private Map<Long, UserSummaryDto> summaries(Collection<Long> userIds) {
        Map<Long, Profile> profiles = profileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> {
            Profile p = profiles.get(u.getId());
            return new UserSummaryDto(u.getId(), u.getUsername(),
                    p == null || p.getDisplayName() == null ? u.getUsername() : p.getDisplayName(),
                    p == null ? null : mediaUrls.url(p.getAvatarKey()));
        }));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
