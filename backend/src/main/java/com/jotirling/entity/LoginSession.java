package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_loginsession")
public class LoginSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Account account;

  @Column(nullable = false, unique = true, length = 64)
  public String tokenHash;

  @Column(nullable = false)
  public Instant expiresAt;
}
