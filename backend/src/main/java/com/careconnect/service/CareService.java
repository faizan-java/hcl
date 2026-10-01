package com.careconnect.service;

import com.careconnect.api.ApiModels.*;
import com.careconnect.config.AppPrincipal;
import com.careconnect.domain.*;
import com.careconnect.repository.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareService {
  private final UserRepository users;
  private final AppointmentRepository appointments;
  private final MedicationRepository medications;
  private final HealthRecordRepository records;
  private final InvoiceRepository invoices;
  private final VitalRepository vitals;
  private final PatientBodyComplaintRepository complaints;
  private final AuditRepository audits;
  private final AuditService audit;

  public CareService(
      UserRepository users,
      AppointmentRepository appointments,
      MedicationRepository medications,
      HealthRecordRepository records,
      InvoiceRepository invoices,
      VitalRepository vitals,
      PatientBodyComplaintRepository complaints,
      AuditRepository audits,
      AuditService audit) {
    this.users = users;
    this.appointments = appointments;
    this.medications = medications;
    this.records = records;
    this.invoices = invoices;
    this.vitals = vitals;
    this.complaints = complaints;
    this.audits = audits;
    this.audit = audit;
  }

  private User user(String id) {
    return users
        .findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
  }

  private static String initials(String name) {
    return Arrays.stream(name.trim().split("\\s+"))
        .map(s -> s.substring(0, 1))
        .limit(2)
        .reduce("", String::concat)
        .toUpperCase(Locale.ROOT);
  }

  private AppointmentDto appointmentDto(Appointment a, AppPrincipal caller) {
    String counterpart =
        caller.role().equals("Patient") ? a.getProvider().getName() : a.getPatient().getName();
    ZonedDateTime t = a.getStartsAt().atZone(ZoneOffset.UTC);
    return new AppointmentDto(
        a.getId(),
        counterpart,
        a.getSpecialty(),
        t.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)),
        t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)),
        a.getStartsAt(),
        a.getStatus(),
        initials(counterpart),
        "lavender",
        a.getLocation());
  }

  @Transactional(readOnly = true)
  public List<AppointmentDto> appointments(AppPrincipal p) {
    List<Appointment> rows =
        p.role().equals("Admin")
            ? appointments.findAllByOrderByStartsAt()
            : p.role().equals("Provider")
                ? appointments.findByProviderIdOrderByStartsAt(p.id())
                : appointments.findByPatientIdOrderByStartsAt(p.id());
    return rows.stream().map(a -> appointmentDto(a, p)).toList();
  }

  @Transactional
  public AppointmentDto createAppointment(AppPrincipal p, AppointmentRequest req) {
    require(p, "Patient");
    Instant start = req.startsAt();
    if (start.isBefore(Instant.now().minusSeconds(60)))
      throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a future appointment time.");
    User provider =
        req.providerId() == null || req.providerId().isBlank()
            ? users.findFirstByRoleOrderByName("Provider").orElse(null)
            : users.findByIdForUpdate(req.providerId()).orElse(null);
    if (provider == null || !provider.getRole().equals("Provider"))
      throw new ApiException(HttpStatus.BAD_REQUEST, "The selected provider is unavailable.");
    if (!appointments
        .findConflicts(provider.getId(), start.minusSeconds(1800), start.plusSeconds(1800))
        .isEmpty())
      throw new ApiException(HttpStatus.CONFLICT, "That appointment time is no longer available.");
    Appointment a = new Appointment();
    a.setId("apt-" + UUID.randomUUID());
    a.setPatient(user(p.id()));
    a.setProvider(provider);
    a.setSpecialty(req.specialty().trim());
    a.setStartsAt(start);
    a.setStatus("Pending");
    if (req.location() != null && !req.location().isBlank()) a.setLocation(req.location());
    a = appointments.save(a);
    audit.record(p.id(), "appointment.requested", "appointment", a.getId());
    return appointmentDto(a, p);
  }

  @Transactional
  public void updateAppointment(AppPrincipal p, String id, String status) {
    if (!List.of("Pending", "Confirmed", "Cancelled", "Completed").contains(status))
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid appointment status.");
    Appointment a =
        appointments
            .findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found."));
    boolean isPatient = p.role().equals("Patient") && a.getPatient().getId().equals(p.id());
    boolean isProvider = p.role().equals("Provider") && a.getProvider().getId().equals(p.id());
    if (!p.role().equals("Admin") && !isPatient && !isProvider)
      throw new ApiException(HttpStatus.FORBIDDEN, "You cannot change this appointment.");

    if (p.role().equals("Admin")) {
      a.setStatus(status);
      audit.record(p.id(), "appointment." + status.toLowerCase(Locale.ROOT), "appointment", id);
      return;
    }

    boolean canTransition =
        isPatient
            ? status.equals("Cancelled")
                && List.of("Pending", "Confirmed").contains(a.getStatus())
                && a.getStartsAt().isAfter(Instant.now())
            : isProvider
                && ((a.getStatus().equals("Pending") && status.equals("Confirmed"))
                    || (List.of("Pending", "Confirmed").contains(a.getStatus())
                        && status.equals("Cancelled"))
                    || (a.getStatus().equals("Confirmed")
                        && status.equals("Completed")
                        && !a.getStartsAt().isAfter(Instant.now())));
    if (!canTransition)
      throw new ApiException(
          HttpStatus.CONFLICT,
          "This appointment cannot move from " + a.getStatus() + " to " + status + ".");

    a.setStatus(status);
    audit.record(p.id(), "appointment." + status.toLowerCase(Locale.ROOT), "appointment", id);
  }

  @Transactional(readOnly = true)
  public List<MedicationDto> medications(AppPrincipal p) {
    require(p, "Patient");
    return medications.findByPatientIdOrderByName(p.id()).stream()
        .map(
            m ->
                new MedicationDto(
                    m.getId(),
                    m.getName(),
                    m.getDosage(),
                    m.getInstructions(),
                    m.isActive() ? 1 : 0))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<HealthRecordDto> records(AppPrincipal p) {
    if (p.role().equals("Admin"))
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "Clinical record access is limited to the patient and assigned care team.");
    if (p.role().equals("Patient"))
      return records.findByPatientIdOrderByRecordedAtDesc(p.id()).stream()
          .map(
              r ->
                  new HealthRecordDto(
                      r.getId(), r.getTitle(), r.getSummary(), r.getRecordedAt(), null, null))
          .toList();
    Set<String> assigned =
        appointments.findByProviderIdAndStatusNotOrderByStartsAt(p.id(), "Cancelled").stream()
            .map(a -> a.getPatient().getId())
            .collect(java.util.stream.Collectors.toSet());
    return records.findByProviderIdOrderByRecordedAtDesc(p.id()).stream()
        .filter(r -> assigned.contains(r.getPatient().getId()))
        .map(
            r ->
                new HealthRecordDto(
                    r.getId(),
                    r.getTitle(),
                    r.getSummary(),
                    r.getRecordedAt(),
                    r.getPatient().getName(),
                    r.getPatient().getId()))
        .toList();
  }

  @Transactional
  public HealthRecordDto createRecord(
      AppPrincipal p, String patientId, String title, String summary) {
    require(p, "Provider");
    if (!appointments.existsByPatientIdAndProviderIdAndStatusNot(patientId, p.id(), "Cancelled"))
      throw new ApiException(
          HttpStatus.FORBIDDEN, "Clinical notes can only be added for patients assigned to you.");
    HealthRecord r = new HealthRecord();
    r.setId("rec-" + UUID.randomUUID());
    r.setPatient(user(patientId));
    r.setProvider(user(p.id()));
    r.setTitle(title.trim());
    r.setSummary(summary.trim());
    r.setRecordedAt(Instant.now());
    r = records.save(r);
    audit.record(p.id(), "record.created", "health_record", r.getId());
    return new HealthRecordDto(
        r.getId(),
        r.getTitle(),
        r.getSummary(),
        r.getRecordedAt(),
        r.getPatient().getName(),
        r.getPatient().getId());
  }

  @Transactional(readOnly = true)
  public List<VitalDto> vitals(AppPrincipal p) {
    require(p, "Patient");
    return vitals.findByPatientIdOrderByRecordedAtDesc(p.id()).stream()
        .map(
            v ->
                new VitalDto(
                    v.getId(),
                    v.getMetric(),
                    v.getValue(),
                    v.getUnit(),
                    v.getRangeStatus(),
                    v.getRecordedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InvoiceDto> invoices(AppPrincipal p) {
    if (!List.of("Patient", "Admin").contains(p.role()))
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "Invoice access is limited to the patient and billing administrators.");
    List<Invoice> rows =
        p.role().equals("Admin")
            ? invoices.findAllByOrderByDueAt()
            : invoices.findByPatientIdOrderByDueAt(p.id());
    return rows.stream()
        .map(
            i ->
                new InvoiceDto(
                    i.getId(),
                    i.getInvoiceNumber(),
                    i.getDescription(),
                    i.getAmountCents(),
                    i.getStatus(),
                    i.getDueAt(),
                    p.role().equals("Admin") ? i.getPatient().getName() : null))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProviderDto> providers() {
    return users.findAllByRoleOrderByName("Provider").stream()
        .map(u -> new ProviderDto(u.getId(), u.getName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AdminUserDto> adminUsers(AppPrincipal p, String role) {
    require(p, "Admin");
    if (role != null && !List.of("Patient", "Provider", "Admin").contains(role))
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role filter.");
    List<User> list =
        role == null
            ? users.findAll().stream().sorted(Comparator.comparing(User::getName)).toList()
            : users.findAllByRoleOrderByName(role);
    return list.stream()
        .map(
            u ->
                new AdminUserDto(
                    u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public OverviewDto overview(AppPrincipal p) {
    boolean admin = p.role().equals("Admin");
    boolean provider = p.role().equals("Provider");
    long total =
        admin
            ? appointments.count()
            : provider
                ? appointments.countByProviderId(p.id())
                : appointments.countByPatientId(p.id());
    long pending =
        admin
            ? appointments.countByStatus("Pending")
            : provider
                ? appointments.countByProviderIdAndStatus(p.id(), "Pending")
                : appointments.countByPatientIdAndStatus(p.id(), "Pending");
    long confirmed =
        admin
            ? appointments.countByStatus("Confirmed")
            : provider
                ? appointments.countByProviderIdAndStatus(p.id(), "Confirmed")
                : appointments.countByPatientIdAndStatus(p.id(), "Confirmed");
    int patientCount =
        admin
            ? Math.toIntExact(users.countByRole("Patient"))
            : provider
                ? Math.toIntExact(appointments.countDistinctPatientsByProviderId(p.id()))
                : 1;
    int providerCount = admin ? Math.toIntExact(users.countByRole("Provider")) : provider ? 1 : 0;
    return new OverviewDto(
        new AppointmentCounts(Math.toIntExact(total), pending, confirmed),
        patientCount,
        providerCount,
        p.role());
  }

  @Transactional(readOnly = true)
  public List<CarePatientDto> carePatients(AppPrincipal p) {
    require(p, "Provider");
    return appointments.findDistinctPatientsByProviderIdAndStatusNot(p.id(), "Cancelled").stream()
        .map(u -> new CarePatientDto(u.getId(), u.getName()))
        .toList();
  }

  @Transactional
  public BodyComplaintDto createComplaint(AppPrincipal p, BodyComplaintRequest r) {
    require(p, "Patient");
    PatientBodyComplaint c = new PatientBodyComplaint();
    c.setId("bc-" + UUID.randomUUID());
    c.setPatient(user(p.id()));
    c.setBodyPart(r.bodyPart().trim());
    c.setBodyPartId(r.bodyPartId().trim());
    c.setSymptom(r.symptom().trim());
    c.setPainLevel(r.painLevel());
    c.setDuration(r.duration().trim());
    c.setDescription(blankAsSpace(r.description()));
    c.setCreatedAt(Instant.now());
    c.setUpdatedAt(c.getCreatedAt());
    complaints.save(c);
    audit.record(p.id(), "body_complaint.created", "patient_body_complaint", c.getId());
    return complaintDto(c);
  }

  @Transactional(readOnly = true)
  public List<BodyComplaintDto> patientComplaints(AppPrincipal p, String patientId) {
    require(p, "Patient");
    if (!p.id().equals(patientId))
      throw new ApiException(HttpStatus.FORBIDDEN, "You can only view your own body complaints.");
    return complaints.findByPatientIdOrderByCreatedAtDesc(p.id()).stream()
        .map(this::complaintDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public DoctorBodyDto doctorBody(AppPrincipal p, String patientId) {
    requireAssigned(p, patientId);
    User patient = user(patientId);
    if (!patient.getRole().equals("Patient"))
      throw new ApiException(HttpStatus.NOT_FOUND, "Patient not found.");
    return new DoctorBodyDto(
        new CarePatientDto(patient.getId(), patient.getName()),
        complaints.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
            .map(this::complaintDto)
            .toList());
  }

  @Transactional
  public BodyComplaintDto reviewComplaint(AppPrincipal p, String id, BodyComplaintUpdate u) {
    require(p, "Provider");
    PatientBodyComplaint c =
        complaints
            .findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Body complaint not found."));
    requireAssigned(p, c.getPatient().getId());
    if (u.diagnosis() != null) c.setDiagnosis(blankAsSpace(u.diagnosis()));
    if (u.doctorNotes() != null) c.setDoctorNotes(blankAsSpace(u.doctorNotes()));
    if (u.treatmentRecommendation() != null)
      c.setTreatmentRecommendation(blankAsSpace(u.treatmentRecommendation()));
    if (u.status() != null) c.setStatus(u.status());
    if (u.diagnosis() == null
        && u.doctorNotes() == null
        && u.treatmentRecommendation() == null
        && u.status() == null)
      throw new ApiException(HttpStatus.BAD_REQUEST, "Provide at least one update.");
    c.setUpdatedAt(Instant.now());
    audit.record(p.id(), "body_complaint.reviewed", "patient_body_complaint", id);
    return complaintDto(c);
  }

  @Transactional
  public void deleteComplaint(AppPrincipal p, String id) {
    require(p, "Patient");
    PatientBodyComplaint c =
        complaints
            .findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Body complaint not found."));
    if (!c.getPatient().getId().equals(p.id()))
      throw new ApiException(HttpStatus.FORBIDDEN, "You can only remove your own body complaints.");
    if (!c.getStatus().equals("Open")
        || !c.getDiagnosis().isBlank()
        || !c.getDoctorNotes().isBlank()
        || !c.getTreatmentRecommendation().isBlank())
      throw new ApiException(
          HttpStatus.CONFLICT,
          "This report has a clinical review and is retained in your care history.");
    audit.record(p.id(), "body_complaint.deleted", "patient_body_complaint", id);
    complaints.delete(c);
  }

  @Transactional(readOnly = true)
  public List<AuditDto> auditLog(AppPrincipal p) {
    require(p, "Admin");
    return audits.findTop100ByOrderByCreatedAtDesc().stream()
        .map(
            a ->
                new AuditDto(
                    a.getId(),
                    a.getActorId(),
                    a.getAction(),
                    a.getEntity(),
                    a.getEntityId(),
                    a.getCreatedAt()))
        .toList();
  }

  private static String blankAsSpace(String value) {
    return value == null || value.isBlank() ? " " : value.trim();
  }

  private static String emptyIfSpace(String value) {
    return value == null ? "" : value.trim();
  }

  private BodyComplaintDto complaintDto(PatientBodyComplaint c) {
    return new BodyComplaintDto(
        c.getId(),
        c.getPatient().getId(),
        c.getBodyPart(),
        c.getBodyPartId(),
        c.getSymptom(),
        c.getPainLevel(),
        c.getDuration(),
        emptyIfSpace(c.getDescription()),
        emptyIfSpace(c.getDiagnosis()),
        emptyIfSpace(c.getDoctorNotes()),
        emptyIfSpace(c.getTreatmentRecommendation()),
        c.getStatus(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }

  private void requireAssigned(AppPrincipal p, String patientId) {
    require(p, "Provider");
    if (!appointments.existsByPatientIdAndProviderIdAndStatusNot(patientId, p.id(), "Cancelled"))
      throw new ApiException(HttpStatus.FORBIDDEN, "This patient is not assigned to your care.");
  }

  private void require(AppPrincipal p, String role) {
    if (!p.role().equals(role))
      throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this resource.");
  }
}
