package com.artisticview.core.auth;

import com.artisticview.common.dto.AuthResponse;
import com.artisticview.common.dto.LoginRequest;
import com.artisticview.common.dto.RegisterRequest;
import com.artisticview.common.dto.ResetPasswordRequest;
import com.artisticview.common.enums.UserTokenType;
import com.artisticview.common.exception.AppException;
import com.artisticview.core.mail.EmailService;
import com.artisticview.core.user.SessionService;
import com.artisticview.persistence.auth.RefreshToken;
import com.artisticview.persistence.auth.RefreshTokenRepository;
import com.artisticview.persistence.auth.UserToken;
import com.artisticview.persistence.auth.UserTokenRepository;
import com.artisticview.persistence.rbac.Role;
import com.artisticview.persistence.rbac.RoleRepository;
import com.artisticview.persistence.rbac.UserRole;
import com.artisticview.persistence.rbac.UserRoleRepository;
import com.artisticview.persistence.user.Profile;
import com.artisticview.persistence.user.ProfileRepository;
import com.artisticview.persistence.user.User;
import com.artisticview.persistence.user.UserRepository;
import com.artisticview.security.auth.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final String BAD_CREDENTIALS = "Incorrect email/username or password";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public AuthResult signUp(RegisterRequest request, String device) {
        String email = normalize(request.email());
        String username = normalize(request.username());
        if (userRepository.existsByEmail(email)) {
            throw AppException.conflict("An account with this email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw AppException.conflict("This username is taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? request.username() : request.displayName().trim();
        profileRepository.save(new Profile(user.getId(), displayName));

        Role member = roleRepository.findByInternalName(Role.MEMBER)
                .orElseThrow(() -> new IllegalStateException("MEMBER role is not seeded"));
        userRoleRepository.save(new UserRole(user.getId(), member.getId()));

        sendVerificationEmail(user);
        return issueTokens(user, device);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String device) {
        String identifier = normalize(request.identifier());
        User user = (identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier))
                .orElseThrow(() -> AppException.unauthorized(BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AppException.unauthorized(BAD_CREDENTIALS);
        }
        requireActive(user);
        return issueTokens(user, device);
    }

    /**
     * Rotates the refresh token. Presenting an already-revoked token means it was stolen and replayed,
     * so every session of that user is revoked.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResult refresh(String refreshToken, String device) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AppException.unauthorized("No session");
        }
        Claims claims;
        try {
            claims = jwtTokenProvider.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw AppException.unauthorized("Session expired, please log in again");
        }
        if (!JwtTokenProvider.TYPE_REFRESH.equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            throw AppException.unauthorized("Session expired, please log in again");
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(claims.getId()))
                .orElseThrow(() -> AppException.unauthorized("Session expired, please log in again"));
        Instant now = Instant.now();
        if (stored.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", stored.getUserId());
            refreshTokenRepository.revokeAllForUser(stored.getUserId(), now);
            throw AppException.unauthorized("Session expired, please log in again");
        }
        if (!stored.isUsable(now)) {
            throw AppException.unauthorized("Session expired, please log in again");
        }
        stored.setRevokedAt(now);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> AppException.unauthorized("Session expired, please log in again"));
        requireActive(user);
        return issueTokens(user, device);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parse(refreshToken);
            refreshTokenRepository.findByTokenHash(TokenHasher.sha256(claims.getId()))
                    .ifPresent(t -> t.setRevokedAt(Instant.now()));
        } catch (JwtException | IllegalArgumentException ignored) {
            // Already invalid: nothing to revoke.
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        UserToken userToken = consume(token, UserTokenType.EMAIL_VERIFICATION);
        User user = userRepository.findById(userToken.getUserId()).orElseThrow(() -> AppException.notFound("User"));
        user.setEmailVerified(true);
    }

    @Transactional
    public void resendVerification(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> AppException.notFound("User"));
        if (user.isEmailVerified()) {
            throw AppException.badRequest("Your email is already verified");
        }
        sendVerificationEmail(user);
    }

    /** Always succeeds, so the response does not reveal which emails have accounts. */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(normalize(email)).filter(User::isActive).ifPresent(user -> {
            String token = createUserToken(user.getId(), UserTokenType.PASSWORD_RESET, RESET_TTL);
            emailService.send(user.getEmail(), "Reset your password",
                    "Someone asked to reset your password. If it was you, open this link within 30 minutes:\n\n"
                            + frontendUrl + "/reset-password?token=" + token
                            + "\n\nIf it was not you, you can ignore this email.");
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserToken userToken = consume(request.token(), UserTokenType.PASSWORD_RESET);
        User user = userRepository.findById(userToken.getUserId()).orElseThrow(() -> AppException.notFound("User"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(normalize(username));
    }

    private AuthResult issueTokens(User user, String device) {
        List<Long> roleIds = userRoleRepository.findRoleIds(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail(),
                roleIds);
        JwtTokenProvider.IssuedRefreshToken refresh = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken stored = new RefreshToken();
        stored.setUserId(user.getId());
        stored.setTokenHash(TokenHasher.sha256(refresh.jti()));
        stored.setExpiresAt(refresh.expiresAt());
        stored.setDevice(device == null ? null : device.substring(0, Math.min(device.length(), 255)));
        refreshTokenRepository.save(stored);

        AuthResponse body = new AuthResponse(accessToken, jwtTokenProvider.accessTokenTtlSeconds(),
                sessionService.buildSession(user, roleIds));
        return new AuthResult(body, refresh.token(), refresh.expiresAt());
    }

    private void sendVerificationEmail(User user) {
        String token = createUserToken(user.getId(), UserTokenType.EMAIL_VERIFICATION, VERIFICATION_TTL);
        emailService.send(user.getEmail(), "Confirm your email",
                "Welcome! Please confirm your email address by opening this link:\n\n"
                        + frontendUrl + "/verify-email?token=" + token);
    }

    private String createUserToken(Long userId, UserTokenType type, Duration ttl) {
        String raw = TokenHasher.newOpaqueToken();
        UserToken token = new UserToken();
        token.setUserId(userId);
        token.setType(type);
        token.setTokenHash(TokenHasher.sha256(raw));
        token.setExpiresAt(Instant.now().plus(ttl));
        userTokenRepository.save(token);
        return raw;
    }

    private UserToken consume(String rawToken, UserTokenType type) {
        UserToken token = userTokenRepository.findByTokenHashAndType(TokenHasher.sha256(rawToken), type)
                .orElseThrow(() -> AppException.badRequest("This link is invalid or has already been used"));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw AppException.badRequest("This link is invalid or has expired");
        }
        token.setUsedAt(now);
        return token;
    }

    private static void requireActive(User user) {
        if (!user.isActive()) {
            throw new AppException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "This account is suspended. Contact support if you think this is a mistake.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
