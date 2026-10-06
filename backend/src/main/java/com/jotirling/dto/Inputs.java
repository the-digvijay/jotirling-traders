package com.jotirling.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class Inputs {

  public record Register(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank @Pattern(regexp = "[0-9]{10}") String phone,
    @NotBlank @Size(min = 10, max = 128) String password
  ) {}

  public record Login(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 128) String password
  ) {}

  public record ProductInput(
    @NotBlank @Size(max = 50) String sku,
    @NotBlank @Size(max = 150) String name,
    @NotBlank String category,
    @NotNull @Size(max = 150) String variant,
    @NotBlank @Size(max = 40) String unit,
    @NotNull @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.01") @Digits(
      integer = 10,
      fraction = 2
    ) BigDecimal price,
    @Min(0) @Max(1000000) int lowStock,
    boolean active
  ) {}

  public record CartLine(
    @NotNull @Positive Long productId,
    @Min(1) @Max(100000) int quantity
  ) {}

  public record OrderInput(
    @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String requestId,
    @NotBlank @Pattern(regexp = "PICKUP|DELIVERY") String fulfilment,
    @NotNull @Size(max = 500) String address,
    @NotNull @Size(max = 500) String notes,
    @NotEmpty @Size(max = 100) List<@Valid CartLine> items
  ) {}

  public record StatusInput(
    @NotBlank @Pattern(
      regexp = "CONFIRMED|PREPARING|READY|DISPATCHED|DELIVERED|REJECTED|CANCELLED"
    ) String status,
    @NotNull @Size(max = 500) String notes
  ) {}

  public record PurchaseInput(
    @NotNull @Positive Long productId,
    @NotBlank @Size(max = 150) String supplier,
    @NotBlank @Size(max = 100) String invoiceReference,
    @Min(1) @Max(100000) int quantity,
    @NotNull @DecimalMin("0.01") @Digits(
      integer = 10,
      fraction = 2
    ) BigDecimal unitCost,
    @NotNull @PastOrPresent LocalDate receivedDate,
    @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String requestId
  ) {}

  public record SubscriptionInput(
    @NotBlank @Size(max = 1500) String endpoint,
    @NotBlank @Size(max = 200) String p256dh,
    @NotBlank @Size(max = 100) String auth
  ) {}
}
