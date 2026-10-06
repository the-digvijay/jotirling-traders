package com.jotirling.controller;

import jakarta.servlet.http.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String origin;

  public WebConfig(@Value("${app.frontend-origin}") String origin) {
    this.origin = origin;
  }

  public void addCorsMappings(CorsRegistry registry) {
    registry
      .addMapping("/api/**")
      .allowedOrigins(origin)
      .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("Content-Type", "X-App-Request")
      .allowCredentials(true);
  }

  public void addInterceptors(InterceptorRegistry registry) {
    registry
      .addInterceptor(
        new HandlerInterceptor() {
          private final Map<String, long[]> rates = new ConcurrentHashMap<>();

          public boolean preHandle(
            HttpServletRequest req,
            HttpServletResponse res,
            Object handler
          ) {
            if (req.getMethod().equals("OPTIONS")) return true;
            if (
              !req.getMethod().equals("GET") &&
              !"1".equals(req.getHeader("X-App-Request"))
            ) throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "Missing request verification header"
            );
            String supplied = req.getHeader("Origin");
            if (
              supplied != null && !supplied.equals(origin)
            ) throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "Origin not allowed"
            );
            if (
              req.getRequestURI().startsWith("/api/auth/") &&
              req.getMethod().equals("POST")
            ) {
              long now = System.currentTimeMillis();
              synchronized (rates) {
                rates.entrySet().removeIf(e -> now - e.getValue()[0] > 60000);
                long[] r = rates.computeIfAbsent(req.getRemoteAddr(), k ->
                  new long[] { now, 0 }
                );
                if (++r[1] > 30) throw new ResponseStatusException(
                  HttpStatus.TOO_MANY_REQUESTS,
                  "Too many sign-in attempts. Try again in a minute"
                );
              }
            }
            return true;
          }
        }
      )
      .addPathPatterns("/api/**");
  }
}
