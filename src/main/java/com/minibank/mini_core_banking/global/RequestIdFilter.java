package com.minibank.mini_core_banking.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-ID";
    private static final int MAX_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));

        RequestIdHolder.set(requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestIdHolder.clear();
        }
    }

    private String resolveRequestId(String requestId) {
        if (isValid(requestId)) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private boolean isValid(String requestId) {
        return requestId != null
                && !requestId.isBlank()
                && requestId.length() <= MAX_LENGTH
                && requestId.chars().allMatch(ch -> ch >= 33 && ch <= 126);
    }
}
