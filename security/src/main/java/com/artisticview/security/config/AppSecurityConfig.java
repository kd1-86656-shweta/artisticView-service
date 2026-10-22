package com.artisticview.security.config;

import com.artisticview.security.filter.RolesEvaluator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/** Plugs {@link RolesEvaluator} into method security so {@code hasPermission(...)} works in @PreAuthorize. */
@Configuration
public class AppSecurityConfig {

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(@Lazy RolesEvaluator rolesEvaluator,
                                                                           ApplicationContext context) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(rolesEvaluator);
        handler.setApplicationContext(context);
        return handler;
    }
}
