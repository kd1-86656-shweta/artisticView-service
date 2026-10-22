package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.AdminUserDto;
import com.artisticview.common.dto.PlatformStatsDto;
import com.artisticview.common.dto.UpdateUserRoleRequest;
import com.artisticview.common.dto.UpdateUserStatusRequest;
import com.artisticview.common.enums.UserStatus;
import com.artisticview.core.admin.AdminService;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @PreAuthorize("hasPermission('ADMINISTRATION', 'PLATFORM_STATS_VIEW')")
    public ApiResponse<PlatformStatsDto> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/users")
    @PreAuthorize("hasPermission('ADMINISTRATION', 'USER_MANAGE')")
    public ApiResponse<List<AdminUserDto>> users(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) UserStatus status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserDto> result = adminService.users(q, status, page, size);
        return ApiResponse.ok(result.getContent(),
                Map.of("page", result.getNumber(), "size", result.getSize(), "total", result.getTotalElements()));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasPermission('ADMINISTRATION', 'USER_MANAGE')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        adminService.updateStatus(CurrentUser.id(), id, request.status());
        return ApiResponse.ok(null);
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasPermission('ADMINISTRATION', 'USER_MANAGE')")
    public ApiResponse<Void> replaceRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        adminService.replaceRoles(CurrentUser.id(), id, request.roles());
        return ApiResponse.ok(null);
    }
}
