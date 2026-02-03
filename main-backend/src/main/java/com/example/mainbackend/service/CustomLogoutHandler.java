package com.example.mainbackend.service;

import com.example.mainbackend.config.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

import static com.example.mainbackend.config.JwtProperties.*;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutSuccessHandler {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            String authHeader = request.getHeader(HEADER_STRING);

            if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
                String token = authHeader.substring(TOKEN_PREFIX.length());

                String jwtID = jwtUtil.extractJWTID(token);
                Date expiration = jwtUtil.extractExpiration(token);

                tokenBlacklistService.blacklistToken(jwtID, expiration);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("Logout successful. (Token has been blacklisted.)");
            }
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("No access token provided.");
            }

        } catch (Exception exServerErr) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("An error occurred while processing the logout request.\n" + exServerErr.getMessage());
            } catch (IOException exIO) {
                throw new RuntimeException(exIO);
            }
        }
    }
}
