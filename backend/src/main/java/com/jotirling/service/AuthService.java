package com.jotirling.service;

import com.jotirling.dto.Inputs.*;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

  private final AccountRepository accounts;
  private final LoginSessionRepository sessions;

  public AuthService(AccountRepository a, LoginSessionRepository s) {
    accounts = a;
    sessions = s;
  }

  public record UserView(
    Long id,
    String name,
    String email,
    String phone,
    String role
  ) {}

  public UserView view(Account a) {
    return new UserView(a.id, a.name, a.email, a.phone, a.role);
  }

  public static String hash(String v) {
    try {
      return HexFormat.of()
        .formatHex(
          MessageDigest.getInstance("SHA-256").digest(
            v.getBytes(StandardCharsets.UTF_8)
          )
        );
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private byte[] derive(String password, byte[] salt, int iterations) {
    try {
      var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
      try {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
          .generateSecret(spec)
          .getEncoded();
      } finally {
        spec.clearPassword();
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String encode(String p) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return (
      "310000:" +
      Base64.getEncoder().encodeToString(salt) +
      ":" +
      Base64.getEncoder().encodeToString(derive(p, salt, 310000))
    );
  }

  private boolean matches(String p, String stored) {
    String[] parts = stored.split(":");
    return MessageDigest.isEqual(
      Base64.getDecoder().decode(parts[2]),
      derive(
        p,
        Base64.getDecoder().decode(parts[1]),
        Integer.parseInt(parts[0])
      )
    );
  }

  public Account register(Register in) {
    String email = in.email().trim().toLowerCase(Locale.ROOT);
    if (
      accounts.findByEmail(email).isPresent()
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Email already registered"
    );
    Account a = new Account();
    a.name = in.name().trim();
    a.email = email;
    a.phone = in.phone();
    a.passwordHash = encode(in.password());
    return accounts.save(a);
  }

  public Account login(Login in) {
    Account a = accounts
      .findByEmail(in.email().trim().toLowerCase(Locale.ROOT))
      .orElse(null);
    if (
      a == null || !matches(in.password(), a.passwordHash)
    ) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Email or password incorrect"
    );
    return a;
  }

  public String createSession(Account a) {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(bytes);
    LoginSession s = new LoginSession();
    s.account = a;
    s.tokenHash = hash(token);
    s.expiresAt = Instant.now().plus(Duration.ofDays(7));
    sessions.save(s);
    return token;
  }

  private String cookie(HttpServletRequest req) {
    if (req.getCookies() != null) for (var c : req.getCookies()) if (
      c.getName().equals("jt_session")
    ) return c.getValue();
    return null;
  }

  @Transactional(readOnly = true)
  public Account current(HttpServletRequest req) {
    String token = cookie(req);
    if (token == null) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Please sign in"
    );
    LoginSession s = sessions
      .findByTokenHash(hash(token))
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in")
      );
    if (s.expiresAt.isBefore(Instant.now())) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Session expired. Sign in again"
    );
    return s.account;
  }

  public Account admin(HttpServletRequest r) {
    Account a = current(r);
    if (!a.role.equals("ADMIN")) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Admin access required"
    );
    return a;
  }

  public void logout(HttpServletRequest r) {
    String t = cookie(r);
    if (t != null) sessions.deleteByTokenHash(hash(t));
  }

  public void seedAdmin(String email, String password) {
    if (
      password == null || password.length() < 10
    ) throw new IllegalStateException(
      "Set ADMIN_PASSWORD to a unique password of at least 10 characters before starting the app"
    );
    if (accounts.findByEmail(email.toLowerCase(Locale.ROOT)).isEmpty()) {
      Account a = new Account();
      a.name = "Jotirling Traders";
      a.email = email.toLowerCase(Locale.ROOT);
      a.phone = "8381017384";
      a.passwordHash = encode(password);
      a.role = "ADMIN";
      accounts.save(a);
    }
  }
}
