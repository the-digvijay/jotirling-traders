package com.jotirling.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public class PaymentInputs {
 public record Receipt(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,
 @NotBlank @Pattern(regexp="CASH|UPI|BANK") String method,
 @NotNull @Size(max=100) String reference,
 @NotNull @Size(max=500) String notes,
 @NotNull @PastOrPresent LocalDate receivedDate,
 @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String requestId) {}
 public record VoidInput(@NotBlank @Size(max=500) String reason) {}
}
