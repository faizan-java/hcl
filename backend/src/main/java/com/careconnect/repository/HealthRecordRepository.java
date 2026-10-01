package com.careconnect.repository;

import com.careconnect.domain.HealthRecord;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, String> {
  List<HealthRecord> findByPatientIdOrderByRecordedAtDesc(String patientId);

  List<HealthRecord> findByProviderIdOrderByRecordedAtDesc(String providerId);
}
