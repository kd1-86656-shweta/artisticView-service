package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.CommunityDto;
import com.artisticview.common.dto.CommunityMemberDto;
import com.artisticview.common.dto.CommunityRequest;
import com.artisticview.common.dto.InviteCollaboratorRequest;
import com.artisticview.common.dto.MemberRoleRequest;
import com.artisticview.core.community.CommunityService;
import com.artisticview.core.permission.StaffCheck;
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

@Tag(name = "Communities")
@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final StaffCheck staffCheck;

    /** scope = discover (default, public communities, optional q search) | mine (joined) | invites */
    @GetMapping
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<List<CommunityDto>> list(@RequestParam(defaultValue = "discover") String scope,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        Long me = CurrentUser.id();
        return ApiResponse.ok(switch (scope) {
            case "mine" -> communityService.joined(me);
            case "invites" -> communityService.invites(me);
            default -> communityService.discover(me, q, page, size);
        });
    }

    @PostMapping
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_CREATE')")
    public ApiResponse<CommunityDto> create(@Valid @RequestBody CommunityRequest request) {
        return ApiResponse.ok(communityService.create(CurrentUser.id(), request));
    }

    @GetMapping("/{slug}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<CommunityDto> get(@PathVariable String slug) {
        return ApiResponse.ok(communityService.get(slug, CurrentUser.id()));
    }

    @PatchMapping("/{slug}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<CommunityDto> update(@PathVariable String slug, @Valid @RequestBody CommunityRequest request) {
        return ApiResponse.ok(communityService.update(slug, CurrentUser.id(), request));
    }

    @DeleteMapping("/{slug}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<Void> delete(@PathVariable String slug) {
        communityService.delete(slug, CurrentUser.id(), staffCheck.canModerateContent());
        return ApiResponse.ok(null);
    }

    /** Join a public community or accept an invite. */
    @PutMapping("/{slug}/membership")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<CommunityDto> join(@PathVariable String slug) {
        return ApiResponse.ok(communityService.join(slug, CurrentUser.id()));
    }

    /** Leave, or decline an invite. */
    @DeleteMapping("/{slug}/membership")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<CommunityDto> leave(@PathVariable String slug) {
        return ApiResponse.ok(communityService.leave(slug, CurrentUser.id()));
    }

    @GetMapping("/{slug}/members")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<List<CommunityMemberDto>> members(@PathVariable String slug) {
        return ApiResponse.ok(communityService.members(slug, CurrentUser.id()));
    }

    /** Owners and moderators add a member by username (they receive an invite). */
    @PostMapping("/{slug}/members")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<List<CommunityMemberDto>> addMember(@PathVariable String slug, @Valid @RequestBody InviteCollaboratorRequest request) {
        return ApiResponse.ok(communityService.invite(slug, CurrentUser.id(), request.username()));
    }

    @DeleteMapping("/{slug}/members/{userId}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<List<CommunityMemberDto>> removeMember(@PathVariable String slug, @PathVariable Long userId) {
        return ApiResponse.ok(communityService.removeMember(slug, CurrentUser.id(), userId));
    }

    @PatchMapping("/{slug}/members/{userId}")
    @PreAuthorize("hasPermission('COMMUNITIES', 'COMMUNITY_VIEW')")
    public ApiResponse<List<CommunityMemberDto>> setRole(@PathVariable String slug, @PathVariable Long userId,
                                                         @Valid @RequestBody MemberRoleRequest request) {
        return ApiResponse.ok(communityService.setRole(slug, CurrentUser.id(), userId, request.role()));
    }
}
