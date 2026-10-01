package com.careconnect.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @Column(name = "invoice_number", nullable = false, unique = true, length = 32)
  private String invoiceNumber;

  @Column(nullable = false, length = 250)
  private String description;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "due_at", nullable = false)
  private LocalDate dueAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
