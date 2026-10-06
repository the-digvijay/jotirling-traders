package com.jotirling.repository;

import com.jotirling.entity.Notice;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
  List<Notice> findTop100ByAccountIdOrderByCreatedAtDescIdDesc(Long id);
  List<
    Notice
  > findTop30ByPushDoneFalseAndPushAttemptsLessThanAndNextPushAtBeforeOrderByIdAsc(
    int max,
    Instant now
  );
}
