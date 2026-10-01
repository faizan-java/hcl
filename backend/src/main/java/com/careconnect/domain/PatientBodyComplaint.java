package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "patient_body_complaints",
    indexes = @Index(name = "body_complaints_patient_idx", columnList = "patient_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class PatientBodyComplaint {
  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @Column(name = "body_part", nullable = false, length = 120)
  private String bodyPart;

  @Column(name = "body_part_id", nullable = false, length = 80)
  private String bodyPartId;

  @Column(nullable = false, length = 120)
  private String symptom;

  @Column(name = "pain_level", nullable = false)
  private int painLevel;

  @Column(nullable = false, length = 80)
  private String duration;

  @Column(nullable = false, length = 2000)
  private String description = " ";

  @Column(nullable = false, length = 1200)
  private String diagnosis = " ";

  @Column(name = "doctor_notes", nullable = false, length = 3000)
  private String doctorNotes = " ";

  @Column(name = "treatment_recommendation", nullable = false, length = 2000)
  private String treatmentRecommendation = " ";

  @Column(nullable = false, length = 20)
  private String status = "Open";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
