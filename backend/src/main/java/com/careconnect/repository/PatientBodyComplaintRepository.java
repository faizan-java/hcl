package com.careconnect.repository;

import com.careconnect.domain.PatientBodyComplaint;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientBodyComplaintRepository
    extends JpaRepository<PatientBodyComplaint, String> {
  List<PatientBodyComplaint> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
