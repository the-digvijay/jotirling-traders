package com.jotirling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import com.jotirling.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class SeedData {

  @Bean
  CommandLineRunner seed(
    AuthService auth,
    ProductRepository products,
    Environment env,
    ObjectMapper mapper,
    @Value("${app.admin-email}") String email,
    @Value("${app.admin-password}") String password
  ) {
    return args -> {
      auth.seedAdmin(email, password);
      if (env.matchesProfiles("demo") && products.count() == 0) {
        try (
          var input = new ClassPathResource(
            "demo-products.json"
          ).getInputStream()
        ) {
          var rows = mapper.readTree(input);
          for (var x : rows) {
            Product p = new Product();
            p.sku = x.get("sku").asText();
            p.name = x.get("name").asText();
            p.category = x.get("category").asText();
            p.variant = x.get("variant").asText();
            p.unit = x.get("unit").asText();
            p.price = new BigDecimal(x.get("price").asText());
            p.stock = 30;
            p.description =
              "Illustrative sample product and price. Shop must verify brand, pack size and actual price before business use.";
            products.save(p);
          }
        }
      }
    };
  }
}
