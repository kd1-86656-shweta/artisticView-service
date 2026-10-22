package com.artisticview.security.config;

import com.artisticview.security.auth.JwtProperties;
import com.artisticview.security.auth.JwtTokenProvider;
import com.artisticview.security.filter.JsonErrorWriter;
import com.artisticview.security.filter.JwtAuthenticationFilter;
import com.artisticview.security.filter.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Endpoints reachable without a token. Everything else needs a valid access token. */
    private static final String[] PUBLIC_POST = {
            "/auth/sign-up",
            "/auth/login",
            "/auth/refresh-token",
            "/auth/logout",
            "/auth/verify-email",
            "/auth/resend-verification",
            "/auth/forgot-password",
            "/auth/reset-password"
    };

    private static final String[] PUBLIC_GET = {
            "/auth/check-availability",
            "/actuator/health/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider,
                                                   JsonErrorWriter errorWriter, StringRedisTemplate redis,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentTypeOptions(c -> { })
                        .frameOptions(f -> f.deny()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> errorWriter.write(res,
                                HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Login required"))
                        .accessDeniedHandler((req, res, ex) -> errorWriter.write(res,
                                HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "You do not have access to this")))
                .addFilterBefore(new RateLimitFilter(redis, errorWriter), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, errorWriter),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
