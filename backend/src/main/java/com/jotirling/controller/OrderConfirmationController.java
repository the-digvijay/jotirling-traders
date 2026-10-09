package com.jotirling.controller;
import com.jotirling.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/orders") public class OrderConfirmationController {
 private final AuthService auth;private final OrderConfirmationService service;
 public OrderConfirmationController(AuthService a,OrderConfirmationService s){auth=a;service=s;}
 @GetMapping("/{id}/delivery-suggestion") public Object suggestion(@PathVariable("id") Long id,@RequestParam(value="routeId",required=false) Long route,HttpServletRequest r){return service.suggest(auth.admin(r),id,route);}
 @PostMapping("/{id}/confirm") public Object confirm(@PathVariable("id") Long id,@Valid @RequestBody OrderConfirmationService.Input in,HttpServletRequest r){return service.confirm(auth.admin(r),id,in);}
}
