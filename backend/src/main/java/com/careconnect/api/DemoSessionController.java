package com.careconnect.api;

import com.careconnect.api.ApiModels.AuthResponse;
import com.careconnect.service.AuthService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Profile("!production")
public class DemoSessionController {
  private final AuthService auth;

  public DemoSessionController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/demo-session/{role}")
  public AuthResponse demo(@PathVariable String role) {
    return auth.demo(role);
  }
}
