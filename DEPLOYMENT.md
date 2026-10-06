# Online deployment and background push

Local trial is complete without this setup. Internet customers need a shared HTTPS URL. A server hosting React static files, Spring Boot and MySQL is required; no hosting account is created by this ZIP. HTTPS certificates and hosting providers are deployment choices, and their availability/cost depends on the provider.

## Recommended layout

Serve React and `/api` on the same HTTPS domain. This avoids cross-site cookies. Use a reverse proxy for TLS; do not expose MySQL or development ports publicly.

Backend environment:

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/jotirling_traders
DB_USERNAME=YOUR_DATABASE_USER
DB_PASSWORD=YOUR_DATABASE_PASSWORD
ADMIN_EMAIL=YOUR_ADMIN_EMAIL
ADMIN_PASSWORD=YOUR_UNIQUE_PASSWORD_AT_LEAST_10_CHARACTERS
FRONTEND_ORIGIN=https://YOUR_DOMAIN
COOKIE_SECURE=true
VAPID_PUBLIC_KEY=YOUR_PUBLIC_KEY
VAPID_PRIVATE_KEY=YOUR_PRIVATE_KEY
VAPID_SUBJECT=mailto:YOUR_CONTACT_EMAIL
```

These are placeholders, not working credentials. Environment files are ignored by Git and must be loaded by your service manager/hosting configuration. Spring Boot does not automatically read `.env` files. Admin credentials seed only a new account; updating environment values does not reset an existing password. Run normal MySQL mode, never the demo profile for an internet shop.

Build the backend `mvn package` in backend; run `java -jar target/traders-1.0.0.jar` with the environment above. Configure a service manager/container restart policy. Keep backend port8082 bound to localhost behind the proxy (e.g. `SERVER_ADDRESS=127.0.0.1`). Restrict DB user rights and take tested backups before schema changes.

Frontend `.env.production` (not committed):

```text
VITE_API_URL=/api
```

Run `npm ci` and `npm run build`; serve `frontend/dist` with SPA fallback. Example nginx locations inside an existing HTTPS server block:

```nginx
root /var/www/jotirling;
location / {
    try_files $uri $uri/ /index.html;
}
location /api/ {
    proxy_pass http://127.0.0.1:8082;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
location = /sw.js {
    add_header Cache-Control "no-cache";
}
```

The example assumes your domain, TLS certificate and nginx server block are already configured. Copy dist contents including manifest, icons, banner and sw.js. Backend origin must exactly match HTTPS frontend origin (no trailing slash).

## VAPID keys

From the project root run once:

```sh
node tools/generate-vapid.cjs
```

Store the generated public and private values in the backend environment. Never commit or share the private key. Keep the same key pair across restarts; replacing it requires browser re-subscription. Use your shop contact email as VAPID_SUBJECT. The public key is exposed to the client as required by Web Push.

After hosting:

1. Sign in on each customer's/shop device, go to Notifications and press Enable browser notifications.
2. Permit notifications in the browser. Install PWA where supported. iPhone/iPad push requires a supported OS/browser and home-screen web app setup.
3. Create a customer order using another device/account; check shop inbox and push; confirm it; check customer inbox and push.
4. Test foreground, background, network reconnect, expired subscription and logout. Do not assume background push works until these device checks pass.

Server checks new inbox events every15seconds and retries failed pushes with limited backoff (up to5attempts). Expired subscriptions (404/410) are removed. Browser notification tags coalesce retries; remote push is best-effort and the stored inbox remains the source of truth. Some browsers suppress push when force-stopped or restricted by battery/network settings. Active app order/inbox polling runs every10seconds. Background push requires backend still running, HTTPS, browser permission and VAPID setup.

Push endpoints are allowlisted for common Chrome/Firefox/Safari/Windows services; unsupported endpoints are rejected. Automatic SMS/WhatsApp sending is not implemented; manual WhatsApp buttons are included.

## Pre-launch checks

Replace sample prices with actual catalogue, add opening stock through receipts, create a unique admin password, test fulfilment rules with staff, verify backups and permissions. Tax invoices, automatic payment collection, returns and stock adjustments require a later release before using this app for those workflows.

References: https://developer.mozilla.org/en-US/docs/Web/API/Push_API and https://github.com/web-push-libs/webpush-java .
