package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "health_records")
@Getter
@Setter
@NoArgsConstructor
public class HealthRecord {
  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "provider_id", nullable = false)
  private User provider;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 4000)
  private String summary;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt = Instant.now();
}
