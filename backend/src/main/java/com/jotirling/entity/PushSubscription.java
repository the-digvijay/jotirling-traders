package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_pushsubscription")
public class PushSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Account account;

  @Column(nullable = false, unique = true, length = 64)
  public String endpointHash;

  @Column(nullable = false, length = 1500)
  public String endpoint;

  @Column(nullable = false, length = 200)
  public String p256dh;

  @Column(nullable = false, length = 100)
  public String auth;
}
