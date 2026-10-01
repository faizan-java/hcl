package com.careconnect.api;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class ApiModels {
  private ApiModels() {}

  public record Envelope<T>(T data) {}

  public record SessionUser(String id, String name, String email, String role) {}

  public record AuthResponse(String token, SessionUser user) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record RegisterRequest(
      @NotBlank @Size(min = 2, max = 100) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 128) String password) {}

  public record AppointmentDto(
      String id,
      String name,
      String specialty,
      String date,
      String time,
      Instant startsAt,
      String status,
      String initials,
      String color,
      String location) {}

  public record AppointmentRequest(
      @NotBlank @Size(max = 80) String specialty,
      String providerId,
      @NotNull Instant startsAt,
      @Size(max = 160) String location) {}

  public record AppointmentStatus(@NotBlank String status) {}

  public record ProviderDto(String id, String name) {}

  public record MedicationDto(
      String id, String name, String dosage, String instructions, int active) {}

  public record InvoiceDto(
      String id,
      String invoiceNumber,
      String description,
      long amountCents,
      String status,
      LocalDate dueAt,
      String patientName) {}

  public record HealthRecordDto(
      String id,
      String title,
      String summary,
      Instant recordedAt,
      String patientName,
      String patientId) {}

  public record VitalDto(
      String id, String metric, double value, String unit, String status, Instant recordedAt) {}

  public record OverviewDto(
      AppointmentCounts appointments, int patients, int providers, String role) {}

  public record AppointmentCounts(long total, long pending, long confirmed) {}

  public record AdminUserDto(
      String id, String name, String email, String role, Instant createdAt) {}

  public record BodyComplaintRequest(
      @NotBlank @Size(max = 120) String bodyPart,
      @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]+") @Size(max = 80) String bodyPartId,
      @NotBlank @Size(min = 2, max = 120) String symptom,
      @Min(1) @Max(10) int painLevel,
      @NotBlank @Size(max = 80) String duration,
      @Size(max = 2000) String description) {}

  public record BodyComplaintUpdate(
      @Size(max = 1200) String diagnosis,
      @Size(max = 3000) String doctorNotes,
      @Size(max = 2000) String treatmentRecommendation,
      @Pattern(regexp = "Open|Under Review|Resolved") String status) {}

  public record BodyComplaintDto(
      String id,
      String patientId,
      String bodyPart,
      String bodyPartId,
      String symptom,
      int painLevel,
      String duration,
      String description,
      String diagnosis,
      String doctorNotes,
      String treatmentRecommendation,
      String status,
      Instant createdAt,
      Instant updatedAt) {}

  public record CarePatientDto(String id, String name) {}

  public record DoctorBodyDto(CarePatientDto patient, List<BodyComplaintDto> complaints) {}

  public record AuditDto(
      Long id, String actorId, String action, String entity, String entityId, Instant createdAt) {}
}
