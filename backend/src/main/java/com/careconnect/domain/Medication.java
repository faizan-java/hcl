package com.careconnect.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
public class Medication {
  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 80)
  private String dosage;

  @Column(nullable = false, length = 500)
  private String instructions = "";

  @Convert(converter = NumericBooleanConverter.class)
  @Column(nullable = false)
  private boolean active = true;

  public Medication(String id, User patient, String name, String dosage, String instructions) {
    this.id = id;
    this.patient = patient;
    this.name = name;
    this.dosage = dosage;
    this.instructions = instructions;
  }
}
