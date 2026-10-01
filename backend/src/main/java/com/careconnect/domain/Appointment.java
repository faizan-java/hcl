package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "appointments",
    indexes = {
      @Index(name = "appointments_patient_idx", columnList = "patient_id,starts_at"),
      @Index(name = "appointments_provider_idx", columnList = "provider_id,starts_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class Appointment {
  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "provider_id", nullable = false)
  private User provider;

  @Column(nullable = false, length = 80)
  private String specialty;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(nullable = false, length = 16)
  private String status = "Pending";

  @Column(nullable = false, length = 160)
  private String location = "Northstar Medical Center";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
