package com.devanshbhosale.channelhub.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.devanshbhosale.channelhub.domain.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Server-enforced read-only mode for the demo admin: when the principal is a
 * demo user hitting /api/admin with anything other than GET/HEAD, answer 403
 * before the request reaches any controller.
 */
public class DemoAdminReadOnlyFilter extends OncePerRequestFilter {

    static final String DEMO_BLOCKED_BODY = "{\"error\":\"Demo mode — actions disabled\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isDemoAdminWrite(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(DEMO_BLOCKED_BODY);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDemoAdminWrite(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof User user && user.isDemo())) {
            return false;
        }
        boolean adminPath = request.getRequestURI().startsWith("/api/admin");
        boolean writeMethod = !HttpMethod.GET.matches(request.getMethod())
                && !HttpMethod.HEAD.matches(request.getMethod());
        return adminPath && writeMethod;
    }
}
