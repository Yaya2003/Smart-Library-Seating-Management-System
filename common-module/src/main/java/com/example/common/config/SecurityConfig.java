//package com.example.common.config;
//
//import com.example.common.filter.JwtAuthenticationFilter;
//import com.example.common.repository.TokenRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.web.authentication.logout.LogoutHandler;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
//
////@Configuration
////@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final TokenRepository tokenRepository;
//    private final LogoutHandler logoutHandler;
//    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    /**
//     * 白名单路径（登录、注册、静态资源、Swagger 等）
//     */
//    private static final String[] WHITE_LIST = {
//            "/auth/**",
//            "/auth/register",
//            "/auth/loginByUserId",
//
//            "/v3/api-docs/**",
//            "/swagger-ui.html",
//            "/swagger-ui/**",
//            "/swagger-resources/**",
//            "/webjars/**"
//    };
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//                .cors(Customizer.withDefaults())
//                .csrf(AbstractHttpConfigurer::disable)
//
//                .authorizeHttpRequests(auth -> auth
//                        // 必须放行所有 OPTIONS，不然前端永远 503
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//
//                        // 白名单
//                        .requestMatchers(
//                                "/auth/register",
//                                "/auth/login",
//                                "/auth/loginByUserId",
//                                "/auth/refresh",
//                                "/auth/logout"
//                        ).permitAll()
//                        .requestMatchers(
//                                "/v3/api-docs/**",
//                                "/*/v3/api-docs/**",
//                                "/swagger-ui/**",
//                                "/swagger-ui.html",
//                                "/swagger-resources/**",
//                                "/webjars/**"
//                        ).permitAll()
//
//                        // 其他全部需要 JWT
//                        .anyRequest().authenticated()
//                )
//
//                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//
//                .logout(logout -> logout
//                        .logoutUrl("/auth/logout")
//                        .addLogoutHandler(logoutHandler)
//                        .logoutSuccessHandler((req, res, auth) ->
//                                SecurityContextHolder.clearContext()
//                        )
//                );
//
//        return http.build();
//    }
//
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//}
