package com.careconnect.service;

import com.careconnect.api.ApiModels.*;
import com.careconnect.config.*;
import com.careconnect.domain.User;
import com.careconnect.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final AuditService audit;

  public AuthService(
      UserRepository users, PasswordEncoder passwords, JwtService jwt, AuditService audit) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    this.audit = audit;
  }

  @Transactional
  public AuthResponse login(LoginRequest req) {
    User u =
        users
            .findByEmailIgnoreCase(req.email().trim())
            .orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
    if (!passwords.matches(req.password(), u.getPasswordHash()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    audit.record(u.getId(), "auth.login", "user", u.getId());
    return response(u);
  }

  @Transactional
  public AuthResponse register(RegisterRequest req) {
    if (req.password().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Password must be no longer than 72 UTF-8 bytes.");
    String email = req.email().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email))
      throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists.");
    User u =
        users.save(
            new User(
                "pat-" + UUID.randomUUID(),
                req.name().trim(),
                email,
                "Patient",
                passwords.encode(req.password())));
    audit.record(u.getId(), "account.registered", "user", u.getId());
    return response(u);
  }

  @Transactional
  public AuthResponse demo(String role) {
    if (!List.of("Patient", "Provider", "Admin").contains(role))
      throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown demo role.");
    User u =
        users.findAllByRoleOrderByName(role).stream()
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demo account not found."));
    audit.record(u.getId(), "auth.demo_login", "user", u.getId());
    return response(u);
  }

  public AuthResponse response(User u) {
    SessionUser s = new SessionUser(u.getId(), u.getName(), u.getEmail(), u.getRole());
    return new AuthResponse(jwt.issue(new AppPrincipal(s.id(), s.name(), s.email(), s.role())), s);
  }
}
