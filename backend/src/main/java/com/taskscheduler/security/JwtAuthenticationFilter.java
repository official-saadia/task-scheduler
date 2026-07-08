package com.taskscheduler.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that intercepts every incoming HTTP request.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution
 * per request within a single request thread. Extracts and validates the
 * JWT token from the {@code Authorization} header and sets the authenticated
 * principal in the {@link SecurityContextHolder} if the token is valid.</p>
 *
 * <p>Expected header format:</p>
 * <pre>
 *   Authorization: Bearer {@code <token>}
 * </pre>
 *
 * <p>Filter flow:</p>
 * <ol>
 *   <li>Extract JWT token from the {@code Authorization} header</li>
 *   <li>Validate the token using {@link JwtTokenProvider}</li>
 *   <li>Load user details from {@link UserDetailsService}</li>
 *   <li>Set authentication in {@link SecurityContextHolder}</li>
 *   <li>Continue the filter chain</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Intercepts the request, extracts and validates the JWT token,
     * and sets the authentication context if the token is valid.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the {@code Authorization} header of the request.
     *
     * <p>Expects the header value to start with {@code "Bearer "}.
     * Returns {@code null} if the header is missing or not in the expected format.</p>
     *
     * @param request the incoming HTTP request
     * @return the raw JWT token string, or {@code null} if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
