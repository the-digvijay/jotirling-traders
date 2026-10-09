package com.jotirling.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="jt_paymentclaim") public class PaymentClaim {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public ShopOrder shopOrder;
 @Column(nullable=false,precision=15,scale=2) public BigDecimal amount;
 @Column(nullable=false,length=100) public String reference;
 @Column(unique=true,length=120) public String referenceKey;
 @Column(nullable=false,unique=true,length=36) public String requestId;
 @Column(nullable=false,length=64) public String requestHash;
 @Column(nullable=false) public LocalDate paidDate;
 @Column(nullable=false,length=20) public String status="PENDING";
 @Lob @Column(nullable=false,columnDefinition="MEDIUMBLOB") public byte[] screenshot;
 @Column(nullable=false,length=30) public String mediaType;
 @Column(nullable=false,length=500) public String reviewNotes="";
 @ManyToOne public Account reviewedBy;
 public Instant reviewedAt;
 public Long receiptId;
 public Instant createdAt=Instant.now();
}
