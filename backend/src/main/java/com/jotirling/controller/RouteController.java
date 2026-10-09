package com.jotirling.controller;
import com.jotirling.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") public class RouteController {
 private final AuthService auth;private final RouteService service;
 public RouteController(AuthService a,RouteService s){auth=a;service=s;}
 @GetMapping("/routes") public Object list(HttpServletRequest r){return service.list("ADMIN".equals(auth.current(r).role));}
 @GetMapping("/routes/mine") public Object mine(HttpServletRequest r){return service.mine(auth.current(r));}
 @PutMapping("/routes/mine") public void assign(@RequestBody RouteService.Assignment a,HttpServletRequest r){service.assign(auth.current(r).id,a.routeId());}
 @PostMapping("/admin/routes/defaults") public void defaults(HttpServletRequest r){auth.admin(r);service.defaults();}
 @PostMapping("/admin/routes") public Object add(@Valid @RequestBody RouteService.Input in,HttpServletRequest r){auth.admin(r);return service.save(null,in);}
 @PutMapping("/admin/routes/{id}") public Object edit(@PathVariable Long id,@Valid @RequestBody RouteService.Input in,HttpServletRequest r){auth.admin(r);return service.save(id,in);}
 @GetMapping("/admin/routes/customers") public Object customers(HttpServletRequest r){auth.admin(r);return service.customers();}
 @PutMapping("/admin/routes/customers/{id}") public void assignCustomer(@PathVariable Long id,@RequestBody RouteService.Assignment a,HttpServletRequest r){auth.admin(r);service.assign(id,a.routeId());}
 @PostMapping("/admin/routes/{id}/announce") public Object announce(@PathVariable Long id,HttpServletRequest r){auth.admin(r);return Map.of("notified",service.announce(id));}
}
