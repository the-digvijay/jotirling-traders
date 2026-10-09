package com.jotirling.controller;
import com.jotirling.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public class TripManifestController {
 private final AuthService auth;private final TripManifestService service;
 public TripManifestController(AuthService a,TripManifestService s){auth=a;service=s;}
 @GetMapping("/trip-manifest") public Object get(@RequestParam("routeId") Long routeId,@RequestParam("date") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,HttpServletRequest r){auth.admin(r);return service.get(routeId,date);}
}
