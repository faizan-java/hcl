package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
  @Id
  @Column(length = 80)
  private String id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false, length = 16)
  private String role;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public User(String id, String name, String email, String role, String passwordHash) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.role = role;
    this.passwordHash = passwordHash;
  }
}
