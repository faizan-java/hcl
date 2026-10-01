package com.careconnect.api;

import com.careconnect.api.ApiModels.*;
import com.careconnect.config.AppPrincipal;
import com.careconnect.service.CareService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CareController {
  private final CareService care;

  public CareController(CareService care) {
    this.care = care;
  }

  private AppPrincipal principal() {
    return (AppPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  public record RecordRequest(
      @NotBlank String patientId,
      @NotBlank @Size(min = 2, max = 120) String title,
      @NotBlank @Size(min = 2, max = 4000) String summary) {}

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of("status", "ok", "service", "careconnect-api", "time", java.time.Instant.now());
  }

  @GetMapping("/me")
  public Map<String, Object> me() {
    return Map.of(
        "user",
        new SessionUser(
            principal().id(), principal().name(), principal().email(), principal().role()));
  }

  @GetMapping("/providers")
  public Envelope<List<ProviderDto>> providers() {
    return new Envelope<>(care.providers());
  }

  @GetMapping("/appointments")
  public Envelope<List<AppointmentDto>> appointments() {
    return new Envelope<>(care.appointments(principal()));
  }

  @PostMapping("/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  public Envelope<AppointmentDto> createAppointment(@Valid @RequestBody AppointmentRequest r) {
    return new Envelope<>(care.createAppointment(principal(), r));
  }

  @PatchMapping("/appointments/{id}/status")
  public Envelope<Map<String, String>> status(
      @PathVariable String id, @Valid @RequestBody AppointmentStatus s) {
    care.updateAppointment(principal(), id, s.status());
    return new Envelope<>(Map.of("id", id, "status", s.status()));
  }

  @GetMapping("/medications")
  public Envelope<List<MedicationDto>> medications() {
    return new Envelope<>(care.medications(principal()));
  }

  @GetMapping("/records")
  public Envelope<List<HealthRecordDto>> records() {
    return new Envelope<>(care.records(principal()));
  }

  @PostMapping("/records")
  @ResponseStatus(HttpStatus.CREATED)
  public Envelope<HealthRecordDto> addRecord(@Valid @RequestBody RecordRequest r) {
    return new Envelope<>(care.createRecord(principal(), r.patientId(), r.title(), r.summary()));
  }

  @GetMapping("/vitals")
  public Envelope<List<VitalDto>> vitals() {
    return new Envelope<>(care.vitals(principal()));
  }

  @GetMapping("/billing/invoices")
  public Envelope<List<InvoiceDto>> invoices() {
    return new Envelope<>(care.invoices(principal()));
  }

  @GetMapping("/overview")
  public Envelope<OverviewDto> overview() {
    return new Envelope<>(care.overview(principal()));
  }

  @GetMapping("/admin/users")
  public Envelope<List<AdminUserDto>> users(@RequestParam(required = false) String role) {
    return new Envelope<>(care.adminUsers(principal(), role));
  }

  @GetMapping("/admin/audit")
  public Envelope<List<AuditDto>> audit() {
    return new Envelope<>(care.auditLog(principal()));
  }

  @PostMapping("/patient/body-complaints")
  @ResponseStatus(HttpStatus.CREATED)
  public Envelope<BodyComplaintDto> complaint(@Valid @RequestBody BodyComplaintRequest r) {
    return new Envelope<>(care.createComplaint(principal(), r));
  }

  @GetMapping("/patient/body-complaints/{patientId}")
  public Envelope<List<BodyComplaintDto>> complaints(@PathVariable String patientId) {
    return new Envelope<>(care.patientComplaints(principal(), patientId));
  }

  @DeleteMapping("/patient/body-complaints/{complaintId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComplaint(@PathVariable String complaintId) {
    care.deleteComplaint(principal(), complaintId);
  }

  @GetMapping("/doctor/patients")
  public Envelope<List<CarePatientDto>> carePatients() {
    return new Envelope<>(care.carePatients(principal()));
  }

  @GetMapping("/doctor/patients/{patientId}/body")
  public Envelope<DoctorBodyDto> doctorBody(@PathVariable String patientId) {
    return new Envelope<>(care.doctorBody(principal(), patientId));
  }

  @PutMapping("/doctor/body-complaints/{complaintId}")
  public Envelope<BodyComplaintDto> review(
      @PathVariable String complaintId, @Valid @RequestBody BodyComplaintUpdate update) {
    return new Envelope<>(care.reviewComplaint(principal(), complaintId, update));
  }
}
