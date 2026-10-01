package com.careconnect.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final ObjectMapper json;

  public JwtAuthenticationFilter(JwtService jwt, ObjectMapper json) {
    this.jwt = jwt;
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String auth = req.getHeader("Authorization");
    if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7))
      try {
        AppPrincipal user = jwt.parse(auth.substring(7));
        var token =
            new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        SecurityContextHolder.getContext().setAuthentication(token);
      } catch (JwtException | IllegalArgumentException ex) {
        SecurityContextHolder.clearContext();
        res.setStatus(401);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(
            res.getOutputStream(), java.util.Map.of("error", "Session expired. Sign in again."));
        return;
      }
    chain.doFilter(req, res);
  }
}
