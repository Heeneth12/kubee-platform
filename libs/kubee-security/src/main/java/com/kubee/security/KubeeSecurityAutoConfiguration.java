package com.kubee.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Registers JWT validation for every Kubee service that depends on kubee-security.
 * Services keep their own SecurityConfig and add {@link JwtAuthFilter} to their filter chain.
 * Needs the {@code jwt.secret} property (and optionally jwt.access-token-expiration / jwt.refresh-token-expiration).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class KubeeSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider();
    }

    @Bean
    @RequestScope
    @ConditionalOnMissingBean
    public UserContext userContext() {
        return new UserContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthFilter jwtAuthFilter(JwtTokenProvider jwtTokenProvider, UserContext userContext,
                                       ObjectProvider<JwtAuthorityContributor> contributors) {
        return new JwtAuthFilter(jwtTokenProvider, userContext, contributors.orderedStream().toList());
    }
}
