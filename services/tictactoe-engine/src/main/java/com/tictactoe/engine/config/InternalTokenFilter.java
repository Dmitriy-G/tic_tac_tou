package com.tictactoe.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.ErrorResponse;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";
    private static final List<String> EXEMPT_PREFIXES = List.of("/actuator");

    private final String expected;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(@Value("${internal.token}") String expected, ObjectMapper objectMapper) {
        this.expected = expected;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void assertConfigured() {
        if (expected == null || expected.isBlank()) {
            throw new IllegalStateException("internal.token must be set (env INTERNAL_TOKEN)");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isExempt(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String presented = request.getHeader(HEADER);
        // MessageDigest.isEqual, not String.equals: constant-time comparison so response timing
        // can't be used to guess the token byte by byte.
        if (presented == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
            writeUnauthorized(response, request);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isExempt(String uri) {
        return EXEMPT_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request) throws IOException {
        ErrorResponse body = ErrorResponse.of(ErrorCode.UNAUTHORIZED, "Missing or invalid internal token",
                request.getRequestURI(), MDC.get(CorrelationIdFilter.MDC_KEY));
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
