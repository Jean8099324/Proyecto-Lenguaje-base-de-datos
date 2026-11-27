package com.rescatevial.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Traza el flujo HTTP de autenticación sin registrar credenciales ni tokens. */
@Component
public class RequestTimingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    public static final String TRACE_ID_ATTRIBUTE = "rescatevial.authTraceId";
    private static final String TRACE_ID_SESSION_ATTRIBUTE = "rescatevialAuthTraceId";
    private static final Set<String> TRACE_PATHS = Set.of(
            "/", "/login", "/logout", "/dashboard", "/cliente/inicio", "/error/403");

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !TRACE_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long started = System.nanoTime();
        String path = safePath(request);
        String traceId = traceId(request);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        String requestedSession = fingerprint(request.getRequestedSessionId());
        log.info("AUTH_FLOW event=request method={} path={} traceId={} requestedSession={} requestedSessionValid={} requestHasSessionCookie={} authBefore={}",
                request.getMethod(), path, traceId, requestedSession,
                request.isRequestedSessionIdValid(), hasSessionCookie(request), authenticationState());
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            HttpSession session = request.getSession(false);
            log.info("AUTH_FLOW event=response method={} path={} status={} location={} traceId={} requestedSession={} activeSession={} responseSetsSessionCookie={} authAfter={} elapsedMs={}",
                    request.getMethod(), path, response.getStatus(), safeLocation(response.getHeader("Location")),
                    traceId, requestedSession, fingerprint(session == null ? null : session.getId()),
                    setsSessionCookie(response), authenticationState(), elapsedMs);
        }
    }

    private String traceId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String traceId = (String) session.getAttribute(TRACE_ID_SESSION_ATTRIBUTE);
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath())
                || traceId == null) {
            traceId = UUID.randomUUID().toString();
            session.setAttribute(TRACE_ID_SESSION_ATTRIBUTE, traceId);
        }
        return traceId;
    }

    private String safePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!"/login".equals(path)) return path;
        StringBuilder safe = new StringBuilder(path);
        String query = request.getQueryString();
        if (query != null) {
            for (String flag : new String[]{"logout", "error", "csrfError"}) {
                if (query.matches("(^|.*&)" + flag + "=true(&.*|$)")) {
                    safe.append(safe.indexOf("?") < 0 ? '?' : '&').append(flag).append("=true");
                }
            }
        }
        return safe.toString();
    }

    private String safeLocation(String location) {
        if (location == null) return "-";
        String path = location;
        int scheme = path.indexOf("://");
        if (scheme >= 0) {
            int slash = path.indexOf('/', scheme + 3);
            path = slash >= 0 ? path.substring(slash) : "/";
        }
        int queryStart = path.indexOf('?');
        if (queryStart < 0) return path;
        String base = path.substring(0, queryStart);
        String query = path.substring(queryStart + 1);
        if (!"/login".equals(base)) return base + "?[omitted]";
        StringBuilder safe = new StringBuilder(base);
        for (String flag : new String[]{"logout", "error", "csrfError"}) {
            if (query.matches("(^|.*&)" + flag + "=true(&.*|$)")) {
                safe.append(safe.indexOf("?") < 0 ? '?' : '&').append(flag).append("=true");
            }
        }
        return safe.toString();
    }

    private boolean hasSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return false;
        for (var cookie : request.getCookies()) {
            if ("JSESSIONID".equals(cookie.getName())) return true;
        }
        return false;
    }

    private boolean setsSessionCookie(HttpServletResponse response) {
        return response.getHeaders("Set-Cookie").stream().anyMatch(value -> value.startsWith("JSESSIONID="));
    }

    private String authenticationState() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return "anonymous";
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted()
                .reduce((left, right) -> left + "," + right).orElse("authenticated");
    }

    private String fingerprint(String value) {
        if (value == null || value.isBlank()) return "-";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }
}
