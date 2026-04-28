package com.example.mainbackend.security;

import com.example.mainbackend.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    final private JwtUtil jwtUtil;
    final private CustomUserDetailsService customUserDetailsService;
    final private TokenBlacklistService tokenBlacklistService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login") || path.equals("/api/auth/refresh-token") || path.startsWith("/api/public") || path.equals("/api/status");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token, header = request.getHeader(JwtProperties.HEADER_STRING);

        if (header != null && header.startsWith(JwtProperties.TOKEN_PREFIX))
            token = header.substring(JwtProperties.TOKEN_PREFIX.length());
        else
            token = request.getParameter("token");

        // Strict approach: if no token is provided, return 401 Unauthorized
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing authentication token");
            return;
        }

        if (tokenBlacklistService.isTokenBlacklisted(jwtUtil.extractJWTID(token))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token is blacklisted");
            return;
        }

        try {
            String nationalId = jwtUtil.extractNationalId(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(nationalId);

            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // If token validation fails, return 401 Unauthorized
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
        } catch (UsernameNotFoundException | AuthenticationCredentialsNotFoundException ex) {
            // Return 401 Unauthorized for invalid credentials
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(ex.getMessage());
            return;
            // any other exception, such as token expiration or invalid signature,
        } catch (Exception ex) {
            // For other exceptions, return 500 Internal Server Error
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("An error occurred while processing the token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
