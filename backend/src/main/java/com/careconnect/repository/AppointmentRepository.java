package com.careconnect.repository;

import com.careconnect.domain.Appointment;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
  long countByStatus(String status);

  long countByPatientId(String patientId);

  long countByPatientIdAndStatus(String patientId, String status);

  long countByProviderId(String providerId);

  long countByProviderIdAndStatus(String providerId, String status);

  @Query("select count(distinct a.patient.id) from Appointment a where a.provider.id = :provider")
  long countDistinctPatientsByProviderId(@Param("provider") String providerId);

  @EntityGraph(attributePaths = {"patient", "provider"})
  List<Appointment> findAllByOrderByStartsAt();

  @EntityGraph(attributePaths = {"patient", "provider"})
  List<Appointment> findByPatientIdOrderByStartsAt(String id);

  @EntityGraph(attributePaths = {"patient", "provider"})
  List<Appointment> findByProviderIdOrderByStartsAt(String id);

  @EntityGraph(attributePaths = {"patient"})
  List<Appointment> findByProviderIdAndStatusNotOrderByStartsAt(String providerId, String status);

  boolean existsByPatientIdAndProviderIdAndStatusNot(
      String patientId, String providerId, String status);

  @Query(
      "select distinct a.patient from Appointment a where a.provider.id = :provider and a.status <> :status order by a.patient.name")
  List<com.careconnect.domain.User> findDistinctPatientsByProviderIdAndStatusNot(
      @Param("provider") String providerId, @Param("status") String status);

  @Query(
      "select a from Appointment a where a.provider.id=:provider and a.status <> 'Cancelled' and a.startsAt between :from and :to")
  List<Appointment> findConflicts(
      @Param("provider") String provider, @Param("from") Instant from, @Param("to") Instant to);
}
