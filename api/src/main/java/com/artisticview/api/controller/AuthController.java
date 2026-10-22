package com.artisticview.api.controller;

import com.artisticview.api.support.RefreshCookie;
import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.dto.AuthResponse;
import com.artisticview.common.dto.AvailabilityDto;
import com.artisticview.common.dto.EmailRequest;
import com.artisticview.common.dto.LoginRequest;
import com.artisticview.common.dto.RegisterRequest;
import com.artisticview.common.dto.ResetPasswordRequest;
import com.artisticview.common.dto.SessionDto;
import com.artisticview.common.dto.TokenRequest;
import com.artisticview.core.auth.AuthResult;
import com.artisticview.core.auth.AuthService;
import com.artisticview.core.user.SessionService;
import com.artisticview.security.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final RefreshCookie refreshCookie;

    @Operation(summary = "Create an account and log in")
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signUp(@Valid @RequestBody RegisterRequest request,
                                            HttpServletRequest http, HttpServletResponse response) {
        return withCookie(authService.signUp(request, http.getHeader("User-Agent")), response);
    }

    @Operation(summary = "Log in with email or username")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest http, HttpServletResponse response) {
        return withCookie(authService.login(request, http.getHeader("User-Agent")), response);
    }

    @Operation(summary = "Exchange the refresh cookie for a new access token (rotates the cookie)")
    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refresh(@CookieValue(name = RefreshCookie.NAME, required = false) String token,
                                             HttpServletRequest http, HttpServletResponse response) {
        try {
            return withCookie(authService.refresh(token, http.getHeader("User-Agent")), response);
        } catch (RuntimeException e) {
            refreshCookie.clear(response);
            throw e;
        }
    }

    @Operation(summary = "Log out this device")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CookieValue(name = RefreshCookie.NAME, required = false) String token,
                                    HttpServletResponse response) {
        authService.logout(token);
        refreshCookie.clear(response);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Current user, privileges and feature access")
    @GetMapping("/me")
    public ApiResponse<SessionDto> me() {
        return ApiResponse.ok(sessionService.currentSession(CurrentUser.id()));
    }

    @Operation(summary = "Is this username free? Used live by the sign-up form")
    @GetMapping("/check-availability")
    public ApiResponse<AvailabilityDto> checkAvailability(@RequestParam String username) {
        return ApiResponse.ok(new AvailabilityDto(username, authService.isUsernameAvailable(username)));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        authService.verifyEmail(request.token());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Send the verification email again (logged-in user)")
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification() {
        authService.resendVerification(CurrentUser.id());
        return ApiResponse.ok(null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(null);
    }

    private ApiResponse<AuthResponse> withCookie(AuthResult result, HttpServletResponse response) {
        refreshCookie.set(response, result.refreshToken(), result.refreshExpiresAt());
        return ApiResponse.ok(result.body());
    }
}
