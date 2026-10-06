package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_orderline")
public class OrderLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public ShopOrder shopOrder;

  @ManyToOne(optional = false)
  public Product product;

  @Column(nullable = false, length = 150)
  public String name;

  @Column(nullable = false, length = 150)
  public String variant;

  @Column(nullable = false, length = 40)
  public String unit;

  public int quantity;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal price;

  @Column(nullable = false, precision = 15, scale = 2)
  public BigDecimal amount;
}
