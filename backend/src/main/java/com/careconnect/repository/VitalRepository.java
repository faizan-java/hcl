package com.careconnect.repository;

import com.careconnect.domain.Vital;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalRepository extends JpaRepository<Vital, String> {
  List<Vital> findByPatientIdOrderByRecordedAtDesc(String patientId);
}
