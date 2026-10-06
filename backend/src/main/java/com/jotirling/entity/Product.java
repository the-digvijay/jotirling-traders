package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_product")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 50)
  public String sku;

  @Column(nullable = false, length = 150)
  public String name;

  @Column(nullable = false, length = 50)
  public String category;

  @Column(nullable = false, length = 150)
  public String variant;

  @Column(nullable = false, length = 40)
  public String unit;

  @Column(nullable = false, length = 1000)
  public String description;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal price;

  public int stock = 0;
  public int reserved = 0;
  public int lowStock = 5;
  public boolean active = true;

  public String imageVersion;
}
