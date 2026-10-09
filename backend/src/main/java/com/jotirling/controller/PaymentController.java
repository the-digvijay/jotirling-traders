package com.jotirling.controller;
import com.jotirling.service.*;
import com.jotirling.dto.PaymentInputs.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/orders/{orderId}/payments")
public class PaymentController {
 private final AuthService auth;private final PaymentService payments;
 public PaymentController(AuthService a,PaymentService p){auth=a;payments=p;}

 @PostMapping("/{paymentId}/void") public PaymentService.Summary voidEntry(@PathVariable Long orderId,@PathVariable Long paymentId,@Valid @RequestBody VoidInput in,HttpServletRequest req){return payments.voidEntry(auth.admin(req),orderId,paymentId,in);}
}
