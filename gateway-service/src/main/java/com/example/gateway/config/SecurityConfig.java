package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",

                                "/v3/api-docs",
                                "/v3/api-docs/**",

                                "/user/v3/api-docs",
                                "/user/v3/api-docs/**",

                                "/room/v3/api-docs",
                                "/room/v3/api-docs/**",

                                "/reservation/v3/api-docs",
                                "/reservation/v3/api-docs/**"
                        ).authenticated()
                        .anyExchange().permitAll()
                )

                // 注意：Reactive Security 中 formLogin 不需要 withDefaults()
                .formLogin(form -> { })   // 空 lambda 即默认配置

                .logout(ServerHttpSecurity.LogoutSpec::disable);

        return http.build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("swagger")
                .password("{noop}swagger123")
                .roles("SWAGGER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}
