package com.example.common.handler;

import com.example.common.repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class UserLogoutHandler implements LogoutHandler {

    @Autowired
    private TokenRepository tokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String token = request.getHeader("Authorization");
        if (!StringUtils.hasLength(token)) {
            return;
        }
        tokenRepository.removeToken(token);
        response.setStatus(HttpStatus.NO_CONTENT.value());
        SecurityContextHolder.clearContext();
        log.info("User logged out.");
    }
}
