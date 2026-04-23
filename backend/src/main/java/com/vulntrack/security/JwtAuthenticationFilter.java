package com.vulntrack.security;

import com.vulntrack.entity.User;
import com.vulntrack.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(header.substring(7));
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType == null || !"access".equals(tokenType)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findById(Long.parseLong(claims.getSubject()))
                    .filter(user -> Boolean.TRUE.equals(user.getActive()))
                    .ifPresent(user -> authenticate(request, user));
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, User user) {
        SecurityUser principal = new SecurityUser(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
