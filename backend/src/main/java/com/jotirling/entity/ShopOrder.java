package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_shoporder")
public class ShopOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Account customer;

  @Column(nullable = false, unique = true, length = 36)
  public String requestId;

  @Column(nullable = false, length = 64)
  public String requestHash;

  @Column(nullable = false, length = 20)
  public String status = "PENDING";

  @Column(nullable = false, length = 15)
  public String fulfilment;

  @Column(nullable = false, length = 500)
  public String address;

  @Column(nullable = false, length = 500)
  public String customerNotes;

  @Column(nullable = false, length = 500)
  public String shopNotes = "";

  @Column(nullable = false, precision = 15, scale = 2)
  public BigDecimal total;

  public Instant createdAt = Instant.now();
  public Instant updatedAt = Instant.now();
}
