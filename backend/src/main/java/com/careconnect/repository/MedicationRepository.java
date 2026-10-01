package com.careconnect.repository;

import com.careconnect.domain.Medication;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, String> {
  List<Medication> findByPatientIdOrderByName(String patientId);
}
