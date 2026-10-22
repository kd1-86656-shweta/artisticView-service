package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.ProfileDto;
import com.artisticview.common.dto.UpdateProfileRequest;
import com.artisticview.core.user.PeopleService;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final PeopleService peopleService;

    @GetMapping("/{username}")
    @PreAuthorize("hasPermission('PEOPLE', 'USER_VIEW')")
    public ApiResponse<ProfileDto> profile(@PathVariable String username) {
        return ApiResponse.ok(peopleService.profile(username, CurrentUser.id()));
    }

    @PatchMapping("/me/profile")
    @PreAuthorize("hasPermission('PROFILE', 'PROFILE_UPDATE')")
    public ApiResponse<ProfileDto> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(peopleService.updateMyProfile(CurrentUser.id(), request));
    }

    @PutMapping("/{id}/follow")
    @PreAuthorize("hasPermission('PEOPLE', 'USER_FOLLOW')")
    public ApiResponse<Void> follow(@PathVariable Long id) {
        peopleService.follow(CurrentUser.id(), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/follow")
    @PreAuthorize("hasPermission('PEOPLE', 'USER_FOLLOW')")
    public ApiResponse<Void> unfollow(@PathVariable Long id) {
        peopleService.unfollow(CurrentUser.id(), id);
        return ApiResponse.ok(null);
    }
}
