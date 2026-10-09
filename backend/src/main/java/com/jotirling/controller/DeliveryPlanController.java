package com.jotirling.controller;
import com.jotirling.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public class DeliveryPlanController {
 private final AuthService auth;private final DeliveryReminderService service;
 public DeliveryPlanController(AuthService a,DeliveryReminderService s){auth=a;service=s;}
 @PutMapping("/orders/{id}/delivery") public void plan(@PathVariable("id") Long id,@Valid @RequestBody DeliveryReminderService.Plan p,HttpServletRequest r){auth.admin(r);service.plan(id,p);}
}
