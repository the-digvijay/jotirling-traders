package com.jotirling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import com.jotirling.dto.Inputs.*;
import com.jotirling.service.AuthService;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:traders_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.admin-password=TestAdmin@123",
    "app.push-public=",
    "app.push-private=",
  }
)
@AutoConfigureMockMvc
class ShopIntegrationTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  AuthService auth;

  Cookie admin() {
    return new Cookie(
      "jt_session",
      auth.createSession(
        auth.login(new Login("admin@jotirling.local", "TestAdmin@123"))
      )
    );
  }

  Cookie customer() {
    String id = UUID.randomUUID().toString();
    return new Cookie(
      "jt_session",
      auth.createSession(
        auth.register(
          new Register(
            "Test customer",
            id + "@example.com",
            "9876543210",
            "Customer@123"
          )
        )
      )
    );
  }

  JsonNode send(
    String method,
    String url,
    Object body,
    Cookie cookie,
    int status
  ) throws Exception {
    var req =
      switch (method) {
        case "POST" -> post(url);
        case "PATCH" -> patch(url);
        default -> get(url);
      };
    req.header("X-App-Request", "1").header("Origin", "http://localhost:5175");
    if (cookie != null) req.cookie(cookie);
    if (body != null) req
      .contentType(MediaType.APPLICATION_JSON)
      .content(json.writeValueAsString(body));
    var r = mvc.perform(req).andReturn().getResponse();
    assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(status);
    return r.getContentAsString().isBlank()
      ? json.nullNode()
      : json.readTree(r.getContentAsString());
  }

  long product(Cookie a, int stock) throws Exception {
    var p = send(
      "POST",
      "/api/admin/products",
      Map.of(
        "sku",
        UUID.randomUUID().toString(),
        "name",
        "Welding rods",
        "category",
        "WELDING",
        "variant",
        "2.5 mm",
        "unit",
        "box",
        "description",
        "Test product",
        "price",
        100,
        "lowStock",
        2,
        "active",
        true
      ),
      a,
      200
    );
    long id = p.get("id").asLong();
    send("POST", "/api/admin/purchases", purchaseBody(id, stock), a, 200);
    return id;
  }

  Map<String, Object> purchaseBody(long id, int qty) {
    return Map.of(
      "productId",
      id,
      "supplier",
      "Supplier",
      "invoiceReference",
      "INV-1",
      "quantity",
      qty,
      "unitCost",
      60,
      "receivedDate",
      "2026-01-01",
      "requestId",
      UUID.randomUUID().toString()
    );
  }

  Map<String, Object> orderBody(long id, int qty) {
    return Map.of(
      "requestId",
      UUID.randomUUID().toString(),
      "fulfilment",
      "PICKUP",
      "address",
      "",
      "notes",
      "Please confirm",
      "items",
      List.of(Map.of("productId", id, "quantity", qty))
    );
  }

  JsonNode inventory(long id) throws Exception {
    for (JsonNode p : send("GET", "/api/products", null, null, 200)) if (
      p.get("id").asLong() == id
    ) return p;
    throw new AssertionError("Missing product");
  }

  @Test
  void orderLifecyclePurchasesNotificationsAndAccess() throws Exception {
    var a = admin();
    var c = customer();
    var stranger = customer();
    long p = product(a, 10);
    var request = orderBody(p, 3);
    var order = send("POST", "/api/orders", request, c, 200);
    long id = order.get("id").asLong();
    assertThat(order.get("total").decimalValue()).isEqualByComparingTo(
      "300.00"
    );
    assertThat(order.get("status").asText()).isEqualTo("PENDING");
    assertThat(
      send("POST", "/api/orders", request, c, 200).get("id").asLong()
    ).isEqualTo(id);
    assertThat(inventory(p).get("reserved").asInt()).isZero();
    send("GET", "/api/orders/" + id, null, stranger, 403);
    send("GET", "/api/admin/dashboard", null, c, 403);
    send("GET", "/api/orders", null, null, 401);
    assertThat(
      send("GET", "/api/notifications", null, a, 200).toString()
    ).contains("New order #" + id);
    assertThat(
      send("GET", "/api/notifications", null, c, 200).toString()
    ).contains("Order received #" + id);
    send(
      "PATCH",
      "/api/admin/orders/" + id,
      Map.of("status", "CONFIRMED", "notes", "Order booked"),
      a,
      200
    );
    assertThat(inventory(p).get("available").asInt()).isEqualTo(7);
    assertThat(
      send("GET", "/api/notifications", null, c, 200).toString()
    ).contains("CONFIRMED");
    send("POST", "/api/orders/" + id + "/cancel", null, c, 400);
    send(
      "PATCH",
      "/api/admin/orders/" + id,
      Map.of("status", "DELIVERED", "notes", ""),
      a,
      400
    );
    send(
      "PATCH",
      "/api/admin/orders/" + id,
      Map.of("status", "READY", "notes", "Pickup ready"),
      a,
      200
    );
    send(
      "PATCH",
      "/api/admin/orders/" + id,
      Map.of("status", "DELIVERED", "notes", "Collected"),
      a,
      200
    );
    assertThat(inventory(p).get("stock").asInt()).isEqualTo(7);
    assertThat(inventory(p).get("reserved").asInt()).isZero();
    send(
      "PATCH",
      "/api/admin/orders/" + id,
      Map.of("status", "DELIVERED", "notes", "Collected"),
      a,
      200
    );
    assertThat(inventory(p).get("stock").asInt()).isEqualTo(7);
    var receipt = purchaseBody(p, 2);
    long purchaseId = send("POST", "/api/admin/purchases", receipt, a, 200)
      .get("id")
      .asLong();
    assertThat(
      send("POST", "/api/admin/purchases", receipt, a, 200).get("id").asLong()
    ).isEqualTo(purchaseId);
    assertThat(inventory(p).get("stock").asInt()).isEqualTo(9);
    var cancelled = send("POST", "/api/orders", orderBody(p, 2), c, 200);
    long cancelId = cancelled.get("id").asLong();
    send(
      "PATCH",
      "/api/admin/orders/" + cancelId,
      Map.of("status", "CONFIRMED", "notes", ""),
      a,
      200
    );
    send(
      "PATCH",
      "/api/admin/orders/" + cancelId,
      Map.of("status", "CANCELLED", "notes", "Customer requested"),
      a,
      200
    );
    assertThat(inventory(p).get("stock").asInt()).isEqualTo(9);
    assertThat(inventory(p).get("reserved").asInt()).isZero();
    var notice = send("GET", "/api/notifications", null, c, 200).get(0);
    send(
      "PATCH",
      "/api/notifications/" + notice.get("id").asLong() + "/read",
      null,
      stranger,
      403
    );
    send(
      "PATCH",
      "/api/notifications/" + notice.get("id").asLong() + "/read",
      null,
      c,
      200
    );
  }

  @Test
  void concurrentConfirmCannotOversell() throws Exception {
    var a = admin();
    long p = product(a, 1);
    long one = send("POST", "/api/orders", orderBody(p, 1), customer(), 200)
      .get("id")
      .asLong();
    long two = send("POST", "/api/orders", orderBody(p, 1), customer(), 200)
      .get("id")
      .asLong();
    var pool = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    try {
      List<Future<Integer>> results = new ArrayList<>();
      for (long id : List.of(one, two)) results.add(
        pool.submit(() -> {
          gate.await();
          return mvc
            .perform(
              patch("/api/admin/orders/" + id)
                .cookie(a)
                .header("X-App-Request", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\",\"notes\":\"\"}")
            )
            .andReturn()
            .getResponse()
            .getStatus();
        })
      );
      gate.countDown();
      assertThat(
        List.of(
          results.get(0).get(15, TimeUnit.SECONDS),
          results.get(1).get(15, TimeUnit.SECONDS)
        )
      ).containsExactlyInAnyOrder(200, 400);
      assertThat(inventory(p).get("stock").asInt()).isEqualTo(1);
      assertThat(inventory(p).get("reserved").asInt()).isEqualTo(1);
      assertThat(inventory(p).get("available").asInt()).isZero();
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void csrfValidationAndPushEndpoints() throws Exception {
    var a = admin();
    var c = customer();
    assertThat(
      mvc
        .perform(post("/api/auth/logout").cookie(c))
        .andReturn()
        .getResponse()
        .getStatus()
    ).isEqualTo(403);
    assertThat(
      mvc
        .perform(
          post("/api/auth/logout")
            .cookie(c)
            .header("X-App-Request", "1")
            .header("Origin", "https://untrusted.example")
        )
        .andReturn()
        .getResponse()
        .getStatus()
    ).isEqualTo(403);
    send(
      "POST",
      "/api/orders",
      Map.of(
        "requestId",
        UUID.randomUUID().toString(),
        "fulfilment",
        "PICKUP",
        "address",
        "",
        "notes",
        "",
        "items",
        List.of(Map.of("productId", 1, "quantity", 0))
      ),
      c,
      400
    );
    send(
      "POST",
      "/api/push/subscribe",
      Map.of(
        "endpoint",
        "https://127.0.0.1/private",
        "p256dh",
        "bad",
        "auth",
        "bad"
      ),
      c,
      400
    );
    assertThat(
      send("GET", "/api/config", null, null, 200)
        .get("push")
        .get("enabled")
        .asBoolean()
    ).isFalse();
    var req = orderBody(product(a, 5), 1);
    var id = send("POST", "/api/orders", req, c, 200).get("id").asLong();
    send("POST", "/api/orders/" + id + "/cancel", null, c, 200);
    send("POST", "/api/orders/" + id + "/cancel", null, c, 200);
  }
}
