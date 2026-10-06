package com.jotirling.repository;

import com.jotirling.entity.Account;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmail(String email);
  List<Account> findByRole(String role);
}
