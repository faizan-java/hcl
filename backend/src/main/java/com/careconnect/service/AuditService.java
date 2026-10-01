package com.careconnect.service;

import com.careconnect.domain.AuditEntry;
import com.careconnect.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
  private final AuditRepository repo;

  public AuditService(AuditRepository repo) {
    this.repo = repo;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(String actor, String action, String entity, String id) {
    repo.save(new AuditEntry(actor, action, entity, id));
  }
}
