package com.jotirling.repository;

import com.jotirling.entity.PushSubscription;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository
  extends JpaRepository<PushSubscription, Long> {
  Optional<PushSubscription> findByEndpointHash(String hash);
  List<PushSubscription> findByAccountId(Long id);
}
