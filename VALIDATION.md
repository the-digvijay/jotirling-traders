# Validation of this ZIP

Validated on 5 October 2026 with Java17, Maven3.9.9 and Node24:

- `mvn package`: passed. Three integration tests, zero failures/errors. These cover pickup lifecycle, access/notification ownership, purchase/order replay protection, cancellation, validation and concurrent order confirmation without overselling.
- `npm run build`: passed.
- Packaged Spring Boot app started in the demo profile with an isolated in-memory H2 database. Actual HTTP requests passed: 18 seeded products, admin login, customer registration/session, delivery order PENDING → CONFIRMED → READY → DISPATCHED → DELIVERED, notifications for both accounts, stock deduction, idempotent purchase receipt and logout.

Source ZIP excludes generated build outputs, database contents and dependencies; first install/build needs internet access. The supplied banner is included.

Not validated here: production MySQL, visual browser/device interaction, remote HTTPS deployment or background push delivery to a physical device. Deployment and device checks are described in DEPLOYMENT.md. No paid messaging service is required for the included inbox/Web Push implementation; hosting is a separate setup.
