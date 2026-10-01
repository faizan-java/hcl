package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "actor_id", length = 80)
  private String actorId;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(nullable = false, length = 100)
  private String entity;

  @Column(name = "entity_id", length = 80)
  private String entityId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public AuditEntry(String actorId, String action, String entity, String entityId) {
    this.actorId = actorId;
    this.action = action;
    this.entity = entity;
    this.entityId = entityId;
  }
}
