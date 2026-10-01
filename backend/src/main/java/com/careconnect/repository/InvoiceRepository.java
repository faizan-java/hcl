package com.careconnect.repository;

import com.careconnect.domain.Invoice;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
  List<Invoice> findByPatientIdOrderByDueAt(String patientId);

  List<Invoice> findAllByOrderByDueAt();
}
