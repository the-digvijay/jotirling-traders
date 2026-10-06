package com.jotirling.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "jt_account")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 100)
  public String name;

  @Column(nullable = false, unique = true, length = 150)
  public String email;

  @Column(nullable = false, length = 15)
  public String phone;

  @Column(nullable = false, length = 300)
  public String passwordHash;

  @Column(nullable = false)
  public String role = "CUSTOMER";
}
