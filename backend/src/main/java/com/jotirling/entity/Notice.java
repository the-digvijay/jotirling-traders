package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_notice")
public class Notice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Account account;

  @Column(nullable = false, length = 150)
  public String title;

  @Column(nullable = false, length = 500)
  public String body;

  public Long orderId;
  public boolean readFlag = false;
  public boolean pushDone = false;
  public int pushAttempts = 0;
  public Instant nextPushAt = Instant.now();
  public Instant createdAt = Instant.now();
}
