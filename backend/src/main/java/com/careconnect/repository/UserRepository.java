package com.careconnect.repository;

import com.careconnect.domain.User;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {
  long countByRole(String role);

  Optional<User> findByEmailIgnoreCase(String email);

  List<User> findAllByRoleOrderByName(String role);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") String id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<User> findFirstByRoleOrderByName(String role);

  boolean existsByEmailIgnoreCase(String email);
}
