package com.example.config;

import com.example.common.filter.JwtAuthenticationFilter;
import com.example.common.repository.TokenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
public class SecurityConfig {

    private final TokenRepository tokenRepository;
    private final LogoutHandler logoutHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(TokenRepository tokenRepository, LogoutHandler logoutHandler, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.tokenRepository = tokenRepository;
        this.logoutHandler = logoutHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CORS 交给网关统一处理，避免重复 Access-Control-Allow-Origin
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/loginByEmailCode",
                                "/auth/loginByUserId",
                                "/auth/sendEmailCode",
                                "/auth/refresh",
                                "/auth/logout",
                                "/register",
                                "/login",
                                "/loginByEmailCode",
                                "/loginByUserId",
                                "/sendEmailCode",
                                "/refresh",
                                "/logout",
                                "/systemManage/template/**"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/*/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((req, res, auth) -> SecurityContextHolder.clearContext())
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
