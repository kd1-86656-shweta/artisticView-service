package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.api.CursorPage;
import com.artisticview.common.dto.CollaborationResponseRequest;
import com.artisticview.common.dto.EventDto;
import com.artisticview.common.dto.EventRequest;
import com.artisticview.common.dto.InviteCollaboratorRequest;
import com.artisticview.common.dto.PromoteEventRequest;
import com.artisticview.core.event.EventService;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Events")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /** scope = upcoming (default) | promoted | mine */
    @GetMapping
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<CursorPage<EventDto>> list(@RequestParam(defaultValue = "upcoming") String scope,
                                                  @RequestParam(required = false) String cursor,
                                                  @RequestParam(required = false) Integer size) {
        Long viewerId = CurrentUser.id();
        return ApiResponse.ok(switch (scope) {
            case "mine" -> eventService.mine(viewerId, cursor, size);
            case "promoted" -> eventService.upcoming(viewerId, true, cursor, size);
            default -> eventService.upcoming(viewerId, false, cursor, size);
        });
    }

    @GetMapping("/invites")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<List<EventDto>> invites() {
        return ApiResponse.ok(eventService.invites(CurrentUser.id()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<EventDto> get(@PathVariable Long id) {
        return ApiResponse.ok(eventService.get(id, CurrentUser.id()));
    }

    @PostMapping
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_HOST')")
    public ApiResponse<EventDto> create(@Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(eventService.create(CurrentUser.id(), request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_HOST')")
    public ApiResponse<EventDto> update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(eventService.update(id, CurrentUser.id(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_HOST')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.delete(id, CurrentUser.id());
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/rsvp")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<EventDto> rsvp(@PathVariable Long id) {
        return ApiResponse.ok(eventService.rsvp(id, CurrentUser.id()));
    }

    @DeleteMapping("/{id}/rsvp")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<EventDto> cancelRsvp(@PathVariable Long id) {
        return ApiResponse.ok(eventService.cancelRsvp(id, CurrentUser.id()));
    }

    @PostMapping("/{id}/collaborators")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_HOST')")
    public ApiResponse<EventDto> inviteCollaborator(@PathVariable Long id, @Valid @RequestBody InviteCollaboratorRequest request) {
        return ApiResponse.ok(eventService.inviteCollaborator(id, CurrentUser.id(), request.username()));
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<EventDto> removeCollaborator(@PathVariable Long id, @PathVariable Long userId) {
        return ApiResponse.ok(eventService.removeCollaborator(id, CurrentUser.id(), userId));
    }

    @PutMapping("/{id}/collaboration")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_VIEW')")
    public ApiResponse<EventDto> respond(@PathVariable Long id, @Valid @RequestBody CollaborationResponseRequest request) {
        return ApiResponse.ok(eventService.respondToInvite(id, CurrentUser.id(), request.accept()));
    }

    @PostMapping("/{id}/promotion")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_HOST')")
    public ApiResponse<EventDto> promote(@PathVariable Long id, @Valid @RequestBody PromoteEventRequest request) {
        return ApiResponse.ok(eventService.promote(id, CurrentUser.id(), request.days()));
    }

    @DeleteMapping("/{id}/promotion")
    @PreAuthorize("hasPermission('EVENTS', 'EVENT_HOST')")
    public ApiResponse<EventDto> stopPromotion(@PathVariable Long id) {
        return ApiResponse.ok(eventService.stopPromotion(id, CurrentUser.id()));
    }
}
