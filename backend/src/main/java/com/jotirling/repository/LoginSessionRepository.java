package com.jotirling.repository;

import com.jotirling.entity.LoginSession;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository
  extends JpaRepository<LoginSession, Long> {
  Optional<LoginSession> findByTokenHash(String hash);
  void deleteByTokenHash(String hash);
  void deleteByExpiresAtBefore(Instant time);
}
