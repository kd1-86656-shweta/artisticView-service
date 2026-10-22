package com.artisticview.api.controller;

import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.HomeDto;
import com.artisticview.core.user.HomeService;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    @PreAuthorize("hasPermission('HOME', 'DISCOVER_VIEW')")
    public ApiResponse<HomeDto> home() {
        return ApiResponse.ok(homeService.home(CurrentUser.id()));
    }
}
