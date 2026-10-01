package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vitals")
@Getter
@Setter
@NoArgsConstructor
public class Vital {
  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @Column(nullable = false, length = 100)
  private String metric;

  @Column(name = "vital_value", nullable = false)
  private double value;

  @Column(nullable = false, length = 32)
  private String unit;

  @Column(name = "range_status", nullable = false, length = 24)
  private String rangeStatus = "Normal";

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt = Instant.now();

  public Vital(
      String id,
      User patient,
      String metric,
      double value,
      String unit,
      String status,
      Instant at) {
    this.id = id;
    this.patient = patient;
    this.metric = metric;
    this.value = value;
    this.unit = unit;
    this.rangeStatus = status;
    this.recordedAt = at;
  }
}
