package com.gaokao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class SecurityResponseFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = validRequestId(request.getHeader("X-Request-ID"));
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        response.setHeader("X-Gaokao-Simulation", "For simulation only; not an official admission result");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (request.getRequestURI().startsWith("/api/auth/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        filterChain.doFilter(request, response);
    }

    private String validRequestId(String candidate) {
        if (candidate != null && candidate.matches("[A-Za-z0-9-]{8,64}")) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
