package com.example.common.filter;

import com.example.common.repository.TokenRepository;
import com.example.common.session.UserSession;
import com.example.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    // 不需要 JWT 的白名单路径（与 SecurityConfig 中保持一致）
    private static final String[] WHITE_LIST = {
            "/auth/register",
            "/auth/login",
            "/auth/logout",
            "/auth/refresh",
            "/auth/loginByUserId",
            "/register",
            "/login",
            "/logout",
            "/refresh",
            "/loginByUserId",
            "/room/**",
            "/reservation/**",
            "/v3/api-docs/**",
            "/**/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private final TokenRepository tokenRepository;

    public JwtAuthenticationFilter(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        String uri = request.getRequestURI();

        // 任何包含 v3/api-docs 的路径都放行，避免经网关转发时前面多一层前缀
        if (path.contains("v3/api-docs") || uri.contains("v3/api-docs")) {
            logger.info("Skipping JWT filter for OpenAPI path: " + path);
            return true;
        }

        for (String pattern : WHITE_LIST) {
            if (ANT_PATH_MATCHER.match(pattern, path)) {
                logger.info("Skipping JWT filter for white-list path: " + path);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        logger.info("Processing jwt authentication request");

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasLength(token) || !token.startsWith("Bearer ")) {
            logger.error("Invalid or expired token");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        token = token.substring(7);
        logger.info("Token: " + token);

        if (!tokenRepository.hasToken(token)) {
            logger.error("The token is expired or invalid");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        Claims claims;

        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            logger.error("Invalid or expired token");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        if ("2".equals(claims.get("state"))) {
            logger.error("User account is disabled");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        UserSession userDetails = new UserSession(claims, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        logger.info("User authenticated successfully");

        filterChain.doFilter(request, response);
    }

}
