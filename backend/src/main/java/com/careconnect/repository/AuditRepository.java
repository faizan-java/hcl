package com.careconnect.repository;

import com.careconnect.domain.AuditEntry;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {
  List<AuditEntry> findTop100ByOrderByCreatedAtDesc();
}
