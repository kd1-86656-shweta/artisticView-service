package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.ChatDto;
import com.artisticview.common.dto.ChatRequests;
import com.artisticview.common.dto.InviteCollaboratorRequest;
import com.artisticview.common.dto.MessageDto;
import com.artisticview.core.chat.ChatService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Chat REST API. Live updates (new messages, typing, read ticks) arrive over the WebSocket at /ws. */
@Tag(name = "Chat")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasPermission('MESSAGES', 'CHAT_USE')")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chats")
    public ApiResponse<List<ChatDto>> list() {
        return ApiResponse.ok(chatService.list(CurrentUser.id()));
    }

    @PostMapping("/chats/direct")
    public ApiResponse<ChatDto> direct(@Valid @RequestBody ChatRequests.Direct request) {
        return ApiResponse.ok(chatService.direct(CurrentUser.id(), request.username()));
    }

    @PostMapping("/chats/groups")
    public ApiResponse<ChatDto> createGroup(@Valid @RequestBody ChatRequests.CreateGroup request) {
        return ApiResponse.ok(chatService.createGroup(CurrentUser.id(), request));
    }

    @GetMapping("/chats/{id}")
    public ApiResponse<ChatDto> get(@PathVariable Long id) {
        return ApiResponse.ok(chatService.get(id, CurrentUser.id()));
    }

    @PatchMapping("/chats/{id}")
    public ApiResponse<ChatDto> updateGroup(@PathVariable Long id, @Valid @RequestBody ChatRequests.UpdateGroup request) {
        return ApiResponse.ok(chatService.updateGroup(id, CurrentUser.id(), request));
    }

    @PostMapping("/chats/{id}/members")
    public ApiResponse<ChatDto> addMember(@PathVariable Long id, @Valid @RequestBody InviteCollaboratorRequest request) {
        return ApiResponse.ok(chatService.addMember(id, CurrentUser.id(), request.username()));
    }

    /** Remove someone (admins) or leave (your own id). */
    @DeleteMapping("/chats/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        chatService.removeMember(id, CurrentUser.id(), userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/chats/{id}/messages")
    public ApiResponse<List<MessageDto>> messages(@PathVariable Long id, @RequestParam(required = false) Long before,
                                                  @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(chatService.messages(id, CurrentUser.id(), before, size));
    }

    @PostMapping("/chats/{id}/messages")
    public ApiResponse<MessageDto> send(@PathVariable Long id, @Valid @RequestBody ChatRequests.Send request) {
        return ApiResponse.ok(chatService.send(id, CurrentUser.id(), request));
    }

    @PostMapping("/chats/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id, @Valid @RequestBody ChatRequests.Read request) {
        chatService.markRead(id, CurrentUser.id(), request.messageId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/messages/{id}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long id) {
        chatService.deleteMessage(id, CurrentUser.id());
        return ApiResponse.ok(null);
    }
}
