# Jotirling Traders, Gargoti

A React + Spring Boot ordering and stock application, with the supplied shop banner, Marathi/English UI and an installable PWA shell.

Start with [START-HERE-MARATHI.md](START-HERE-MARATHI.md). Deployment/push configuration is in [DEPLOYMENT.md](DEPLOYMENT.md).

## Included

- Public searchable catalogue: welding, paints, plumbing, electrical, hardware, plywood, cement, sanitary and tools.
- Customer registration, login, cart, pickup/delivery requests, personal order history, pending cancellation, printable order summaries.
- Shop admin: product metadata/prices, stock register, supplier purchase receipts, order confirmation/status updates, dashboard and low-stock alerts.
- Stored per-account notifications on new order and status changes; active app refreshes every10seconds; opt-in alert sound and foreground browser alerts.
- Background Web Push subscription/service-worker/server dispatcher. Requires HTTPS deployment, VAPID configuration, permission, supported browser and connectivity. Actual remote device push has not been validated in this development environment.
- Manual Cash/UPI/Bank payment receipts, partial/full payments, customer outstanding balances, audited void of incorrect entries and payment inbox notices.
- Manual WhatsApp order/status buttons with shop contact 8381017384 and a louder opt-in alert.
- Cookie sessions, password hashing, role/ownership checks, input validation, origin/header checks, transactional stock reservation and request replay protection.

## Local trial

Java17, Maven3.9+, Node20+.

```sh
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

In a second terminal:

```sh
cd frontend
npm install
npm run dev
```

Open http://localhost:5175. Backend: http://localhost:8082/api.
Admin: `admin@jotirling.local` / `DemoShop@123`. Customers create their own accounts. Test both sides using a normal window and an incognito window. Demo uses persistent H2; normal mode uses MySQL with a unique ADMIN_PASSWORD environment variable.

## Stock and order rules

Pending submissions do not reserve stock. Confirmation checks available stock and reserves it atomically. Delivery subtracts on-hand and reserved stock. Cancellation before dispatch releases reservations. Pending orders may be rejected. Dispatched orders must be delivered; returns and stock corrections are not implemented. Products can be hidden; historical order prices/names remain snapshots. Purchases add received stock. Amounts use decimal arithmetic; quantity is whole sale units (box, bag, tin, piece etc.). Define separate SKUs for each size/shade; fractional lengths/weights are not implemented.

Order and purchase submissions have UUID request IDs. Reusing an ID with the same data returns the prior result. Reusing an ID with different data is rejected. Frontend retains uncertain requests for retry without creating a new receipt/order.

## Validation

```sh
cd backend
mvn test
mvn package
cd ../frontend
npm run build
```

Integration tests cover order lifecycle, notification ownership, purchase/order replay, reservation release, concurrent confirmation without overselling, authentication/roles, CSRF/origin checks and invalid push endpoints. Production MySQL and real browser/device push require deployment testing.

## First-release scope

Purchase entries represent received stock, not supplier purchase-order approval. Dashboard sales means delivered order value, not money collected. Order printout is not a GST invoice or a payment receipt. There is no payment gateway/bank transfer, automatic SMS/WhatsApp sending, password reset/email verification, image uploads, returns or multi-branch support. Customer notifications are app inbox + optional Web Push. Keep backups, control admin access and validate actual shop catalogue/rates before real use.

Tech: React19/Vite6, Spring Boot3.4.4, JPA, MySQL8 or trial H2, webpush-java5.1.2. Brand/banner belongs to its supplied owner; demo product prices are illustrative.

## Payment update

See PAYMENT-UPDATE-MARATHI.md. This records money already received; it does not transfer money. Confirm an order before adding receipts. Non-cash payments require a unique transaction reference. Paid orders cannot be cancelled; refunds are not implemented. Incorrect receipts can be voided with a reason and retained audit history. All amounts/balances are calculated on the backend with BigDecimal.
