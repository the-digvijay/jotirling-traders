package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_purchase")
public class Purchase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Product product;

  @Column(nullable = false, length = 150)
  public String productName;

  @Column(nullable = false, length = 150)
  public String supplier;

  @Column(nullable = false, length = 100)
  public String invoiceReference;

  @Column(nullable = false, unique = true, length = 36)
  public String requestId;

  @Column(nullable = false, length = 64)
  public String requestHash;

  public int quantity;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal unitCost;

  @Column(nullable = false, precision = 15, scale = 2)
  public BigDecimal total;

  public LocalDate receivedDate;
  public Instant createdAt = Instant.now();
}
