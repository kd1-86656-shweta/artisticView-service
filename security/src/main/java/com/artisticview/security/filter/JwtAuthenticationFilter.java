package com.artisticview.security.filter;

import com.artisticview.security.auth.AuthUser;
import com.artisticview.security.auth.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code Authorization: Bearer <access token>} and puts an {@link AuthUser} into the security context.
 * Requests without a token continue anonymously; the security rules decide whether that is allowed.
 * An expired or invalid token is rejected straight away so the frontend knows to refresh.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ROLE_PREFIX = "ROLE_";

    private final JwtTokenProvider jwtTokenProvider;
    private final JsonErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            AuthUser user = jwtTokenProvider.toAuthUser(jwtTokenProvider.parse(token));
            var authorities = user.roleIds().stream()
                    .map(id -> new SimpleGrantedAuthority(ROLE_PREFIX + id))
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(user, token, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED", "Access token expired");
            return;
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            log.debug("Rejected invalid token on {}: {}", request.getRequestURI(), e.getMessage());
            errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Invalid access token");
            return;
        }
        chain.doFilter(request, response);
    }
}
