package com.careconnect.service;

import com.careconnect.domain.*;
import com.careconnect.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class SeedData implements CommandLineRunner {
  private final UserRepository users;
  private final AppointmentRepository appointments;
  private final MedicationRepository medications;
  private final HealthRecordRepository records;
  private final InvoiceRepository invoices;
  private final VitalRepository vitals;
  private final PasswordEncoder encoder;
  private final boolean production;

  public SeedData(
      UserRepository users,
      AppointmentRepository appointments,
      MedicationRepository medications,
      HealthRecordRepository records,
      InvoiceRepository invoices,
      VitalRepository vitals,
      PasswordEncoder encoder,
      @Value("${careconnect.production:false}") boolean production) {
    this.users = users;
    this.appointments = appointments;
    this.medications = medications;
    this.records = records;
    this.invoices = invoices;
    this.vitals = vitals;
    this.encoder = encoder;
    this.production = production;
  }

  @Override
  @Transactional
  public void run(String... args) {
    List<String> demoIds =
        List.of("pat-1001", "pat-1002", "pat-1003", "pat-1004", "pat-1005", "prv-0204", "adm-0001");
    if (production) {
      if (demoIds.stream().anyMatch(users::existsById))
        throw new IllegalStateException(
            "Demo accounts exist in the Oracle production schema. Use a clean database before deployment.");
      return;
    }
    if (users.existsById("pat-1001")) return;
    String hash = encoder.encode("CareConnectDemo2026!");
    List<User> people =
        List.of(
            new User("pat-1001", "Aarav Sharma", "patient@careconnect.local", "Patient", hash),
            new User("pat-1002", "Maya Patel", "maya@careconnect.local", "Patient", hash),
            new User("pat-1003", "Noah Williams", "noah@careconnect.local", "Patient", hash),
            new User("pat-1004", "Emma Wilson", "emma@careconnect.local", "Patient", hash),
            new User("pat-1005", "Liam Chen", "liam@careconnect.local", "Patient", hash),
            new User(
                "prv-0204", "Dr. Sarah Johnson", "provider@careconnect.local", "Provider", hash),
            new User("adm-0001", "Morgan Lee", "admin@careconnect.local", "Admin", hash));
    users.saveAllAndFlush(people);
    User patient = users.findById("pat-1001").orElseThrow(),
        provider = users.findById("prv-0204").orElseThrow();
    Instant now = Instant.now();
    seedAppointment(
        "apt-2001",
        "pat-1001",
        "prv-0204",
        "Cardiology",
        now.plus(Duration.ofHours(3)),
        "Confirmed",
        "Room 204 · Northstar Medical Center");
    seedAppointment(
        "apt-2002",
        "pat-1001",
        "prv-0204",
        "Cardiology",
        now.plus(Duration.ofDays(3)),
        "Confirmed",
        "Northstar Medical Center");
    seedAppointment(
        "apt-2003",
        "pat-1001",
        "prv-0204",
        "Dermatology",
        now.plus(Duration.ofDays(7)),
        "Pending",
        "Northstar Medical Center");
    seedAppointment(
        "apt-2011",
        "pat-1002",
        "prv-0204",
        "Cardiology follow-up",
        now.plus(Duration.ofHours(2)),
        "Confirmed",
        "Room 204 · Northstar Medical Center");
    seedAppointment(
        "apt-2012",
        "pat-1003",
        "prv-0204",
        "Blood pressure review",
        now.plus(Duration.ofHours(4)),
        "Confirmed",
        "Room 206 · Northstar Medical Center");
    seedAppointment(
        "apt-2013",
        "pat-1004",
        "prv-0204",
        "New patient consultation",
        now.plus(Duration.ofHours(5)),
        "Pending",
        "Room 204 · Northstar Medical Center");
    seedAppointment(
        "apt-2014",
        "pat-1005",
        "prv-0204",
        "Medication follow-up",
        now.plus(Duration.ofHours(6)),
        "Confirmed",
        "Room 204 · Northstar Medical Center");
    medications.saveAll(
        List.of(
            new Medication("med-1001", patient, "Metformin", "500 mg", "After breakfast"),
            new Medication("med-1002", patient, "Vitamin D3", "1000 IU", "With lunch"),
            new Medication("med-1003", patient, "Lisinopril", "10 mg", "Evening")));
    HealthRecord r = new HealthRecord();
    r.setId("rec-1001");
    r.setPatient(patient);
    r.setProvider(provider);
    r.setTitle("Annual wellness check");
    r.setSummary("Routine check-up completed. Follow-up recommended in one year.");
    r.setRecordedAt(Instant.parse("2026-09-24T09:00:00Z"));
    records.save(r);
    Invoice invoice = new Invoice();
    invoice.setId("inv-2084");
    invoice.setPatient(patient);
    invoice.setInvoiceNumber("INV-2084");
    invoice.setDescription("Annual wellness visit");
    invoice.setAmountCents(12500);
    invoice.setStatus("Open");
    invoice.setDueAt(LocalDate.now().plusDays(15));
    invoices.save(invoice);
    Instant recorded = Instant.parse("2026-09-24T09:00:00Z");
    vitals.saveAll(
        List.of(
            new Vital("vit-1001", patient, "Heart rate", 72, "bpm", "Normal", recorded),
            new Vital(
                "vit-1002", patient, "Blood pressure systolic", 120, "mmHg", "Normal", recorded),
            new Vital(
                "vit-1003", patient, "Blood pressure diastolic", 80, "mmHg", "Normal", recorded),
            new Vital("vit-1004", patient, "Body temperature", 98.6, "°F", "Normal", recorded),
            new Vital("vit-1005", patient, "Oxygen saturation", 98, "%", "Normal", recorded)));
  }

  private void seedAppointment(
      String id,
      String patientId,
      String providerId,
      String specialty,
      Instant startsAt,
      String status,
      String location) {
    Appointment a = new Appointment();
    a.setId(id);
    a.setPatient(users.findById(patientId).orElseThrow());
    a.setProvider(users.findById(providerId).orElseThrow());
    a.setSpecialty(specialty);
    a.setStartsAt(startsAt);
    a.setStatus(status);
    a.setLocation(location);
    appointments.save(a);
  }
}
